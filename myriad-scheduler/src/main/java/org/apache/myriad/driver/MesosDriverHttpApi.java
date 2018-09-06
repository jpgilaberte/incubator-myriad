package org.apache.myriad.driver;

import org.apache.myriad.driver.call.*;
import org.apache.myriad.driver.model.MesosV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


public class MesosDriverHttpApi implements MesosDriver{

    private static final Logger LOGGER = LoggerFactory.getLogger(MesosDriverHttpApi.class);
    private final MesosV1.FrameworkInfo frameworkInfo;
    private final SchedulerInterfaceV1 scheduler;
    private final List<String> mesosMasterList;
    private volatile String mesosLeader;
    private volatile String mesosStreamId;
    private volatile MesosV1.Status driverStatus;
    private volatile MesosV1.FrameworkID frameworkId;

    private Thread threadStream;

    public MesosDriverHttpApi (SchedulerInterfaceV1 scheduler, MesosV1.FrameworkInfo frameworkInfo, List<String> mesosMasterList){
        this.scheduler = scheduler;
        this.frameworkInfo = frameworkInfo;
        this.mesosMasterList = mesosMasterList;
        driverStatus = MesosV1.Status.DRIVER_NOT_STARTED;
    }

    @Override
    public MesosV1.Status start() {
        if (driverStatus != MesosV1.Status.DRIVER_RUNNING) {
            LOGGER.info("Starting driver");
            InputStream inputStream = (InputStream) MesosDriverHttpApiCommonService.call(this, new Subscribe(this));
            if (null == inputStream){
                driverStatus = MesosV1.Status.DRIVER_ABORTED;
                LOGGER.info("Aborting driver");
                return driverStatus;
            }
            threadStream = new Thread(new MesosDriverHttpApiStream(this, inputStream), "STREAM_API");
            threadStream.start();
            driverStatus = MesosV1.Status.DRIVER_RUNNING;
            LOGGER.info("Driver started ", driverStatus);
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status stop(boolean failover) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING) {
            driverStatus = MesosV1.Status.DRIVER_STOPPED;
            threadStream.interrupt();
            if (!failover){
                MesosDriverHttpApiCommonService.call(this, new TearDown(this));
            }
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status abort() {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING) {
            driverStatus = MesosV1.Status.DRIVER_ABORTED;
            threadStream.interrupt();
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status join() {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING) {
            driverStatus = MesosV1.Status.DRIVER_STOPPED;
            threadStream.interrupt();
            try {
                threadStream.join();
            } catch (InterruptedException e) {
                driverStatus = MesosV1.Status.DRIVER_ABORTED;
                e.printStackTrace();
            }
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status run() {
        if (driverStatus != MesosV1.Status.DRIVER_RUNNING) {
            LOGGER.info("Running driver");
            InputStream inputStream = (InputStream) MesosDriverHttpApiCommonService.call(this, new Subscribe(this));
            if (null == inputStream){
                driverStatus = MesosV1.Status.DRIVER_ABORTED;
                LOGGER.info("Aborting driver");
                return driverStatus;
            }
            threadStream = new Thread(new MesosDriverHttpApiStream(this, inputStream));
            driverStatus = MesosV1.Status.DRIVER_RUNNING;
            LOGGER.info("Driver run ", driverStatus);
            threadStream.run();
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status requestResources(List<MesosV1.Request> requests) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new RequestResources(this, requests));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status launchTasks(MesosV1.OfferID offerId, List<MesosV1.TaskInfo> tasks, MesosV1.Filters filters) {
        return launchTasks(Arrays.asList(offerId), tasks, filters);
    }

    @Override
    public MesosV1.Status launchTasks(List<MesosV1.OfferID> offerIds, List<MesosV1.TaskInfo> tasks, MesosV1.Filters filters) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new LaunchTasks(this, offerIds, tasks, filters));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status killTask(MesosV1.TaskID taskId, MesosV1.AgentID agent_id, MesosV1.KillPolicy kill_policy) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new Kill(this, taskId, agent_id, kill_policy));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status acceptOffers(List<MesosV1.OfferID> offerIds, List<MesosV1.Offer.Operation> operations, MesosV1.Filters filters) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new AcceptOffers(this, offerIds, operations, filters));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status declineOffer(List<MesosV1.OfferID> offerId, MesosV1.Filters filters) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new Decline(this, offerId, filters));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status reviveOffers() {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new Revive(this, frameworkInfo.getRoles()));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status suppressOffers() {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new SuppressOffers(this, frameworkInfo.getRoles()));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status acknowledgeStatusUpdate(MesosV1.TaskStatus status) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new Acknowledge(this, status));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status sendFrameworkMessage(MesosV1.ExecutorID executorId, MesosV1.AgentID agentId, byte[] data) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new FrameworkMessage(this, executorId, agentId, data));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status acknowledgeOperationStatus( MesosV1.AgentID agentId, MesosV1.ResourceProviderID resourceProviderId, MesosV1.OperationStatus operationStaus) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new AcknowledgeOperationStatus(this, agentId, resourceProviderId, operationStaus));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status reconcileTasks(List<MesosV1.TaskStatus> statuses) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new Reconcile(this, statuses));
        }
        return driverStatus;
    }

    @Override
    public MesosV1.Status shutdown(MesosV1.ExecutorID executorId, MesosV1.AgentID agentId) {
        if (driverStatus == MesosV1.Status.DRIVER_RUNNING){
            MesosDriverHttpApiCommonService.call(this, new Shutdown(this, executorId, agentId));
        }
        return driverStatus;
    }

    public MesosV1.FrameworkInfo getFrameworkInfo() {
        return frameworkInfo;
    }

    public SchedulerInterfaceV1 getScheduler() {
        return scheduler;
    }

    public String getMesosLeader() {
        return mesosLeader;
    }

    public void setMesosLeader(String mesosLeader) {
        this.mesosLeader = mesosLeader;
    }

    public String getMesosStreamId() {
        return mesosStreamId;
    }

    public void setMesosStreamId(String mesosStreamId) {
        this.mesosStreamId = mesosStreamId;
    }

    public List<String> getMesosMasterList() {
        return mesosMasterList;
    }

    public MesosV1.Status getDriverStatus() {
        return driverStatus;
    }

    public void setDriverStatus(MesosV1.Status driverStatus) {
        this.driverStatus = driverStatus;
    }

    public void setFrameworkId(MesosV1.FrameworkID frameworkId) {
        this.frameworkId = frameworkId;
    }

    public MesosV1.FrameworkID getFrameworkId(){
        return frameworkId;
    }
}
