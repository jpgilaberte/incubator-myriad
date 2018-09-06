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
package org.apache.myriad.scheduler.event.handlers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.lmax.disruptor.EventHandler;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.myriad.configuration.MyriadConfiguration;
import org.apache.myriad.driver.MesosDriver;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.scheduler.SchedulerUtils;
import org.apache.myriad.scheduler.ServiceResourceProfile;
import org.apache.myriad.scheduler.TaskFactory;
import org.apache.myriad.scheduler.constraints.Constraint;
import org.apache.myriad.scheduler.event.ResourceOffersEvent;
import org.apache.myriad.scheduler.fgs.OfferLifecycleManager;
import org.apache.myriad.scheduler.resource.ResourceOfferContainer;
import org.apache.myriad.state.NodeTask;
import org.apache.myriad.state.SchedulerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.myriad.configuration.MyriadConfiguration.DEFAULT_ROLE;

/**
 * handles and logs resource offers events
 */
public class ResourceOffersEventHandler implements EventHandler<ResourceOffersEvent> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceOffersEventHandler.class);

  private static final Lock driverOperationLock = new ReentrantLock();

  private SchedulerState schedulerState;
  private MyriadConfiguration cfg;
  private Map<String, TaskFactory> taskFactoryMap;
  private OfferLifecycleManager offerLifecycleMgr;
  private String role;

  @Inject
  public ResourceOffersEventHandler(SchedulerState schedulerState, MyriadConfiguration cfg, Map<String, TaskFactory> taskFactoryMap, OfferLifecycleManager offerLifecycleManager) {
    this.schedulerState = schedulerState;
    this.cfg = cfg;
    this.taskFactoryMap = taskFactoryMap;
    this.offerLifecycleMgr = offerLifecycleManager;
    this.role = cfg.getFrameworkRole();
  }

  @Override
  public void onEvent(ResourceOffersEvent event, long sequence, boolean endOfBatch) throws Exception {
    MesosDriver driver = event.getDriver();
    List<MesosV1.Offer> offers = event.getOffers();

    // Sometimes, we see that mesos sends resource offers before Myriad receives
    // a notification for "framework registration". This is a simple defensive code
    // to not process any offers unless Myriad receives a "framework registered" notification.
    if (schedulerState.getFrameworkID() == null) {
      LOGGER.warn("Received {} offers, but declining them since Framework ID is not yet set", offers.size());
      for (MesosV1.Offer offer : offers) {
        driver.declineOffer(Arrays.asList(offer.getId()), null);
      }
      return;
    }
    LOGGER.debug("Received offers {}", offers.size());
    LOGGER.debug("Pending tasks: {}", this.schedulerState.getPendingTaskIds());

    // Let Myriad use reserved resources firstly.
    Collections.sort(offers, new Comparator<MesosV1.Offer>() {
      boolean isReserved(MesosV1.Offer o) {
        for (MesosV1.Resource resource : o.getResources()) {
          if (resource.getRole() != null && !Objects.equals(resource.getRole(), DEFAULT_ROLE)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public int compare(MesosV1.Offer o1, MesosV1.Offer o2) {
        boolean reserved1 = isReserved(o1);
        boolean reserved2 = isReserved(o2);

        if (reserved1 == reserved2) {
          return 0;
        }

        return reserved1 ? -1 : 1;
      }
    });

    driverOperationLock.lock();
    try {
      for (Iterator<MesosV1.Offer> iterator = offers.iterator(); iterator.hasNext(); ) {
        MesosV1.Offer offer = iterator.next();
        Set<NodeTask> nodeTasks = schedulerState.getNodeTasks(offer.getAgent_id());
        for (NodeTask nodeTask : nodeTasks) {
          nodeTask.setSlaveAttributes(offer.getAttributes());
        }
        // keep this in case SchedulerState gets out of sync. This should not happen with
        // synchronizing addNodes method in SchedulerState
        // but to keep it safe
        final Set<MesosV1.TaskID> missingTasks = Sets.newHashSet();
        Set<MesosV1.TaskID> pendingTasks = schedulerState.getPendingTaskIds();
        if (CollectionUtils.isNotEmpty(pendingTasks)) {
          for (MesosV1.TaskID pendingTaskId : pendingTasks) {
            NodeTask taskToLaunch = schedulerState.getTask(pendingTaskId);
            if (taskToLaunch == null) {
              missingTasks.add(pendingTaskId);
              LOGGER.warn("Node task for TaskID: {} does not exist", pendingTaskId);
              continue;
            }
            String taskPrefix = taskToLaunch.getTaskPrefix();
            ServiceResourceProfile profile = taskToLaunch.getProfile();
            Constraint constraint = taskToLaunch.getConstraint();

            Set<NodeTask> launchedTasks = new HashSet<>();
            launchedTasks.addAll(schedulerState.getActiveTasksByType(taskPrefix));
            launchedTasks.addAll(schedulerState.getStagingTasksByType(taskPrefix));

            ResourceOfferContainer resourceOfferContainer = new ResourceOfferContainer(offer, taskToLaunch.getProfile(), role);
            if (SchedulerUtils.isUniqueHostname(offer, taskToLaunch, launchedTasks)
                && resourceOfferContainer.satisfies(taskToLaunch.getProfile(), constraint)) {
              try {
                final MesosV1.TaskInfo task = taskFactoryMap.get(taskPrefix).createTask(resourceOfferContainer,
                    schedulerState.getFrameworkID(), pendingTaskId, taskToLaunch);
                LOGGER.info("Launching task: {} using offer: {}", task.getTask_id().getValue(), offer.getId());
                LOGGER.debug("Launching task: {} with profile: {} using offer: {}", task, profile, offer);
                driver.launchTasks(Lists.newArrayList(offer.getId()), Lists.newArrayList(task), null);
                schedulerState.makeTaskStaging(pendingTaskId);
                // For every NM Task that we launch, we currently
                // need to backup the ExecutorInfo for that NM Task in the State Store.
                // Without this, we will not be able to launch tasks corresponding to yarn
                // containers. This is specially important in case the RM restarts.
                taskToLaunch.setExecutorInfo(task.getExecutor());
                taskToLaunch.setHostname(offer.getHostname());
                taskToLaunch.setSlaveId(offer.getAgent_id());
                schedulerState.addTask(pendingTaskId, taskToLaunch);
                iterator.remove(); // remove the used offer from offers list
                break;
              } catch (Throwable t) {
                LOGGER.error("Exception thrown while trying to create a task for {}", taskPrefix, t);
              }
            }
          }
          for (MesosV1.TaskID taskId : missingTasks) {
            schedulerState.removeTask(taskId);
          }
        }
      }

      for (MesosV1.Offer offer : offers) {
        if (SchedulerUtils.isEligibleForFineGrainedScaling(offer.getHostname(), schedulerState)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Picking an offer from slave with hostname {} for fine grained scaling.", offer.getHostname());
          }
          offerLifecycleMgr.addOffers(offer);
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Declining offer {} from slave {}.", offer, offer.getHostname());
          }
          driver.declineOffer(Lists.newArrayList(offer.getId()), null);
        }
      }
    } finally {
      driverOperationLock.unlock();
    }
  }
}
