package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;

public class Acknowledge extends PostAndAcceptResponse{

    private final MesosV1.TaskStatus status;

    public Acknowledge(MesosDriverHttpApi driver, MesosV1.TaskStatus status) {
        super(driver);
        this.status = status;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Acknowledge acknowledge = new SchedulerV1.Call.Acknowledge();
        acknowledge.setAgent_id(status.getAgent_id());
        acknowledge.setTask_id(status.getTask_id());
        acknowledge.setUuid(status.getUuid());
        call.setType(SchedulerV1.Call.Type.ACKNOWLEDGE);
        call.setAcknowledge(acknowledge);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
