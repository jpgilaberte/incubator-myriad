package org.apache.myriad.driver;

import com.google.gson.Gson;
import org.apache.myriad.driver.call.ExponentialBackOff;
import org.apache.myriad.driver.call.ReSubscribe;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;


public class MesosDriverHttpApiStream implements Runnable{

    private static final Logger LOGGER = LoggerFactory.getLogger(MesosDriverHttpApi.class);
    final MesosDriverHttpApi driver;
    InputStream stream;

    public MesosDriverHttpApiStream(final MesosDriverHttpApi driver, InputStream stream){
        this.driver = driver;
        this.stream = stream;
    }

    private void reConnect(){
        driver.setDriverStatus(MesosV1.Status.DRIVER_STOPPED);
        LOGGER.warn("Reconnecting driver...");
        ExponentialBackOff ebo = new ExponentialBackOff(4, -1);
        while (ebo.lockUntilNextStep()){
            LOGGER.warn("Reconnecting driver...");
            stream = (InputStream) MesosDriverHttpApiCommonService.call(driver, new ReSubscribe(driver));
            if (stream != null){
                driver.setDriverStatus(MesosV1.Status.DRIVER_RUNNING);
                break;
            }
            LOGGER.error("Wrong Reconnecting driver step... Check mesos master state... Reconnecting now...");
        }
        if (driver.getDriverStatus() != MesosV1.Status.DRIVER_RUNNING){
            driver.setDriverStatus(MesosV1.Status.DRIVER_ABORTED);
            LOGGER.error("Wrong Reconnecting driver step... Check mesos master state... Aborting driver...");

        }
    }

    @Override
    public void run() {
        Boolean bPrincipal = new Boolean(true);
        Gson gs = new Gson();
        SchedulerV1.Event event;
        RecordIO rio;
        while(bPrincipal){
            try {
                rio = new RecordIO(stream);
                boolean bSecond = true;

                while (bSecond){
                    event = gs.fromJson(rio.read(), SchedulerV1.Event.class);
                    switch (event.getType()) {
                        case UNKNOWN:
                            break;
                        case SUBSCRIBED: driver.getScheduler().registered(driver, event.getSubscribed().getFramework_id(), event.getSubscribed().getMaster_info());
                            break;
                        case OFFERS: driver.getScheduler().resourceOffers(driver, event.getOffers().getOffers());
                            break;
                        case INVERSE_OFFERS: driver.getScheduler().inverseResourceOffers(driver, event.getOffers().getOffers());
                            break;
                        case RESCIND: driver.getScheduler().offerRescinded(driver, event.getRescind().getOffer_id());
                            break;
                        case RESCIND_INVERSE_OFFERS: driver.getScheduler().inverseOfferRescinded(driver, event.getRescind().getOffer_id());
                            break;
                        case UPDATE: driver.getScheduler().statusUpdate(driver, event.getUpdate().getStatus());
                            break;
                        case UPDATE_OPERATION_STATUS: driver.getScheduler().statusUpdateOperations(driver, event.getUpdate_operation_status().getStatus());
                            break;
                        case MESSAGE: driver.getScheduler().frameworkMessage(driver, event.getMessage().getExecutor_id(), event.getMessage().getAgent_id(), event.getMessage().getData());
                            break;
                        case FAILURE: driver.getScheduler().failure(driver, event.getFailure().getAgent_id(), event.getFailure().getExecutor_id(), event.getFailure().getStatus());
                            break;
                        case ERROR: driver.getScheduler().error(driver, event.getError().getMessage());
                            break;
                        case HEARTBEAT: driver.getScheduler().heartBeat(driver);
                            break;
                        default:
                            break;
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        bSecond = false;
                        bPrincipal = false;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Closed stream...", e);
                if (Thread.currentThread().isInterrupted()) {
                    bPrincipal = false;
                } else {
                    reConnect();
                }
            }
        }
    }
}

