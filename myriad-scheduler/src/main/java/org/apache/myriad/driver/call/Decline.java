package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class Decline extends PostAndAcceptResponse{
    private static final Logger LOGGER = LoggerFactory.getLogger(Decline.class);
    private final List<MesosV1.OfferID> offerId;
    private final MesosV1.Filters filters;

    public Decline(MesosDriverHttpApi driver, List<MesosV1.OfferID> offerId, MesosV1.Filters filters) {
        super(driver);
        this.offerId = offerId;
        this.filters = filters;
    }

    @Override
    protected SchedulerV1.Call createCall(){
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Decline decline = new SchedulerV1.Call.Decline();
        decline.setOffer_ids(offerId);
        decline.setFilters(filters);
        call.setType(SchedulerV1.Call.Type.DECLINE);
        call.setFramework_id(driver.getFrameworkId());
        call.setDecline(decline);
        return call;
    }
}
