package org.apache.myriad.driver.call;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.MesosDriverHttpApiCommonService;

import java.io.IOException;
import java.net.URI;

public abstract class PostAndAcceptResponse extends Call{

    public PostAndAcceptResponse(MesosDriverHttpApi driver) {
        super(driver);
    }

    @Override
    public HttpUriRequest createRequest(URI uri) {
        return MesosDriverHttpApiCommonService.createHttpPost(driver, uri, createCall());
    }

    @Override
    public Object validateResponse(HttpResponse httpResponse) throws IOException {
        return MesosDriverHttpApiCommonService.manageDefaultResponse(driver, httpResponse, HttpStatus.SC_ACCEPTED, this.getClass().getCanonicalName());
    }

}
