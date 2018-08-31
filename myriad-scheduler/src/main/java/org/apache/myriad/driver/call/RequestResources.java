package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;
import java.util.List;


public class RequestResources extends PostAndAcceptResponse{

    protected final List<MesosV1.Request> requests;

    public RequestResources(final MesosDriverHttpApi driver, final List<MesosV1.Request> requests){
        super(driver);
        this.requests = requests;
    }

    @Override
    protected SchedulerV1.Call createCall(){
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Request request = new SchedulerV1.Call.Request();
        request.setRequests(requests);
        call.setType(SchedulerV1.Call.Type.REQUEST);
        call.setRequest(request);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
