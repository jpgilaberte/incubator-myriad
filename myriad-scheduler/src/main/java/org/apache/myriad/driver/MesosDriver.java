package org.apache.myriad.driver;

import org.apache.myriad.driver.model.MesosV1;

import java.util.List;

public interface MesosDriver {

    MesosV1.Status start();
    MesosV1.Status stop(boolean failover);
    MesosV1.Status abort();
    MesosV1.Status join();
    MesosV1.Status run();

    MesosV1.Status requestResources(List<MesosV1.Request> requests);

    // TODO(nnielsen): launchTasks using single offer is deprecated.
    // Use launchTasks with offer list instead.
    MesosV1.Status launchTasks(final MesosV1.OfferID offerId, final List<MesosV1.TaskInfo> tasks, final MesosV1.Filters filters);

    MesosV1.Status launchTasks(final List<MesosV1.OfferID> offerIds, final List<MesosV1.TaskInfo> tasks, final MesosV1.Filters filters);

    MesosV1.Status killTask(final MesosV1.TaskID taskId, final MesosV1.AgentID agent_id, final MesosV1.KillPolicy kill_policy);

    MesosV1.Status acceptOffers(final List<MesosV1.OfferID> offerIds, final List<MesosV1.Offer.Operation> operations, final MesosV1.Filters filters);

    MesosV1.Status declineOffer(final List<MesosV1.OfferID> offerId, final MesosV1.Filters filters);

    MesosV1.Status reviveOffers();

    MesosV1.Status suppressOffers();

    MesosV1.Status acknowledgeStatusUpdate(final MesosV1.TaskStatus status);

    MesosV1.Status sendFrameworkMessage(final MesosV1.ExecutorID executorId, final MesosV1.AgentID agentId, final byte[] data);

    MesosV1.Status reconcileTasks(final List<MesosV1.TaskStatus> statuses);

    MesosV1.Status shutdown(final MesosV1.ExecutorID executorId, final MesosV1.AgentID agentId);

    MesosV1.Status acknowledgeOperationStatus(final MesosV1.AgentID agentId, final MesosV1.ResourceProviderID resourceProviderId, final MesosV1.OperationStatus operationStaus);
}
