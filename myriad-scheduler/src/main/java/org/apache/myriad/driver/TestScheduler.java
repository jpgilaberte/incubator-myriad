package org.apache.myriad.driver;

import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class TestScheduler implements SchedulerInterfaceV1 {

    /*
delete
*/
    public static void main(String[] args){
        MesosDriverHttpApi m = new MesosDriverHttpApi(new TestScheduler(), createFarmeworkInfo(), Arrays.asList("127.0.0.1:5050"));
        try {
            m.start();
            m.stop(true);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    /*
 delete
 */
    private static MesosV1.FrameworkInfo createFarmeworkInfo(){
        MesosV1.FrameworkInfo fi = new MesosV1.FrameworkInfo();
        fi.setName("example");
        fi.setUser("foo");
        //fi.setRoles(Arrays.asList("test"));
        //fi.setCapabilities(Arrays.asList(new MesosV1.FrameworkInfo.Capability(MesosV1.FrameworkInfo.Capability.Type.MULTI_ROLE)));

        return fi;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TestScheduler.class);

    @Override
    public void registered(MesosDriver driver, MesosV1.FrameworkID frameworkId, MesosV1.MasterInfo masterInfo) {
        LOGGER.info("registered");
    }

    @Override
    public void reregistered(MesosDriver driver, MesosV1.MasterInfo masterInfo) {
        LOGGER.info("reregistered");
    }

    @Override
    public void resourceOffers(MesosDriver driver, List<MesosV1.Offer> offers) {
        LOGGER.info("resourceOffers");
    }

    @Override
    public void offerRescinded(MesosDriver driver, MesosV1.OfferID offerId) {
        LOGGER.info("offerRescinded");
    }

    @Override
    public void statusUpdate(MesosDriver driver, MesosV1.TaskStatus status) {
        LOGGER.info("statusUpdate");
    }

    @Override
    public void frameworkMessage(MesosDriver driver, MesosV1.ExecutorID executorId, MesosV1.AgentID slaveId, byte[] data) {

    }

    @Override
    public void disconnected(MesosDriver driver) {
        LOGGER.info("disconnected");
    }

    @Override
    public void slaveLost(MesosDriver driver, MesosV1.AgentID slaveId) {

    }

    @Override
    public void executorLost(MesosDriver driver, MesosV1.ExecutorID executorId, MesosV1.AgentID slaveId, int status) {

    }

    @Override
    public void error(MesosDriver driver, String message) {
        LOGGER.info("error");
    }

    @Override
    public void statusUpdateOperations(MesosDriver driver, MesosV1.OperationStatus status) {
        LOGGER.info("statusUpdateOperations");
    }

    @Override
    public void inverseResourceOffers(MesosDriver driver, List<MesosV1.Offer> offers) {
        LOGGER.info("inverseResourceOffers");
    }

    @Override
    public void inverseOfferRescinded(MesosDriver driver, MesosV1.OfferID offerId) {
        LOGGER.info("inverseOfferRescinded");
    }

    @Override
    public void failure(MesosDriver driver, MesosV1.AgentID agentId, MesosV1.ExecutorID executorId, Integer status) {
        LOGGER.info("failure");
    }

    @Override
    public void heartBeat(MesosDriver driver) {
        LOGGER.info("hearbeat");
        System.out.println("hearbeat");
    }
}
