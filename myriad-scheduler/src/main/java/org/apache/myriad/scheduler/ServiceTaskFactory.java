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
import org.apache.myriad.configuration.ServiceConfiguration;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.scheduler.resource.ResourceOfferContainer;
import org.apache.myriad.state.NodeTask;

import java.util.List;
import java.util.Objects;

/**
 * Generic Service Class that allows to create a service solely base don the configuration
 * Main properties of configuration are:
 * 1. command to run
 * 2. Additional env. variables to set (serviceOpts)
 * 3. ports to use with names of the properties
 * 4. TODO (yufeldman) executor info
 */
public class ServiceTaskFactory extends TaskFactory {

  @Inject
  ServiceTaskFactory(MyriadConfiguration cfg, TaskUtils taskUtils, ExecutorCommandLineGenerator clGenerator) {
    super(cfg, taskUtils, clGenerator);
    this.clGenerator = new ServiceCommandLineGenerator(cfg);
  }

  @Override
  public MesosV1.TaskInfo createTask(ResourceOfferContainer resourceOfferContainer, MesosV1.FrameworkID frameworkId, MesosV1.TaskID taskId, NodeTask nodeTask) {
    ServiceConfiguration serviceConfig = cfg.getServiceConfiguration(nodeTask.getTaskPrefix()).get();

    Objects.requireNonNull(serviceConfig, "ServiceConfig should be non-null");
    Objects.requireNonNull(serviceConfig.getCommand().orNull(), "command for ServiceConfig should be non-null");
    List<MesosV1.Resource> portResources = resourceOfferContainer.consumePorts(nodeTask.getProfile().getPorts().values());
    MesosV1.CommandInfo commandInfo = clGenerator.generateCommandLine(nodeTask.getProfile(), serviceConfig, rangesConverter(portResources));

    LOGGER.info("Command line for service: {} is: {}", commandInfo.getValue());

    MesosV1.TaskInfo taskBuilder = new MesosV1.TaskInfo();

    taskBuilder.setName(nodeTask.getTaskPrefix());
    taskBuilder.setTask_id(taskId);
    taskBuilder.setAgent_id(resourceOfferContainer.getSlaveId());

    List<MesosV1.Resource> resourceList = resourceOfferContainer.consumeCpus(nodeTask.getProfile().getCpus());
    resourceList.addAll(resourceOfferContainer.consumeMem(nodeTask.getProfile().getMemory()));
    resourceList.addAll(portResources);

    taskBuilder.setResources(resourceList);

    taskBuilder.setCommand(commandInfo);
    if (cfg.getContainerInfo().isPresent()) {
      taskBuilder.setContainer(getContainerInfo());
    }
    return taskBuilder;
  }

  @Override
  public MesosV1.ExecutorInfo getExecutorInfoForSlave(ResourceOfferContainer resourceOfferContainer, MesosV1.FrameworkID frameworkId, MesosV1.CommandInfo commandInfo) {
    return null;
  }
}
