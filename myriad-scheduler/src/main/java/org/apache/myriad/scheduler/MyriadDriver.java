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
package org.apache.myriad.scheduler;

import javax.inject.Inject;
import org.apache.myriad.driver.MesosDriver;
import org.apache.myriad.driver.model.MesosV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MyriadDriver class is a wrapper for the Mesos SchedulerDriver class. Accordingly, 
 * all public MyriadDriver methods delegate to the corresponding SchedulerDriver methods. 
 */
public class MyriadDriver {
  private static final Logger LOGGER = LoggerFactory.getLogger(MyriadDriver.class);

  private final MesosDriver driver;

  @Inject
  public MyriadDriver(MesosDriver driver) {
    this.driver = driver;
  }

  /**
   * Stops the underlying Mesos SchedulerDriver. If the failover flag is set to
   * false, Myriad will not reconnect to Mesos. Consequently, Mesos will unregister 
   * the Myriad framework and shutdown all the Myriad tasks and executors. If failover 
   * is set to true, all Myriad executors and tasks will remain running for a defined
   * period of time, allowing the MyriadScheduler to reconnect to Mesos.
   *
   * @param failover    Whether framework failover is expected.
   *
   * @return            The state of the driver after the call.
   *
   * @see Status
   */
  public MesosV1.Status stop(boolean failover) {
    LOGGER.info("Stopping driver");
    MesosV1.Status status = driver.stop(failover);
    LOGGER.info("Driver stopped with status: {}", status);
    return status;
  }

  /**
   * Starts the underlying Mesos SchedulerDriver. Note: this method must
   * be called before any other MyriadDriver methods are invoked.
   *
   * @return The state of the driver after the call.
   *
   * @see Status
   */
  public MesosV1.Status start() {
    LOGGER.info("Starting driver");
    MesosV1.Status status = driver.start();
    LOGGER.info("Driver started with status: {}", status);
    return status;
  }

  /**
   * Kills the specified task via the underlying Mesos SchedulerDriver. 
   * Important note from the Mesos documentation: "attempting to kill a 
   * task is currently not reliable. If, for example, a scheduler fails over
   * while it was attempting to kill a task it will need to retry in
   * the future Likewise, if unregistered / disconnected, the request
   * will be dropped (these semantics may be changed in the future)."
   *
   * @param taskId  The ID of the task to be killed.
   *
   * @return        The state of the driver after the call.
   * 
   * @see Status
   */  
  public MesosV1.Status kill(final MesosV1.TaskID taskId, final MesosV1.AgentID agent_id, final MesosV1.KillPolicy kill_policy) {
    MesosV1.Status status = driver.killTask(taskId, agent_id, kill_policy);
    LOGGER.info("Task {} kill initiated with Driver status  {}", taskId, status);    
    return status;
  }

  /**
   * Aborts the underlying Mesos SchedulerDriver so that no more callbacks 
   * can be made to the MyriadScheduler. Note from Mesos documentation: 
   * The semantics of abort and stop have deliberately been separated so that 
   * code can detect an aborted driver and instantiate and start another driver 
   * if desired (from within the same process).
   *
   * @return The state of the driver after the call.
   * 
   * @see Status
   */  
  public MesosV1.Status abort() {
    LOGGER.info("Aborting driver");
    MesosV1.Status status = driver.abort();
    LOGGER.info("Aborted driver with status: {}", status);
    return status;
  }

  /**
   * Returns reference to the underlying Mesos SchedulerDriver
   * to which all method invocations are delegated to.
   * 
   * @return the underlying Mesos SchedulerDriver
   */
  public MesosDriver getDriver() {
    return driver;
  }
}
