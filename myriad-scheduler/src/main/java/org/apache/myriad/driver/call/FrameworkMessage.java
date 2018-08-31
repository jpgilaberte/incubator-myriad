package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;

public class FrameworkMessage extends PostAndAcceptResponse{

    private final MesosV1.ExecutorID executorId;
    private final MesosV1.AgentID agentId;
    private final byte[] data;

    public FrameworkMessage(MesosDriverHttpApi driver, MesosV1.ExecutorID executorId, MesosV1.AgentID agentId, byte[] data) {
        super(driver);
        this.executorId = executorId;
        this.agentId = agentId;
        this.data = data;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Message message = new SchedulerV1.Call.Message();
        message.setAgent_id(agentId);
        message.setExecutor_id(executorId);
        message.setData(data);
        call.setType(SchedulerV1.Call.Type.MESSAGE);
        call.setMessage(message);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
