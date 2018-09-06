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
import org.apache.commons.lang3.StringUtils;
import org.apache.myriad.configuration.MyriadConfiguration;
import org.apache.myriad.configuration.ServiceConfiguration;
import org.apache.myriad.driver.model.MesosV1;

import java.util.*;

/**
 * CommandLineGenerator for any aux service launched by Myriad as binary distro
 */
public class ServiceCommandLineGenerator extends ExecutorCommandLineGenerator {

  public static final String ENV_HADOOP_OPTS = "HADOOP_OPTS";

  private String baseCmd;

  public ServiceCommandLineGenerator(MyriadConfiguration cfg) {
    this.myriadConfiguration = cfg;
    myriadExecutorConfiguration = cfg.getMyriadExecutorConfiguration();
    generateStaticCommandLine();
  }

  protected void generateStaticCommandLine() {
    MesosV1.CommandInfo builder = new MesosV1.CommandInfo();

    builder.setUris(getUris());
    builder.setUser(getUser());
    staticCommandInfo = builder;

    StringBuilder cmdLine = new StringBuilder();
    appendDistroExtractionCommands(cmdLine);
    appendUserSudo(cmdLine);
    baseCmd = cmdLine.toString();
  }

  @Override
  public MesosV1.CommandInfo generateCommandLine(ServiceResourceProfile profile,
                                                ServiceConfiguration serviceConfiguration,
                                                Collection<Long> ports) {
    MesosV1.CommandInfo builder = staticCommandInfo;
    builder.setValue(String.format(CMD_FORMAT, baseCmd + " " + serviceConfiguration.getCommand().get()));
    builder.setEnvironment(generateEnvironment(profile, ports));
    return builder;
  }

  protected MesosV1.Environment generateEnvironment(ServiceResourceProfile serviceResourceProfile, Collection<Long> ports) {
    Map<String, String> yarnEnv = myriadConfiguration.getYarnEnvironment();
    MesosV1.Environment builder = new MesosV1.Environment();

    builder.setVariables(Lists.newArrayList(Iterables.transform(yarnEnv.entrySet(), new Function<Map.Entry<String, String>, MesosV1.Environment.Variable>() {
      public MesosV1.Environment.Variable apply(Map.Entry<String, String> x) {
        MesosV1.Environment.Variable var = new MesosV1.Environment.Variable();
        var.setName(x.getKey());
        var.setValue(x.getValue());
        return var;
      }
    })));

    StringBuilder hadoopOpts = new StringBuilder();
    String rmHostName = System.getProperty(KEY_YARN_RM_HOSTNAME);

    if (StringUtils.isNotEmpty(rmHostName)) {
      addJavaOpt(hadoopOpts, KEY_YARN_RM_HOSTNAME, rmHostName);
    }

    if (yarnEnv.containsKey(KEY_YARN_HOME)) {
      addJavaOpt(hadoopOpts, KEY_YARN_HOME, yarnEnv.get("YARN_HOME"));
    }

    Map<String, Long> portsMap = serviceResourceProfile.getPorts();
    Preconditions.checkState(portsMap.size() == ports.size());
    Iterator itr = ports.iterator();
    for (String portProperty : portsMap.keySet()) {
      addJavaOpt(hadoopOpts, portProperty, ALL_LOCAL_IPV4ADDR + itr.next());
    }

    if (myriadConfiguration.getYarnEnvironment().containsKey(ENV_HADOOP_OPTS)) {
      hadoopOpts.append(" ").append(yarnEnv.get(ENV_HADOOP_OPTS));
    }

    MesosV1.Environment.Variable var = new MesosV1.Environment.Variable();
    var.setName(ENV_HADOOP_OPTS);
    var.setValue(hadoopOpts.toString());

    builder.setVariables(new ArrayList(Collections.singleton(var)));

    return builder;
  }

}
