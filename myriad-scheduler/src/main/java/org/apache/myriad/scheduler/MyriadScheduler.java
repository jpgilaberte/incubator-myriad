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

import java.util.List;

import javax.inject.Inject;

import org.apache.myriad.configuration.MyriadConfiguration;
import org.apache.myriad.driver.MesosDriver;
import org.apache.myriad.driver.SchedulerInterfaceV1;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.scheduler.event.DisconnectedEvent;
import org.apache.myriad.scheduler.event.ErrorEvent;
import org.apache.myriad.scheduler.event.ExecutorLostEvent;
import org.apache.myriad.scheduler.event.FrameworkMessageEvent;
import org.apache.myriad.scheduler.event.OfferRescindedEvent;
import org.apache.myriad.scheduler.event.ReRegisteredEvent;
import org.apache.myriad.scheduler.event.RegisteredEvent;
import org.apache.myriad.scheduler.event.ResourceOffersEvent;
import org.apache.myriad.scheduler.event.SlaveLostEvent;
import org.apache.myriad.scheduler.event.StatusUpdateEvent;

import com.lmax.disruptor.EventTranslator;

/**
 * The Myriad implementation of the Mesos Scheduler callback interface, where the method implementations
 * publish Myriad framework events corresponding to the Mesos callbacks.
 */
public class MyriadScheduler implements SchedulerInterfaceV1 {
  private org.apache.myriad.DisruptorManager disruptorManager;

  @Inject
  public MyriadScheduler(final MyriadConfiguration cfg, final org.apache.myriad.DisruptorManager disruptorManager) {
    this.disruptorManager = disruptorManager;
  }

  /**
   * Publishes a RegisteredEvent
   */
  @Override
  public void registered(final MesosDriver driver, final MesosV1.FrameworkID frameworkId, final MesosV1.MasterInfo masterInfo) {
    disruptorManager.getRegisteredEventDisruptor().publishEvent(new EventTranslator<RegisteredEvent>() {
      @Override
      public void translateTo(RegisteredEvent event, long sequence) {
        event.setDriver(driver);
        event.setFrameworkId(frameworkId);
        event.setMasterInfo(masterInfo);
      }
    });
  }

  /**
   * Publishes a ReRegisteredEvent
   */
  @Override
  public void reregistered(final MesosDriver driver, final MesosV1.MasterInfo masterInfo) {
    disruptorManager.getReRegisteredEventDisruptor().publishEvent(new EventTranslator<ReRegisteredEvent>() {
      @Override
      public void translateTo(ReRegisteredEvent event, long sequence) {
        event.setDriver(driver);
        event.setMasterInfo(masterInfo);
      }
    });
  }

  /**
   * Publishes a ResourceOffersEvent
   */
  @Override
  public void resourceOffers(final MesosDriver driver, final List<MesosV1.Offer> offers) {
    disruptorManager.getResourceOffersEventDisruptor().publishEvent(new EventTranslator<ResourceOffersEvent>() {
      @Override
      public void translateTo(ResourceOffersEvent event, long sequence) {
        event.setDriver(driver);
        event.setOffers(offers);
      }
    });
  }

  /**
   * Publishes a OfferRescindedEvent
   */
  @Override
  public void offerRescinded(final MesosDriver driver, final MesosV1.OfferID offerId) {
    disruptorManager.getOfferRescindedEventDisruptor().publishEvent(new EventTranslator<OfferRescindedEvent>() {
      @Override
      public void translateTo(OfferRescindedEvent event, long sequence) {
        event.setDriver(driver);
        event.setOfferId(offerId);
      }
    });
  }

  /**
   * Publishes a StatusUpdateEvent
   */
  @Override
  public void statusUpdate(final MesosDriver driver, final MesosV1.TaskStatus status) {
    disruptorManager.getStatusUpdateEventDisruptor().publishEvent(new EventTranslator<StatusUpdateEvent>() {
      @Override
      public void translateTo(StatusUpdateEvent event, long sequence) {
        event.setDriver(driver);
        event.setStatus(status);
      }
    });
  }

  /**
   * Publishes FrameworkMessageEvent
   */
  @Override
  public void frameworkMessage(final MesosDriver driver, final MesosV1.ExecutorID executorId, final MesosV1.AgentID slaveId,
                               final byte[] bytes) {
    disruptorManager.getFrameworkMessageEventDisruptor().publishEvent(new EventTranslator<FrameworkMessageEvent>() {
      @Override
      public void translateTo(FrameworkMessageEvent event, long sequence) {
        event.setDriver(driver);
        event.setBytes(bytes);
        event.setExecutorId(executorId);
        event.setSlaveId(slaveId);
      }
    });
  }

  /**
   * Publishes DisconnectedEvent
   */
  @Override
  public void disconnected(final MesosDriver driver) {
    disruptorManager.getDisconnectedEventDisruptor().publishEvent(new EventTranslator<DisconnectedEvent>() {
      @Override
      public void translateTo(DisconnectedEvent event, long sequence) {
        event.setDriver(driver);
      }
    });
  }

  /**
   * Publishes SlaveLostEvent
   */
  @Override
  public void slaveLost(final MesosDriver driver, final MesosV1.AgentID slaveId) {
    disruptorManager.getSlaveLostEventDisruptor().publishEvent(new EventTranslator<SlaveLostEvent>() {
      @Override
      public void translateTo(SlaveLostEvent event, long sequence) {
        event.setDriver(driver);
        event.setSlaveId(slaveId);
      }
    });
  }

  /**
   * Publishes ExecutorLostEvent
   */
  @Override
  public void executorLost(final MesosDriver driver, final MesosV1.ExecutorID executorId, final MesosV1.AgentID slaveId,
                           final int exitStatus) {
    disruptorManager.getExecutorLostEventDisruptor().publishEvent(new EventTranslator<ExecutorLostEvent>() {
      @Override
      public void translateTo(ExecutorLostEvent event, long sequence) {
        event.setDriver(driver);
        event.setExecutorId(executorId);
        event.setSlaveId(slaveId);
        event.setExitStatus(exitStatus);
      }
    });
  }

  /**
   * Publishes ErrorEvent
   */
  @Override
  public void error(final MesosDriver driver, final String message) {
    disruptorManager.getErrorEventDisruptor().publishEvent(new EventTranslator<ErrorEvent>() {
      @Override
      public void translateTo(ErrorEvent event, long sequence) {
        event.setDriver(driver);
        event.setMessage(message);
      }
    });
  }

  @Override
  public void statusUpdateOperations(MesosDriver driver, MesosV1.OperationStatus status) {

  }

  @Override
  public void inverseResourceOffers(MesosDriver driver, List<MesosV1.Offer> offers) {

  }

  @Override
  public void inverseOfferRescinded(MesosDriver driver, MesosV1.OfferID offerId) {

  }

  @Override
  public void failure(MesosDriver driver, MesosV1.AgentID agentId, MesosV1.ExecutorID executorId, Integer status) {

  }

  @Override
  public void heartBeat(MesosDriver driver) {

  }
}
