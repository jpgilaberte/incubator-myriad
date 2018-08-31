package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.SchedulerV1;

import java.util.List;

public class Revive extends PostAndAcceptResponse{

    private final List<String> roles;

    public Revive(MesosDriverHttpApi driver, List<String> roles) {
        super(driver);
        this.roles = roles;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Revive revive = new SchedulerV1.Call.Revive();
        revive.setRoles(roles);
        call.setType(SchedulerV1.Call.Type.REVIVE);
        call.setRevive(revive);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
