package org.apache.myriad.driver.call;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.SchedulerV1;
import java.io.IOException;
import java.net.URI;


public abstract class Call {
        protected final MesosDriverHttpApi driver;
        public Call(final MesosDriverHttpApi driver){
                this.driver = driver;
        }
        public abstract HttpUriRequest createRequest(final URI uri);
        public abstract Object validateResponse(final HttpResponse httpResponse) throws IOException;
        protected abstract SchedulerV1.Call createCall();
}
