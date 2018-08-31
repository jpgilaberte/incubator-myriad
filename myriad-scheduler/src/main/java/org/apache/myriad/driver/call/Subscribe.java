package org.apache.myriad.driver.call;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.RecordIO;
import org.apache.myriad.driver.model.SchedulerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;


public class Subscribe extends Call {

    private static final Logger LOGGER = LoggerFactory.getLogger(Subscribe.class);

    public Subscribe(final MesosDriverHttpApi driver){
        super(driver);
    }

    @Override
    public HttpUriRequest createRequest(final URI uri) {
        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpPost.setEntity(requestEntityCall());
        return httpPost;
    }

    @Override
    public Object validateResponse(HttpResponse httpResponse) {
        InputStream is = null;
        RecordIO rio;
        SchedulerV1.Event event;
        try{
            if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()){
                is = httpResponse.getEntity().getContent();
                rio = new RecordIO(is);
                event = new Gson().fromJson(rio.read(), SchedulerV1.Event.class);
                if (event.getType() == SchedulerV1.Event.Type.SUBSCRIBED){
                    driver.setMesosStreamId(httpResponse.getLastHeader("Mesos-Stream-Id").getValue());
                    driver.setFrameworkId(event.getSubscribed().getFramework_id());
                    driver.getScheduler().registered(driver, event.getSubscribed().getFramework_id(), event.getSubscribed().getMaster_info());
                } else {
                    LOGGER.error("Mesos HttpResponse error event in connection: ", event.getError().getMessage());
                    driver.getScheduler().error(driver, event.getError().getMessage());
                    is = null;
                }
            } else {
                LOGGER.error("Mesos HttpResponse error event in connection: {}, HttpCode: {}",  IOUtils.toString(httpResponse.getEntity().getContent()), httpResponse.getStatusLine().getStatusCode());
                driver.getScheduler().error(driver, "Mesos HttpResponse error event in connection: " + IOUtils.toString(httpResponse.getEntity().getContent()) + ", HttpCode: " + httpResponse.getStatusLine().getStatusCode());
            }
        }catch (Exception e){
            LOGGER.error("Mesos HttpResponse error event in connection: ", e.getMessage());
            driver.getScheduler().error(driver, e.getMessage());
            is = null;
        }
        return is;
    }

    private  StringEntity requestEntityCall(){
        return new StringEntity(new Gson().toJson(createCall()), ContentType.APPLICATION_JSON);
    }

    @Override
    protected SchedulerV1.Call createCall(){
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Subscribe subscribe = new SchedulerV1.Call.Subscribe();
        subscribe.setFramework_info(driver.getFrameworkInfo());
        call.setType(SchedulerV1.Call.Type.SUBSCRIBE);
        call.setSubscribe(subscribe);
        call.setFramework_id(driver.getFrameworkInfo().getId());
        return call;
    }
}
