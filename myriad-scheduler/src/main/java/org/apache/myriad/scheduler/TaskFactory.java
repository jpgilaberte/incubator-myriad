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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.myriad.configuration.MyriadConfiguration;
import org.apache.myriad.configuration.MyriadContainerConfiguration;
import org.apache.myriad.configuration.MyriadDockerConfiguration;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.scheduler.resource.ResourceOfferContainer;
import org.apache.myriad.state.NodeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class to create Tasks based upon Mesos offers
 */
public abstract class TaskFactory {
  public static final String EXECUTOR_NAME = "myriad_task";
  public static final String EXECUTOR_PREFIX = "myriad_executor";

  protected static final Logger LOGGER = LoggerFactory.getLogger(TaskFactory.class);

  static final String YARN_RESOURCEMANAGER_HOSTNAME = "yarn.resourcemanager.hostname";
  static final String YARN_RESOURCEMANAGER_WEBAPP_ADDRESS = "yarn.resourcemanager.webapp.address";
  static final String YARN_RESOURCEMANAGER_WEBAPP_HTTPS_ADDRESS = "yarn.resourcemanager.webapp.https.address";
  static final String YARN_HTTP_POLICY = "yarn.http.policy";
  static final String YARN_HTTP_POLICY_HTTPS_ONLY = "HTTPS_ONLY";

  private static final String CONTAINER_PATH_KEY = "containerPath";
  private static final String HOST_PATH_KEY = "hostPath";
  private static final String RW_MODE = "mode";
  private static final String PARAMETER_KEY_KEY = "key";
  private static final String PARAMETER_VALUE_KEY = "value";

  protected MyriadConfiguration cfg;
  protected TaskUtils taskUtils;
  protected ExecutorCommandLineGenerator clGenerator;

  public TaskFactory() {

  }

  @Inject
  public TaskFactory(MyriadConfiguration cfg, TaskUtils taskUtils, ExecutorCommandLineGenerator clGenerator) {
    this.cfg = cfg;
    this.taskUtils = taskUtils;
    this.clGenerator = clGenerator;
  }

  public abstract MesosV1.TaskInfo createTask(ResourceOfferContainer resourceOfferContainer, MesosV1.FrameworkID frameworkId,
                                             MesosV1.TaskID taskId, NodeTask nodeTask);

  // TODO(Santosh): This is needed because the ExecutorInfo constructed
  // to launch NM needs to be specified to launch placeholder tasks for
  // yarn containers (for fine grained scaling).
  // If mesos supports just specifying the 'ExecutorId' without the full
  // ExecutorInfo, we wouldn't need this interface method.
  public abstract MesosV1.ExecutorInfo getExecutorInfoForSlave(ResourceOfferContainer resourceOfferContainer, MesosV1.FrameworkID frameworkId, MesosV1.CommandInfo commandInfo);

  protected Iterable<MesosV1.Volume> getVolumes(Iterable<Map<String, String>> volume) {
    return Iterables.transform(volume, new Function<Map<String, String>, MesosV1.Volume>() {
      @Nullable
      @Override
      public MesosV1.Volume apply(Map<String, String> map) {
        Preconditions.checkArgument(map.containsKey(HOST_PATH_KEY) && map.containsKey(CONTAINER_PATH_KEY));
        MesosV1.Volume.Mode mode = MesosV1.Volume.Mode.RO;
        if (map.containsKey(RW_MODE) && map.get(RW_MODE).toLowerCase().equals("rw")) {
          mode = MesosV1.Volume.Mode.RW;
        }
        MesosV1.Volume volume = new MesosV1.Volume();
        volume.setContainer_path(map.get(CONTAINER_PATH_KEY));
        volume.setHost_path(map.get(HOST_PATH_KEY));
        volume.setMode(mode);
        return volume;
      }
    });
  }

  protected Iterable<MesosV1.Parameter> getParameters(Iterable<Map<String, String>> params) {
    Preconditions.checkNotNull(params);
    return Iterables.transform(params, new Function<Map<String, String>, MesosV1.Parameter>() {
      @Override
      public MesosV1.Parameter apply(Map<String, String> parameter) {
        Preconditions.checkNotNull(parameter, "Null parameter");
        Preconditions.checkState(parameter.containsKey(PARAMETER_KEY_KEY), "Missing key");
        Preconditions.checkState(parameter.containsKey(PARAMETER_VALUE_KEY), "Missing value");
        MesosV1.Parameter param = new MesosV1.Parameter();
        param.setKey(parameter.get(PARAMETER_KEY_KEY));
        param.setValue(PARAMETER_VALUE_KEY);
        return param;
      }
    });
  }

  protected MesosV1.ContainerInfo.DockerInfo getDockerInfo(MyriadDockerConfiguration dockerConfiguration) {
    Preconditions.checkArgument(dockerConfiguration.getNetwork().equals("HOST"), "Currently only host networking supported");
    MesosV1.ContainerInfo.DockerInfo dockerBuilder = new MesosV1.ContainerInfo.DockerInfo();
    dockerBuilder.setImage(dockerConfiguration.getImage());
    dockerBuilder.setForce_pull_image(dockerConfiguration.getForcePullImage());
    dockerBuilder.setNetwork(MesosV1.ContainerInfo.DockerInfo.Network.valueOf(dockerConfiguration.getNetwork()));
    dockerBuilder.setPrivileged(dockerConfiguration.getPrivledged());
    dockerBuilder.setParameters(Lists.newArrayList(getParameters(dockerConfiguration.getParameters())));
    return dockerBuilder;
  }

  /**
   * Builds a ContainerInfo Object
   *
   * @return ContainerInfo
   */
  protected MesosV1.ContainerInfo getContainerInfo() {
    Preconditions.checkArgument(cfg.getContainerInfo().isPresent(), "ContainerConfiguration doesn't exist!");
    MyriadContainerConfiguration containerConfiguration = cfg.getContainerInfo().get();
    MesosV1.ContainerInfo containerBuilder = new MesosV1.ContainerInfo();
    containerBuilder.setType(MesosV1.ContainerInfo.Type.valueOf(containerConfiguration.getType()));
    containerBuilder.setVolumes(Lists.newArrayList(getVolumes(containerConfiguration.getVolumes())));
    if (containerConfiguration.getDockerInfo().isPresent()) {
      MyriadDockerConfiguration dockerConfiguration = containerConfiguration.getDockerInfo().get();
      containerBuilder.setDocker(getDockerInfo(dockerConfiguration));
    }
    return containerBuilder;
  }

  /**
   * Simple helper to convert Mesos Range Resource to a list of longs.
   */
  protected List<Long> rangesConverter(List<MesosV1.Resource> rangeResources) {
    List<Long> ret = new ArrayList<Long>();
    for (MesosV1.Resource range : rangeResources) {
      ret.add(range.getRanges().getRange().get(0).getBegin().longValue());
    }
    return ret;
  }
}
