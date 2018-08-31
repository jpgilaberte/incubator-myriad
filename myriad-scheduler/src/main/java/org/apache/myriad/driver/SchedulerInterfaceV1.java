package org.apache.myriad.driver;

import org.apache.myriad.driver.model.MesosV1;

import java.util.List;

public interface SchedulerInterfaceV1 {

    void registered(MesosDriver driver, MesosV1.FrameworkID frameworkId, MesosV1.MasterInfo masterInfo);

    void reregistered(MesosDriver driver, MesosV1.MasterInfo masterInfo);

    void resourceOffers(MesosDriver driver, List<MesosV1.Offer> offers);

    void offerRescinded(MesosDriver driver, MesosV1.OfferID offerId);


    void statusUpdate(MesosDriver driver, MesosV1.TaskStatus status);

    void frameworkMessage(MesosDriver driver, MesosV1.ExecutorID executorId, MesosV1.AgentID slaveId, byte[] data);


    void disconnected(MesosDriver driver);


    void slaveLost(MesosDriver driver, MesosV1.AgentID slaveId);


    void executorLost(MesosDriver driver, MesosV1.ExecutorID executorId, MesosV1.AgentID slaveId, int status);

    void error(MesosDriver driver, String message);

    void statusUpdateOperations(MesosDriver driver, MesosV1.OperationStatus status);

    void inverseResourceOffers(MesosDriver driver, List<MesosV1.Offer> offers);

    void inverseOfferRescinded(MesosDriver driver, MesosV1.OfferID offerId);

    void failure(MesosDriver driver, MesosV1.AgentID agentId, MesosV1.ExecutorID executorId, Integer status);

    void heartBeat(MesosDriver driver);

}
