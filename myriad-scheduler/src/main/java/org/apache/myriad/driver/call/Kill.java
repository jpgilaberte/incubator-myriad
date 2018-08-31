package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;

public class Kill extends PostAndAcceptResponse{

    private final MesosV1.TaskID taskId;
    private final MesosV1.AgentID agent_id;
    private final MesosV1.KillPolicy kill_policy;

    public Kill(MesosDriverHttpApi driver, MesosV1.TaskID taskId, MesosV1.AgentID agent_id, MesosV1.KillPolicy kill_policy) {
        super(driver);
        this.taskId = taskId;
        this.agent_id = agent_id;
        this.kill_policy = kill_policy;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Kill kill = new SchedulerV1.Call.Kill();
        kill.setTask_id(taskId);
        kill.setAgent_id(agent_id);
        kill.setKill_policy(kill_policy);
        call.setType(SchedulerV1.Call.Type.KILL);
        call.setKill(kill);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
