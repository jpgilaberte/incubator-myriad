package org.apache.myriad.driver.call;

import com.google.gson.Gson;
import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.SchedulerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TearDown extends PostAndAcceptResponse{

    private static final Logger LOGGER = LoggerFactory.getLogger(Subscribe.class);

    public TearDown(MesosDriverHttpApi driver){
        super(driver);
    }

    @Override
    protected SchedulerV1.Call createCall(){
        SchedulerV1.Call call = new SchedulerV1.Call();
        call.setType(SchedulerV1.Call.Type.TEARDOWN);
        call.setFramework_id(driver.getFrameworkId());
        if (LOGGER.isDebugEnabled()){
            LOGGER.debug("TearDown call: {}", new Gson().toJson(call));
        }
        return call;
    }
}
