package org.apache.myriad.driver;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.myriad.driver.call.Call;
import org.apache.myriad.driver.model.SchedulerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;


public class MesosDriverHttpApiCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MesosDriverHttpApiCommonService.class);

    public static HttpResponse connection (final DefaultHttpClient httpClient, final HttpContext httpContext, final HttpUriRequest httpsPost) throws IOException {
        return httpClient.execute(httpsPost, httpContext);
    }

    public static Object call(final MesosDriverHttpApi driver, final Call call){
        Object response = null;
        DefaultHttpClient httpClient;
        HttpContext httpContext;
        URI uri;

        httpClient = new DefaultHttpClient();
        httpClient.setRedirectStrategy(new LaxRedirectStrategy());
        httpContext = new BasicHttpContext();

        for (String mesosMaster: driver.getMesosMasterList()) {
            try{
                uri = new URI("http://" + mesosMaster + "/api/v1/scheduler");
                response = call.validateResponse(connection(httpClient, httpContext, call.createRequest(uri)));
                setMesosLeader(driver, httpContext, uri);
                //TODO:review
                if (response != null) break;
            } catch (Exception e) {
                LOGGER.error("Master connection error: ", e.getMessage());
                driver.getScheduler().error(driver, "Master connection error: " + e.getMessage());
                response = null;
            }
        }

        return response;
    }

    private static void setMesosLeader(final MesosDriverHttpApi driver, final HttpContext httpContext, final URI uri){
        HttpHost host = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        if ( ! host.getHostName().equals(uri.getHost()) || null == driver.getMesosLeader()){
            driver.setMesosLeader(host.getHostName() + ":" + host.getPort());
        }
    }

    public static Boolean manageDefaultResponse(final MesosDriverHttpApi driver, final HttpResponse httpResponse, final Integer httpStatus, final String callName) throws IOException {
        boolean success = false;
        if (httpStatus == httpResponse.getStatusLine().getStatusCode()){
            success = true;
            LOGGER.info("{} call return success", callName);
        } else {
            String errorMessage = callName+ " call return error code: " +IOUtils.toString(httpResponse.getEntity().getContent())+ ", and message: " +httpResponse.getStatusLine().getStatusCode();
            LOGGER.error(errorMessage);
            driver.getScheduler().error(driver, errorMessage);
        }
        return success;
    }

    public static HttpPost createHttpPost(final MesosDriverHttpApi driver, final URI uri, final SchedulerV1.Call call){
        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpPost.addHeader("Mesos-Stream-Id", driver.getMesosStreamId());
        httpPost.setEntity(new StringEntity(new Gson().toJson(call), ContentType.APPLICATION_JSON));
        return httpPost;
    }
}
