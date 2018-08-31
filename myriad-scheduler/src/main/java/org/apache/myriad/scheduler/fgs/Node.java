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
package org.apache.myriad.scheduler.fgs;

import java.util.HashSet;
import java.util.Set;
import org.apache.hadoop.yarn.server.resourcemanager.rmcontainer.RMContainer;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.SchedulerNode;
import org.apache.mesos.Protos;
import org.apache.myriad.driver.model.MesosV1;

/**
 * Abstraction that encapsulates the combined YARN and Mesos views of a node.
 */
public class Node {
  /**
   * Mesos slave id associated with this node.
   */
  private MesosV1.AgentID slaveId;

  /**
   * Mesos executor on this node.
   */
  private MesosV1.ExecutorInfo execInfo;

  /**
   * YARN scheduler's representation of this node.
   */
  private SchedulerNode node;

  /**
   * Snapshot of containers allocated by YARN scheduler.
   * This need not reflect the current state. It is meant to be used by the
   * Myriad scheduler.
   */
  private Set<RMContainer> containerSnapshot;

  public Node(SchedulerNode node) {
    this.node = node;
  }

  public SchedulerNode getNode() {
    return node;
  }

  public MesosV1.AgentID getSlaveId() {
    return slaveId;
  }

  public void setSlaveId(MesosV1.AgentID slaveId) {
    this.slaveId = slaveId;
  }

  public MesosV1.ExecutorInfo getExecInfo() {
    return execInfo;
  }

  public void setExecInfo(MesosV1.ExecutorInfo execInfo) {
    this.execInfo = execInfo;
  }

  public void snapshotRunningContainers() {
    this.containerSnapshot = new HashSet<>(node.getRunningContainers());
  }

  public void removeContainerSnapshot() {
    this.containerSnapshot = null;
  }

  public Set<RMContainer> getContainerSnapshot() {
    return this.containerSnapshot;
  }
}
