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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.myriad.scheduler;

import com.google.inject.Inject;
import org.apache.myriad.configuration.MyriadConfiguration;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.scheduler.resource.ResourceOfferContainer;
import org.apache.myriad.state.NodeTask;

import java.util.List;

/**
 * Creates Node Manager Tasks based upon Mesos offers
 */
public class NMTaskFactory extends TaskFactory {


  @Inject
  NMTaskFactory(MyriadConfiguration cfg, TaskUtils taskUtils, ExecutorCommandLineGenerator clGenerator) {
    super(cfg, taskUtils, clGenerator);
  }

  @Override
  public MesosV1.TaskInfo createTask(ResourceOfferContainer resourceOfferContainer, MesosV1.FrameworkID frameworkId, MesosV1.TaskID taskId, NodeTask nodeTask) {
    ServiceResourceProfile serviceProfile = nodeTask.getProfile();
    Double taskMemory = serviceProfile.getAggregateMemory();
    Double taskCpus = serviceProfile.getAggregateCpu();
    List<MesosV1.Resource> portResources = resourceOfferContainer.consumePorts(serviceProfile.getPorts().values());
    MesosV1.CommandInfo commandInfo = clGenerator.generateCommandLine(serviceProfile, null, rangesConverter(portResources));
    MesosV1.ExecutorInfo executorInfo = getExecutorInfoForSlave(resourceOfferContainer, frameworkId, commandInfo);

    MesosV1.TaskInfo taskInfo = new MesosV1.TaskInfo();
    taskInfo.setName(cfg.getFrameworkName() + "-" + taskId.getValue());
    taskInfo.setTask_id(taskId);
    taskInfo.setAgent_id(resourceOfferContainer.getSlaveId());

    List<MesosV1.Resource> resourceList = resourceOfferContainer.consumeCpus(taskCpus);
    resourceList.addAll(resourceOfferContainer.consumeMem(taskMemory));
    resourceList.addAll(portResources);
    taskInfo.setResources(resourceList);
    taskInfo.setExecutor(executorInfo);
    return taskInfo;
  }

  @Override
  public MesosV1.ExecutorInfo getExecutorInfoForSlave(ResourceOfferContainer resourceOfferContainer, MesosV1.FrameworkID frameworkId, MesosV1.CommandInfo commandInfo) {

    MesosV1.ExecutorID executorId = new MesosV1.ExecutorID();
    executorId.setValue(EXECUTOR_PREFIX + frameworkId.getValue() + resourceOfferContainer.getOfferId() + resourceOfferContainer.getSlaveId().getValue());

    MesosV1.ExecutorInfo executorInfo = new MesosV1.ExecutorInfo();
    executorInfo.setCommand(commandInfo);
    executorInfo.setName(EXECUTOR_NAME);
    executorInfo.setExecutor_id(executorId);

    List<MesosV1.Resource> resourceList = resourceOfferContainer.consumeCpus(taskUtils.getExecutorCpus());
    resourceList.addAll(resourceOfferContainer.consumeMem(taskUtils.getExecutorMemory()));

    executorInfo.setResources(resourceList);
    if (cfg.getContainerInfo().isPresent()) {
      executorInfo.setContainer(getContainerInfo());
    }
    return executorInfo;

  }
}
