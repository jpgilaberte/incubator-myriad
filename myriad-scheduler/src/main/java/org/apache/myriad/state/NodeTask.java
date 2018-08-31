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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.scheduler.ServiceResourceProfile;
import org.apache.myriad.scheduler.TaskUtils;
import org.apache.myriad.scheduler.constraints.Constraint;

import java.util.List;

/**
 * Represents a Mesos task to be launched by the Mesos executor
 */
public class NodeTask {
  @JsonProperty
  private String hostname = StringUtils.EMPTY;
  @JsonProperty
  private MesosV1.AgentID slaveId;
  @JsonProperty
  private MesosV1.TaskStatus taskStatus;
  @JsonProperty
  private String taskPrefix;
  @JsonProperty
  private ServiceResourceProfile profile;

  @Inject
  TaskUtils taskUtils;

  /**
   * Mesos executor for this node.
   */
  private MesosV1.ExecutorInfo executorInfo;

  private Constraint constraint;
  
  private List<MesosV1.Attribute> slaveAttributes;

  public NodeTask(ServiceResourceProfile profile, Constraint constraint) {
    this.profile    = profile;
    this.constraint = constraint;
  }

  public MesosV1.AgentID getSlaveId() {
    return slaveId;
  }

  public void setSlaveId(MesosV1.AgentID slaveId) {
    this.slaveId = slaveId;
  }

  public Constraint getConstraint() {
    return constraint;
  }

  public String getHostname() {
    return this.hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public MesosV1.TaskStatus getTaskStatus() {
    return taskStatus;
  }

  public void setTaskStatus(MesosV1.TaskStatus taskStatus) {
    this.taskStatus = taskStatus;
  }

  public MesosV1.ExecutorInfo getExecutorInfo() {
    return executorInfo;
  }

  public void setExecutorInfo(MesosV1.ExecutorInfo executorInfo) {
    this.executorInfo = executorInfo;
  }

  public void setSlaveAttributes(List<MesosV1.Attribute> slaveAttributes) {
    this.slaveAttributes = slaveAttributes;
  }

  public List<MesosV1.Attribute> getSlaveAttributes() {
    return slaveAttributes;
  }

  public String getTaskPrefix() {
    return taskPrefix;
  }

  public void setTaskPrefix(String taskPrefix) {
    this.taskPrefix = taskPrefix;
  }

  public ServiceResourceProfile getProfile() {
    return profile;
  }

  public void setProfile(ServiceResourceProfile serviceresourceProfile) {
    this.profile = serviceresourceProfile;
  }
}
