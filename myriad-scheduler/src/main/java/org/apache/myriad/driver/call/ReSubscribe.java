package org.apache.myriad.driver.call;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.RecordIO;
import org.apache.myriad.driver.model.SchedulerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class ReSubscribe extends Subscribe{

    private static final Logger LOGGER = LoggerFactory.getLogger(ReSubscribe.class);

    public ReSubscribe(MesosDriverHttpApi driver) {
        super(driver);
    }

    @Override
    public Object validateResponse(HttpResponse httpResponse) {
        InputStream is = null;
        RecordIO rio;
        SchedulerV1.Event event;
        try {
            if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()) {
                is = httpResponse.getEntity().getContent();
                rio = new RecordIO(is);
                event = new Gson().fromJson(rio.read(), SchedulerV1.Event.class);
                if (event.getType() == SchedulerV1.Event.Type.SUBSCRIBED) {
                    driver.setMesosStreamId(httpResponse.getLastHeader("Mesos-Stream-Id").getValue());
                    driver.getScheduler().reregistered(driver, event.getSubscribed().getMaster_info());
                } else {
                    LOGGER.error("Mesos HttpResponse error event in connection: ", event.getError().getMessage());
                    driver.getScheduler().error(driver, event.getError().getMessage());
                    is = null;
                }
            } else {
                LOGGER.error("Mesos HttpResponse error event in connection: {}, HttpCode: {}", IOUtils.toString(httpResponse.getEntity().getContent()), httpResponse.getStatusLine().getStatusCode());
                driver.getScheduler().error(driver, "Mesos HttpResponse error event in connection: " + IOUtils.toString(httpResponse.getEntity().getContent()) + ", HttpCode: " + httpResponse.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Mesos HttpResponse error event in connection: ", e.getMessage());
            driver.getScheduler().error(driver, e.getMessage());
            is = null;
        }
        return is;
    }
}
