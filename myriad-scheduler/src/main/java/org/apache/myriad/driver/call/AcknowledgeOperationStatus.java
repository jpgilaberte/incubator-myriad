package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;

public class AcknowledgeOperationStatus extends PostAndAcceptResponse{

    private final MesosV1.OperationStatus operationStatus;
    private final MesosV1.AgentID agentId;
    private final MesosV1.ResourceProviderID resourceProviderId;

    public AcknowledgeOperationStatus(MesosDriverHttpApi driver, MesosV1.AgentID agentId, MesosV1.ResourceProviderID resourceProviderId, MesosV1.OperationStatus operationStatus) {
        super(driver);
        this.operationStatus = operationStatus;
        this.agentId = agentId;
        this.resourceProviderId = resourceProviderId;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.AcknowledgeOperationStatus acknowledgeOperationStatus = new SchedulerV1.Call.AcknowledgeOperationStatus();
        acknowledgeOperationStatus.setAgent_id(agentId);
        acknowledgeOperationStatus.setResource_provider_id(resourceProviderId);
        acknowledgeOperationStatus.setOperation_id(operationStatus.getOperation_id());
        acknowledgeOperationStatus.setUuid(operationStatus.getUuid());
        call.setType(SchedulerV1.Call.Type.ACKNOWLEDGE_OPERATION_STATUS);
        call.setAcknowledge_operation_status(acknowledgeOperationStatus);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
