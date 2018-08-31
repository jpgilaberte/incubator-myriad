package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.SchedulerV1;

import java.util.List;

public class SuppressOffers extends PostAndAcceptResponse{

    private final List<String> roles;

    public SuppressOffers(MesosDriverHttpApi driver, List<String> roles) {
        super(driver);
        this.roles = roles;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Suppress suppress = new SchedulerV1.Call.Suppress();
        suppress.setRoles(roles);
        call.setType(SchedulerV1.Call.Type.SUPPRESS);
        call.setSuppress(suppress);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
