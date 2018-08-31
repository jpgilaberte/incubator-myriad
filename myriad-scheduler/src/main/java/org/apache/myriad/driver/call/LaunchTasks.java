package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;

import java.util.Arrays;
import java.util.List;

public class LaunchTasks extends PostAndAcceptResponse{

    private final List<MesosV1.OfferID> offerIds;
    private final List<MesosV1.TaskInfo> tasks;
    private final MesosV1.Filters filters;

    public LaunchTasks(MesosDriverHttpApi driver, List<MesosV1.OfferID> offerIds, List<MesosV1.TaskInfo> tasks, MesosV1.Filters filters) {
        super(driver);
        this.offerIds = offerIds;
        this.tasks = tasks;
        this.filters = filters;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Accept accept = new SchedulerV1.Call.Accept();
        accept.setOffer_ids(offerIds);
        accept.setOperations(Arrays.asList(createOperationLaunch()));
        accept.setFilters(filters);
        call.setType(SchedulerV1.Call.Type.ACCEPT);
        call.setAccept(accept);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }

    public MesosV1.Offer.Operation createOperationLaunch(){
        MesosV1.Offer.Operation operation = new MesosV1.Offer.Operation();
        MesosV1.Offer.Operation.Launch launch = new MesosV1.Offer.Operation.Launch();
        launch.setTask_infos(tasks);
        operation.setType(MesosV1.Offer.Operation.Type.LAUNCH);
        operation.setLaunch(launch);
        return operation;
    }
}
