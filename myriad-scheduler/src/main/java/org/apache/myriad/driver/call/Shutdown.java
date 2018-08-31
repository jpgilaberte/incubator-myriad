package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;

public class Shutdown extends PostAndAcceptResponse{

    private final MesosV1.ExecutorID executorId;
    private final MesosV1.AgentID agentId;

    public Shutdown(MesosDriverHttpApi driver, MesosV1.ExecutorID executorId, MesosV1.AgentID agentId) {
        super(driver);
        this.executorId = executorId;
        this.agentId = agentId;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Shutdown shutdown = new SchedulerV1.Call.Shutdown();
        shutdown.setExecutor_id(executorId);
        shutdown.setAgent_id(agentId);
        call.setType(SchedulerV1.Call.Type.SHUTDOWN);
        call.setShutdown(shutdown);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
