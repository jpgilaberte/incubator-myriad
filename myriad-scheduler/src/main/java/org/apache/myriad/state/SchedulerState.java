/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myriad.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.scheduler.ServiceResourceProfile;
import org.apache.myriad.state.utils.StoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Encapsulates the state of the all {@link NodeTask} objects managed
 * by a {@link MyriadScheduler} as well as corresponding state update methods.
 */
public class SchedulerState {
  private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerState.class);

  private static Pattern taskIdPattern = Pattern.compile("\\.");

  private Map<MesosV1.TaskID, NodeTask> tasks;
  private MesosV1.FrameworkID frameworkId;
  private MyriadStateStore stateStore;
  private Map<String, SchedulerStateForType> statesForTaskType;

  public SchedulerState(MyriadStateStore stateStore) {
    this.tasks = new ConcurrentHashMap<>();
    this.stateStore = stateStore;
    this.statesForTaskType = new ConcurrentHashMap<>();
    loadStateStore();
  }

  /**
   * Making method synchronized, so if someone tries flexup/down at the same time
   * addNodes and removeTask will not put data into an inconsistent state
   *
   * @param nodes
   */
  public synchronized void addNodes(Collection<NodeTask> nodes) {
    if (CollectionUtils.isEmpty(nodes)) {
      LOGGER.info("No nodes to add");
      return;
    }
    for (NodeTask node : nodes) {
      MesosV1.TaskID taskId = new MesosV1.TaskID();
      taskId.setValue(String.format("%s.%s.%s", node.getTaskPrefix(),
          node.getProfile().getName(), UUID.randomUUID()));
      addTask(taskId, node);
      SchedulerStateForType taskState = this.statesForTaskType.get(node.getTaskPrefix());
      LOGGER.info("Marked taskId {} pending, size of pending queue for {} is: {}", taskId.getValue(), node.getTaskPrefix(),
          (taskState == null ? 0 : taskState.getPendingTaskIds().size()));
      makeTaskPending(taskId);
    }

  }

  // TODO (sdaingade) Clone NodeTask
  public synchronized void addTask(MesosV1.TaskID taskId, NodeTask node) {
    this.tasks.put(taskId, node);
    updateStateStore();
  }

  public synchronized void updateTask(MesosV1.TaskStatus taskStatus) {
    Objects.requireNonNull(taskStatus, "TaskStatus object shouldn't be null");
    MesosV1.TaskID taskId = taskStatus.getTask_id();
    if (this.tasks.containsKey(taskId)) {
      this.tasks.get(taskId).setTaskStatus(taskStatus);
    }
    updateStateStore();
  }

  public synchronized void makeTaskPending(MesosV1.TaskID taskId) {
    Objects.requireNonNull(taskId, "taskId cannot be empty or null");
    String taskPrefix = taskIdPattern.split(taskId.getValue())[0];
    SchedulerStateForType taskTypeState = statesForTaskType.get(taskPrefix);
    if (taskTypeState == null) {
      taskTypeState = new SchedulerStateForType(taskPrefix);
      statesForTaskType.put(taskPrefix, taskTypeState);
    }
    taskTypeState.makeTaskPending(taskId);
    updateStateStore();
  }

  public synchronized void makeTaskStaging(MesosV1.TaskID taskId) {
    Objects.requireNonNull(taskId, "taskId cannot be empty or null");
    String taskPrefix = taskIdPattern.split(taskId.getValue())[0];
    SchedulerStateForType taskTypeState = statesForTaskType.get(taskPrefix);
    if (taskTypeState == null) {
      taskTypeState = new SchedulerStateForType(taskPrefix);
      statesForTaskType.put(taskPrefix, taskTypeState);
    }
    taskTypeState.makeTaskStaging(taskId);
    updateStateStore();
  }

  public synchronized void makeTaskActive(MesosV1.TaskID taskId) {
    Objects.requireNonNull(taskId, "taskId cannot be empty or null");
    String taskPrefix = taskIdPattern.split(taskId.getValue())[0];
    SchedulerStateForType taskTypeState = statesForTaskType.get(taskPrefix);
    if (taskTypeState == null) {
      taskTypeState = new SchedulerStateForType(taskPrefix);
      statesForTaskType.put(taskPrefix, taskTypeState);
    }
    taskTypeState.makeTaskActive(taskId);
    updateStateStore();
  }

  public synchronized void makeTaskLost(MesosV1.TaskID taskId) {
    Objects.requireNonNull(taskId, "taskId cannot be empty or null");
    String taskPrefix = taskIdPattern.split(taskId.getValue())[0];
    SchedulerStateForType taskTypeState = statesForTaskType.get(taskPrefix);
    if (taskTypeState == null) {
      taskTypeState = new SchedulerStateForType(taskPrefix);
      statesForTaskType.put(taskPrefix, taskTypeState);
    }
    taskTypeState.makeTaskLost(taskId);
    updateStateStore();
  }

  public synchronized void makeTaskKillable(MesosV1.TaskID taskId) {
    Objects.requireNonNull(taskId, "taskId cannot be empty or null");
    String taskPrefix = taskIdPattern.split(taskId.getValue())[0];
    SchedulerStateForType taskTypeState = statesForTaskType.get(taskPrefix);
    if (taskTypeState == null) {
      taskTypeState = new SchedulerStateForType(taskPrefix);
      statesForTaskType.put(taskPrefix, taskTypeState);
    }
    taskTypeState.makeTaskKillable(taskId);
    updateStateStore();
  }

  // TODO (sdaingade) Clone NodeTask
  public synchronized NodeTask getTask(MesosV1.TaskID taskId) {
    return this.tasks.get(taskId);
  }

  /**
   * Return a list of TaskIDs corresponding to all killable tasks
   * @return
   */
  public synchronized Set<MesosV1.TaskID> getKillableTaskIds() {
    Set<MesosV1.TaskID> returnSet = new HashSet<>();
    for (Map.Entry<String, SchedulerStateForType> entry : statesForTaskType.entrySet()) {
      returnSet.addAll(entry.getValue().getKillableTaskIds());
    }
    return returnSet;
  }

  /**
   * Retrieve set of TaskIDs corresponding to killable tasks for a given prefix
   * 
   * @param taskPrefix
   * @return
   */
  public synchronized Set<MesosV1.TaskID> getKillableTaskIds(String taskPrefix) {
    SchedulerStateForType stateTask = statesForTaskType.get(taskPrefix);
    return (stateTask == null ? new HashSet<MesosV1.TaskID>() : stateTask.getKillableTaskIds());
  }

  public synchronized void removeTask(MesosV1.TaskID taskId) {
    String taskPrefix = taskIdPattern.split(taskId.getValue())[0];
    SchedulerStateForType taskTypeState = statesForTaskType.get(taskPrefix);
    if (taskTypeState != null) {
      taskTypeState.removeTask(taskId);
    }
    this.tasks.remove(taskId);
    updateStateStore();
  }

  public synchronized Set<MesosV1.TaskID> getPendingTaskIds() {
    Set<MesosV1.TaskID> returnSet = new HashSet<>();
    for (Map.Entry<String, SchedulerStateForType> entry : statesForTaskType.entrySet()) {
      returnSet.addAll(entry.getValue().getPendingTaskIds());
    }
    return returnSet;
  }

  public synchronized Collection<MesosV1.TaskID> getPendingTaskIDsForProfile(ServiceResourceProfile serviceProfile) {
    List<MesosV1.TaskID> pendingTaskIds = new ArrayList<>();
    Set<MesosV1.TaskID> pendingTasks = getPendingTaskIds();
    for (Map.Entry<MesosV1.TaskID, NodeTask> entry : tasks.entrySet()) {
      NodeTask nodeTask = entry.getValue();
      if (pendingTasks.contains(entry.getKey()) && nodeTask.getProfile().getName().equals(serviceProfile.getName())) {
        pendingTaskIds.add(entry.getKey());
      }
    }
    return Collections.unmodifiableCollection(pendingTaskIds);
  }

  public synchronized Set<MesosV1.TaskID> getPendingTaskIds(String taskPrefix) {
    SchedulerStateForType stateTask = statesForTaskType.get(taskPrefix);
    return (stateTask == null ? new HashSet<MesosV1.TaskID>() : stateTask.getPendingTaskIds());
  }

  public synchronized Set<MesosV1.TaskID> getActiveTaskIds() {
    Set<MesosV1.TaskID> returnSet = new HashSet<>();
    for (Map.Entry<String, SchedulerStateForType> entry : statesForTaskType.entrySet()) {
      returnSet.addAll(entry.getValue().getActiveTaskIds());
    }
    return returnSet;
  }

  public synchronized Set<MesosV1.TaskID> getActiveTaskIds(String taskPrefix) {
    SchedulerStateForType stateTask = statesForTaskType.get(taskPrefix);
    return (stateTask == null ? new HashSet<MesosV1.TaskID>() : stateTask.getActiveTaskIds());
  }

  public synchronized Set<NodeTask> getActiveTasks() {
    return getTasks(getActiveTaskIds());
  }

  public Set<NodeTask> getActiveTasksByType(String taskPrefix) {
    return getTasks(getActiveTaskIds(taskPrefix));
  }

  public Set<NodeTask> getStagingTasks() {
    return getTasks(getStagingTaskIds());
  }

  public Set<NodeTask> getStagingTasksByType(String taskPrefix) {
    return getTasks(getStagingTaskIds(taskPrefix));
  }

  public Set<NodeTask> getPendingTasksByType(String taskPrefix) {
    return getTasks(getPendingTaskIds(taskPrefix));
  }

  public synchronized Set<NodeTask> getTasks(Set<MesosV1.TaskID> taskIds) {
    Set<NodeTask> nodeTasks = new HashSet<>();
    if (CollectionUtils.isNotEmpty(taskIds) && CollectionUtils.isNotEmpty(tasks.values())) {
      for (Map.Entry<MesosV1.TaskID, NodeTask> entry : tasks.entrySet()) {
        if (taskIds.contains(entry.getKey())) {
          nodeTasks.add(entry.getValue());
        }
      }
    }
    return Collections.unmodifiableSet(nodeTasks);
  }

  public synchronized Collection<MesosV1.TaskID> getActiveTaskIDsForProfile(ServiceResourceProfile serviceProfile) {
    List<MesosV1.TaskID> activeTaskIDs = new ArrayList<>();
    Set<MesosV1.TaskID> activeTaskIds = getActiveTaskIds();
    if (CollectionUtils.isNotEmpty(activeTaskIds) && CollectionUtils.isNotEmpty(tasks.values())) {
      for (Map.Entry<MesosV1.TaskID, NodeTask> entry : tasks.entrySet()) {
        NodeTask nodeTask = entry.getValue();
        if (activeTaskIds.contains(entry.getKey()) && nodeTask.getProfile().getName().equals(serviceProfile.getName())) {
          activeTaskIDs.add(entry.getKey());
        }
      }
    }
    return Collections.unmodifiableCollection(activeTaskIDs);
  }

  // TODO (sdaingade) Clone NodeTask
  public synchronized NodeTask getNodeTask(MesosV1.AgentID slaveId, String taskPrefix) {
    if (taskPrefix == null) {
      return null;
    }
    for (Map.Entry<MesosV1.TaskID, NodeTask> entry : tasks.entrySet()) {
      final NodeTask task = entry.getValue();
      if (task.getSlaveId() != null &&
          task.getSlaveId().equals(slaveId) &&
          taskPrefix.equals(task.getTaskPrefix())) {
        return entry.getValue();
      }
    }
    return null;
  }

  public synchronized Set<NodeTask> getNodeTasks(MesosV1.AgentID slaveId) {
    Set<NodeTask> nodeTasks = Sets.newHashSet();
    for (Map.Entry<MesosV1.TaskID, NodeTask> entry : tasks.entrySet()) {
      final NodeTask task = entry.getValue();
      if (task.getSlaveId() != null && task.getSlaveId().equals(slaveId)) {
        nodeTasks.add(entry.getValue());
      }
    }
    return nodeTasks;
  }

  public Set<MesosV1.TaskID> getStagingTaskIds() {
    Set<MesosV1.TaskID> returnSet = new HashSet<>();
    for (Map.Entry<String, SchedulerStateForType> entry : statesForTaskType.entrySet()) {
      returnSet.addAll(entry.getValue().getStagingTaskIds());
    }
    return returnSet;
  }

  public synchronized Collection<MesosV1.TaskID> getStagingTaskIDsForProfile(ServiceResourceProfile serviceProfile) {
    List<MesosV1.TaskID> stagingTaskIDs = new ArrayList<>();

    Set<MesosV1.TaskID> stagingTasks = getStagingTaskIds();
    for (Map.Entry<MesosV1.TaskID, NodeTask> entry : tasks.entrySet()) {
      NodeTask nodeTask = entry.getValue();
      if (stagingTasks.contains(entry.getKey()) && nodeTask.getProfile().getName().equals(serviceProfile.getName())) {
        stagingTaskIDs.add(entry.getKey());
      }
    }
    return Collections.unmodifiableCollection(stagingTaskIDs);
  }

  public Set<MesosV1.TaskID> getStagingTaskIds(String taskPrefix) {
    SchedulerStateForType stateTask = statesForTaskType.get(taskPrefix);
    return (stateTask == null ? new HashSet<MesosV1.TaskID>() : stateTask.getStagingTaskIds());
  }

  public Set<MesosV1.TaskID> getLostTaskIds() {
    Set<MesosV1.TaskID> returnSet = new HashSet<>();
    for (Map.Entry<String, SchedulerStateForType> entry : statesForTaskType.entrySet()) {
      returnSet.addAll(entry.getValue().getLostTaskIds());
    }
    return returnSet;
  }

  public Set<MesosV1.TaskID> getLostTaskIds(String taskPrefix) {
    SchedulerStateForType stateTask = statesForTaskType.get(taskPrefix);
    return (stateTask == null ? new HashSet<MesosV1.TaskID>() : stateTask.getLostTaskIds());
  }

  // TODO (sdaingade) Currently cannot return unmodifiableCollection
  // as this will break ReconcileService code
  public synchronized Collection<MesosV1.TaskStatus> getTaskStatuses() {
    Collection<MesosV1.TaskStatus> taskStatuses = new ArrayList<>(this.tasks.size());
    Collection<NodeTask> tasks = this.tasks.values();
    for (NodeTask task : tasks) {
      MesosV1.TaskStatus taskStatus = task.getTaskStatus();
      if (taskStatus != null) {
        taskStatuses.add(taskStatus);
      }
    }

    return taskStatuses;
  }

  public synchronized boolean hasTask(MesosV1.TaskID taskID) {
    return this.tasks.containsKey(taskID);
  }

  public synchronized MesosV1.FrameworkID getFrameworkID() {
    return frameworkId;
  }

  public synchronized void setFrameworkId(MesosV1.FrameworkID newFrameworkId) {
    this.frameworkId = newFrameworkId;
    updateStateStore();
  }

  private synchronized void updateStateStore() {
    if (this.stateStore == null) {
      LOGGER.debug("Could not update state to state store as HA is disabled");
      return;
    }

    try {
      StoreContext sc = new StoreContext(frameworkId, tasks, getPendingTaskIds(), getStagingTaskIds(), getActiveTaskIds(),
          getLostTaskIds(), getKillableTaskIds());
      stateStore.storeMyriadState(sc);
    } catch (Exception e) {
      LOGGER.error("Failed to update scheduler state to state store", e);
    }
  }

  private synchronized void loadStateStore() {
    if (this.stateStore == null) {
      LOGGER.debug("Could not load state from state store as HA is disabled");
      return;
    }

    try {
      StoreContext sc = stateStore.loadMyriadState();
      if (sc != null) {
        this.frameworkId = sc.getFrameworkId();
        this.tasks.putAll(sc.getTasks());
        convertToThis(TaskState.PENDING, sc.getPendingTasks());
        convertToThis(TaskState.STAGING, sc.getStagingTasks());
        convertToThis(TaskState.ACTIVE, sc.getActiveTasks());
        convertToThis(TaskState.LOST, sc.getLostTasks());
        convertToThis(TaskState.KILLABLE, sc.getKillableTasks());
        LOGGER.info("Loaded Myriad state from state store successfully.");
        LOGGER.debug("State Store state includes frameworkId: {}, pending tasks count: {}, staging tasks count: {} " +
                     "active tasks count: {}, lost tasks count: {}, and killable tasks count: {}", frameworkId.getValue(),
                      this.getPendingTaskIds().size(), this.getStagingTaskIds().size(), this.getActiveTaskIds().size(),
                      this.getLostTaskIds().size(), this.getKillableTaskIds().size());
      }
    } catch (Exception e) {
      LOGGER.error("Failed to read scheduler state from state store", e);
    }
  }

  private void convertToThis(TaskState taskType, Set<MesosV1.TaskID> taskIds) {
    for (MesosV1.TaskID taskId : taskIds) {
      String taskPrefix = taskIdPattern.split(taskId.getValue())[0];
      SchedulerStateForType taskTypeState = statesForTaskType.get(taskPrefix);
      if (taskTypeState == null) {
        taskTypeState = new SchedulerStateForType(taskPrefix);
        statesForTaskType.put(taskPrefix, taskTypeState);
      }
      switch (taskType) {
        case PENDING:
          taskTypeState.makeTaskPending(taskId);
          break;
        case STAGING:
          taskTypeState.makeTaskStaging(taskId);
          break;
        case ACTIVE:
          taskTypeState.makeTaskActive(taskId);
          break;
        case KILLABLE:
          taskTypeState.makeTaskKillable(taskId);
          break;
        case LOST:
          taskTypeState.makeTaskLost(taskId);
          break;
      }
    }
  }

  /**
   * Class to keep all the tasks states for a particular taskPrefix together
   */
  private static class SchedulerStateForType {

    private final String taskPrefix;
    private Set<MesosV1.TaskID> pendingTasks;
    private Set<MesosV1.TaskID> stagingTasks;
    private Set<MesosV1.TaskID> activeTasks;
    private Set<MesosV1.TaskID> lostTasks;
    private Set<MesosV1.TaskID> killableTasks;

    public SchedulerStateForType(String taskPrefix) {
      this.taskPrefix = taskPrefix;
      // Since Sets.newConcurrentHashSet is available only starting form Guava version 15
      // and so far (Hadoop 2.7) uses guava 13 we can not easily use it
      this.pendingTasks = Collections.newSetFromMap(new ConcurrentHashMap<MesosV1.TaskID, Boolean>());
      this.stagingTasks = Collections.newSetFromMap(new ConcurrentHashMap<MesosV1.TaskID, Boolean>());
      this.activeTasks = Collections.newSetFromMap(new ConcurrentHashMap<MesosV1.TaskID, Boolean>());
      this.lostTasks = Collections.newSetFromMap(new ConcurrentHashMap<MesosV1.TaskID, Boolean>());
      this.killableTasks = Collections.newSetFromMap(new ConcurrentHashMap<MesosV1.TaskID, Boolean>());

    }

    @SuppressWarnings("unused")
    public String getTaskPrefix() {
      return taskPrefix;
    }

    public synchronized void makeTaskPending(MesosV1.TaskID taskId) {
      Objects.requireNonNull(taskId, "taskId cannot be empty or null");

      pendingTasks.add(taskId);
      stagingTasks.remove(taskId);
      activeTasks.remove(taskId);
      lostTasks.remove(taskId);
      killableTasks.remove(taskId);
    }

    public synchronized void makeTaskStaging(MesosV1.TaskID taskId) {
      Objects.requireNonNull(taskId, "taskId cannot be empty or null");
      pendingTasks.remove(taskId);
      stagingTasks.add(taskId);
      activeTasks.remove(taskId);
      lostTasks.remove(taskId);
      killableTasks.remove(taskId);
    }

    public synchronized void makeTaskActive(MesosV1.TaskID taskId) {
      Objects.requireNonNull(taskId, "taskId cannot be empty or null");
      pendingTasks.remove(taskId);
      stagingTasks.remove(taskId);
      activeTasks.add(taskId);
      lostTasks.remove(taskId);
      killableTasks.remove(taskId);
    }

    public synchronized void makeTaskLost(MesosV1.TaskID taskId) {
      Objects.requireNonNull(taskId, "taskId cannot be empty or null");
      pendingTasks.remove(taskId);
      stagingTasks.remove(taskId);
      activeTasks.remove(taskId);
      lostTasks.add(taskId);
      killableTasks.remove(taskId);
    }

    public synchronized void makeTaskKillable(MesosV1.TaskID taskId) {
      Objects.requireNonNull(taskId, "taskId cannot be empty or null");
      pendingTasks.remove(taskId);
      stagingTasks.remove(taskId);
      activeTasks.remove(taskId);
      lostTasks.remove(taskId);
      killableTasks.add(taskId);
    }

    public synchronized void removeTask(MesosV1.TaskID taskId) {
      this.pendingTasks.remove(taskId);
      this.stagingTasks.remove(taskId);
      this.activeTasks.remove(taskId);
      this.lostTasks.remove(taskId);
      this.killableTasks.remove(taskId);
    }

    public synchronized Set<MesosV1.TaskID> getPendingTaskIds() {
      return Collections.unmodifiableSet(this.pendingTasks);
    }

    public Set<MesosV1.TaskID> getActiveTaskIds() {
      return Collections.unmodifiableSet(this.activeTasks);
    }

    public synchronized Set<MesosV1.TaskID> getStagingTaskIds() {
      return Collections.unmodifiableSet(this.stagingTasks);
    }

    public synchronized Set<MesosV1.TaskID> getLostTaskIds() {
      return Collections.unmodifiableSet(this.lostTasks);
    }

    public synchronized Set<MesosV1.TaskID> getKillableTaskIds() {
      return Collections.unmodifiableSet(this.killableTasks);
    }

  }

  /**
   * TaskState type
   */
  public enum TaskState {
    PENDING,
    STAGING,
    ACTIVE,
    KILLABLE,
    LOST
  }
}
