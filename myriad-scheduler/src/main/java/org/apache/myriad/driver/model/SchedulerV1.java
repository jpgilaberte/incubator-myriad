package org.apache.myriad.driver.model;

import java.util.List;

public class SchedulerV1 {
    // Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless  by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
    
    /**
     * Scheduler event API.
     *
     * An event is described using the standard protocol buffer "union"
     * trick, see:
     * https://developers.google.com/protocol-buffers/docs/techniques#union.
     */
    public static class Event {
        // Possible event types, followed by public static class definitions if
        // applicable.
        public enum Type {
            // This must be the first public enum value in this list, to
            // ensure that if 'type' is not set, the default value
            // is UNKNOWN. This enables public enum values to be added
            // in a backwards-compatible way. See: MESOS-4997.
            UNKNOWN ,

            SUBSCRIBED ,               // See 'Subscribed' below.
            OFFERS,                   // See 'MesosV1.Offers' below.
            INVERSE_OFFERS ,           // See 'InverseMesosV1.Offers' below.
            RESCIND ,                  // See 'Rescind' below.
            RESCIND_INVERSE_OFFERS ,   // See 'RescindInverseMesosV1.Offer' below.
            UPDATE ,                   // See 'Update' below.
            UPDATE_OPERATION_STATUS, // See 'UpdateMesosV1.OperationStatus' below.
            MESSAGE,                  // See 'public static class' below.
            FAILURE,                  // See 'Failure' below.
            ERROR ,                    // See 'Error' below.

            // Periodic public static class sent by the Mesos master according to
            // 'Subscribed.heartbeat_interval_seconds'. If the scheduler does
            // not receive any events (including heartbeats) for an extended
            // period of time (e.g., 5 x heartbeat_interval_seconds), there is
            // likely a network partition. In such a case the scheduler should
            // close the existing subscription connection and resubscribe
            // using a backoff strategy.
            HEARTBEAT
        }

        // First event received when the scheduler subscribes.
        public static class Subscribed {
             MesosV1.FrameworkID framework_id ;

            // This value will be set if the master is sending heartbeats. See
            // the comment above on 'HEARTBEAT' for more details.
             double heartbeat_interval_seconds ;

            // Since Mesos 1.1.
             MesosV1.MasterInfo master_info ;

            public MesosV1.FrameworkID getFramework_id() {
                return framework_id;
            }

            public void setFramework_id(MesosV1.FrameworkID framework_id) {
                this.framework_id = framework_id;
            }

            public double getHeartbeat_interval_seconds() {
                return heartbeat_interval_seconds;
            }

            public void setHeartbeat_interval_seconds(double heartbeat_interval_seconds) {
                this.heartbeat_interval_seconds = heartbeat_interval_seconds;
            }

            public MesosV1.MasterInfo getMaster_info() {
                return master_info;
            }

            public void setMaster_info(MesosV1.MasterInfo master_info) {
                this.master_info = master_info;
            }
        }

        // Received whenever there are new resources that are MesosV1.Offered to the
        // scheduler. Each MesosV1.Offer corresponds to a set of resources on an
        // agent. Until the scheduler accepts or declines an MesosV1.Offer the
        // resources are considered allocated to the scheduler.
        public static class Offers {
            List<MesosV1.Offer> offers ;

            public List<MesosV1.Offer> getOffers() {
                return offers;
            }

            public void setOffers(List<MesosV1.Offer> offers) {
                this.offers = offers;
            }
        }

        // Received whenever there are resources requested back from the
        // scheduler. Each inverse MesosV1.Offer specifies the agent, and
        // ly specific resources. Accepting or Declining an inverse
        // MesosV1.Offer informs the allocator of the scheduler's ability to release
        // the specified resources without violating an SLA. If no resources
        // are specified then all resources on the agent are requested to be
        // released.
        public static class InverseOffers {
            List< MesosV1.InverseOffer> inverse_offers ;

            public List<MesosV1.InverseOffer> getInverse_offers() {
                return inverse_offers;
            }

            public void setInverse_offers(List<MesosV1.InverseOffer> inverse_offers) {
                this.inverse_offers = inverse_offers;
            }
        }

        // Received when a particular MesosV1.Offer is no longer valid (e.g., the
        // agent corresponding to the MesosV1.Offer has been removed) and hence
        // needs to be rescinded. Any future calls ('Accept' / 'Decline') made
        // by the scheduler regarding this MesosV1.Offer will be invalid.
        public static class Rescind {
             MesosV1.OfferID offer_id ;

            public MesosV1.OfferID getOffer_id() {
                return offer_id;
            }

            public void setOffer_id(MesosV1.OfferID offer_id) {
                this.offer_id = offer_id;
            }
        }

        // Received when a particular inverse MesosV1.Offer is no longer valid
        // (e.g., the agent corresponding to the MesosV1.Offer has been removed)
        // and hence needs to be rescinded. Any future calls ('Accept' /
        // 'Decline') made by the scheduler regarding this inverse MesosV1.Offer
        // will be invalid.
        public static class RescindInverseOffer {
             MesosV1.OfferID inverse_offer_id ;

            public MesosV1.OfferID getInverse_offer_id() {
                return inverse_offer_id;
            }

            public void setInverse_offer_id(MesosV1.OfferID inverse_offer_id) {
                this.inverse_offer_id = inverse_offer_id;
            }
        }

        // Received whenever there is a status update that is generated by
        // the executor or agent or master. Status updates should be used by
        // executors to reliably communicate the status of the tasks that
        // they manage. It is crucial that a terminal update (see TaskState
        // in v1/mesos.proto) is sent by the executor as soon as the task
        // terminates, in order for Mesos to release the resources allocated
        // to the task. It is also the responsibility of the scheduler to
        // explicitly acknowledge the receipt of a status update. See
        // 'Acknowledge' in the 'Call' section below for the semantics.
        //
        // A task status update may be used for guaranteed delivery of some
        // task-related information, e.g., task's health update. Such
        // information may be shadowed by subsequent task status updates, that
        // do not preserve fields of the previously sent public static class.
        public static class Update {
             MesosV1.TaskStatus status ;

            public MesosV1.TaskStatus getStatus() {
                return status;
            }

            public void setStatus(MesosV1.TaskStatus status) {
                this.status = status;
            }
        }

        // Received when there is an operation status update generated by the
        // master, agent, or resource provider. These updates are only sent to
        // the framework for operations which had the operation ID set by the
        // framework. It is the responsibility of the scheduler to explicitly
        // acknowledge the receipt of a status update.
        // See 'AcknowledgeMesosV1.OperationStatus' in the 'Call' section below for
        // the semantics.
        public static class UpdateOperationStatus {
             MesosV1.OperationStatus status ;

            public MesosV1.OperationStatus getStatus() {
                return status;
            }

            public void setStatus(MesosV1.OperationStatus status) {
                this.status = status;
            }
        }

        // Received when a custom public static class generated by the executor is
        // forwarded by the master. Note that this public static class is not
        // interpreted by Mesos and is only forwarded (without reliability
        // guarantees) to the scheduler. It is up to the executor to retry
        // if the public static class is dropped for any reason.
        public static class Message {
             MesosV1.AgentID agent_id ;
             MesosV1.ExecutorID executor_id ;
             byte[] data ;

            public MesosV1.AgentID getAgent_id() {
                return agent_id;
            }

            public void setAgent_id(MesosV1.AgentID agent_id) {
                this.agent_id = agent_id;
            }

            public MesosV1.ExecutorID getExecutor_id() {
                return executor_id;
            }

            public void setExecutor_id(MesosV1.ExecutorID executor_id) {
                this.executor_id = executor_id;
            }

            public byte[] getData() {
                return data;
            }

            public void setData(byte[] data) {
                this.data = data;
            }
        }

        // Received when an agent is removed from the cluster (e.g., failed
        // health checks) or when an executor is terminated. Note that, this
        // event coincides with receipt of terminal UPDATE events for any
        // active tasks belonging to the agent or executor and receipt of
        // 'Rescind' events for any outstanding MesosV1.Offers belonging to the
        // agent. Note that there is no guaranteed order between the
        // 'Failure', 'Update' and 'Rescind' events when an agent or executor
        // is removed.
        // TODO(vinod): Consider splitting the lost agent and terminated
        // executor into separate events and ensure it's reliably generated.
        public static class Failure {
             MesosV1.AgentID agent_id ;

            // If this was just a failure of an executor on an agent then
            // 'executor_id' will be set and possibly 'status' (if we were
            // able to determine the exit status).
             MesosV1.ExecutorID executor_id ;

            // On Posix, `status` corresponds to termination information in the
            // `stat_loc` area returned from a `waitpid` call. On Windows, `status`
            // is obtained via calling the `GetExitCodeProcess()` function. For
            // public static classs coming from Posix agents, schedulers need to apply
            // `WEXITSTATUS` family macros or equivalent transformations to obtain
            // exit codes.
            //
            // TODO(alexr): Consider unifying Windows and Posix behavior by returning
            // exit code here, see MESOS-7241.
             Integer status ;

            public MesosV1.AgentID getAgent_id() {
                return agent_id;
            }

            public void setAgent_id(MesosV1.AgentID agent_id) {
                this.agent_id = agent_id;
            }

            public MesosV1.ExecutorID getExecutor_id() {
                return executor_id;
            }

            public void setExecutor_id(MesosV1.ExecutorID executor_id) {
                this.executor_id = executor_id;
            }

            public Integer getStatus() {
                return status;
            }

            public void setStatus(Integer status) {
                this.status = status;
            }
        }

        // Received when there is an unrecoverable error in the scheduler (e.g.,
        // scheduler failed over, rate limiting, authorization errors etc.). The
        // scheduler should abort on receiving this event.
        public static class Error {
             String message ;

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }
        }

        // Type of the event, indicates which  field below should be
        // present if that type has a nested public static class definition.
        // public enum fields should be , see: MESOS-4997.
         Type type ;

         Subscribed subscribed ;
         Offers offers ;
         InverseOffers inverse_offers ;
         Rescind rescind ;
         RescindInverseOffer rescind_inverse_offer ;
         Update update ;
         UpdateOperationStatus update_operation_status ;
         Message message ;
         Failure failure ;
         Error error ;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public Subscribed getSubscribed() {
            return subscribed;
        }

        public void setSubscribed(Subscribed subscribed) {
            this.subscribed = subscribed;
        }

        public Offers getOffers() {
            return offers;
        }

        public void setOffers(Offers offers) {
            this.offers = offers;
        }

        public InverseOffers getInverse_offers() {
            return inverse_offers;
        }

        public void setInverse_offers(InverseOffers inverse_offers) {
            this.inverse_offers = inverse_offers;
        }

        public Rescind getRescind() {
            return rescind;
        }

        public void setRescind(Rescind rescind) {
            this.rescind = rescind;
        }

        public RescindInverseOffer getRescind_inverse_offer() {
            return rescind_inverse_offer;
        }

        public void setRescind_inverse_offer(RescindInverseOffer rescind_inverse_offer) {
            this.rescind_inverse_offer = rescind_inverse_offer;
        }

        public Update getUpdate() {
            return update;
        }

        public void setUpdate(Update update) {
            this.update = update;
        }

        public UpdateOperationStatus getUpdate_operation_status() {
            return update_operation_status;
        }

        public void setUpdate_operation_status(UpdateOperationStatus update_operation_status) {
            this.update_operation_status = update_operation_status;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public Failure getFailure() {
            return failure;
        }

        public void setFailure(Failure failure) {
            this.failure = failure;
        }

        public Error getError() {
            return error;
        }

        public void setError(Error error) {
            this.error = error;
        }
    }


    /**
     * Synchronous responses for calls made to the scheduler API.
     */
    public static class Response {
        // Each of the responses of type `FOO` corresponds to `Foo` public static class below.
        public enum Type {
            UNKNOWN ,

            RECONCILE_OPERATIONS // See 'ReconcileOperations' below.
        }

        public static class ReconcileOperations {
            List<MesosV1.OperationStatus> operation_statuses ;
        }

         ReconcileOperations reconcile_operations ;
    }


    /**
     * Scheduler call API.
     *
     * Like Event, a Call is described using the standard protocol buffer
     * "union" trick (see above).
     */
    public static class Call {
        // Possible call types, followed by public static class definitions if
        // applicable.
        public enum Type {
            // See comments above on `Event::Type` for more details on this public enum value.
            UNKNOWN ,

            SUBSCRIBE ,   // See 'Subscribe' below.
            TEARDOWN ,    // Shuts down all tasks/executors and removes framework.
            ACCEPT ,      // See 'Accept' below.
            DECLINE ,     // See 'Decline' below.
            ACCEPT_INVERSE_OFFERS ,  // See 'AcceptInverseMesosV1.Offers' below.
            DECLINE_INVERSE_OFFERS , // See 'DeclineInverseMesosV1.Offers' below.
            REVIVE ,      // Removes any previous MesosV1.Filters set via ACCEPT or DECLINE.
            KILL ,        // See 'Kill' below.
            SHUTDOWN ,    // See 'Shutdown' below.
            ACKNOWLEDGE , // See 'Acknowledge' below.
            ACKNOWLEDGE_OPERATION_STATUS , // See public static class below.
            RECONCILE ,   // See 'Reconcile' below.
            RECONCILE_OPERATIONS , // See 'ReconcileOperations' below.
            MESSAGE ,    // See 'public static class' below.
            REQUEST ,    // See 'Request' below.
            SUPPRESS   // Inform master to stop sending MesosV1.Offers to the framework.

            // TODO(benh): Consider adding an 'ACTIVATE' and 'DEACTIVATE' for
            // already subscribed frameworks as a way of stopping MesosV1.Offers from
            // being generated and other events from being sent by the master.
            // Note that this functionality existed originally to support
            // SchedulerDriver::abort which was only necessary to handle
            // exceptions getting thrown from within Scheduler callbacks,
            // something that is not an issue with the Event/Call API.
        }

        // Subscribes the scheduler with the master to receive events. A
        // scheduler must send other calls only after it has received the
        // SUBCRIBED event.
        public static class Subscribe {
            // See the comments below on 'framework_id' on the semantics for
            // 'framework_info.id'.
             MesosV1.FrameworkInfo framework_info ;

            // List of suppressed roles for which the framework does not wish to be
            // MesosV1.Offered resources. The framework can decide to suppress all or a subset
            // of roles the framework (re)registers as.
            List<String> suppressed_roles ;

            public MesosV1.FrameworkInfo getFramework_info() {
                return framework_info;
            }

            public void setFramework_info(MesosV1.FrameworkInfo framework_info) {
                this.framework_info = framework_info;
            }

            public List<String> getSuppressed_roles() {
                return suppressed_roles;
            }

            public void setSuppressed_roles(List<String> suppressed_roles) {
                this.suppressed_roles = suppressed_roles;
            }
        }

        // Accepts an MesosV1.Offer, performing the specified operations
        // in a sequential manner.
        //
        // E.g. Launch a task with a newly reserved persistent volume:
        //
        //   Accept {
        //     MesosV1.Offer_ids: [ ... ]
        //     operations: [
        //       { type: RESERVE,
        //         reserve: { resources: [ disk(role):2 ] } }
        //       { type: CREATE,
        //         create: { volumes: [ disk(role):1+persistence ] } }
        //       { type: LAUNCH,
        //         launch: { task_infos ... disk(role):1;disk(role):1+persistence } }
        //     ]
        //   }
        //
        // Note that any of the MesosV1.Offer’s resources not used in the 'Accept'
        // call (e.g., to launch a task) are considered unused and might be
        // reMesosV1.Offered to other frameworks. In other words, the same MesosV1.OfferID
        // cannot be used in more than one 'Accept' call.
        public static class Accept {
            List< MesosV1.OfferID> offer_ids ;
            List< MesosV1.Offer.Operation> operations ;
            MesosV1.Filters filters ;

            public List<MesosV1.OfferID> getOffer_ids() {
                return offer_ids;
            }

            public void setOffer_ids(List<MesosV1.OfferID> offer_ids) {
                this.offer_ids = offer_ids;
            }

            public List<MesosV1.Offer.Operation> getOperations() {
                return operations;
            }

            public void setOperations(List<MesosV1.Offer.Operation> operations) {
                this.operations = operations;
            }

            public MesosV1.Filters getFilters() {
                return filters;
            }

            public void setFilters(MesosV1.Filters filters) {
                this.filters = filters;
            }
        }

        // Declines an MesosV1.Offer, signaling the master to potentially reMesosV1.Offer
        // the resources to a different framework. Note that this is same
        // as sending an Accept call with no operations. See comments on
        // top of 'Accept' for semantics.
        public static class Decline {
            List<MesosV1.OfferID> offer_ids ;
             MesosV1.Filters filters ;

            public List<MesosV1.OfferID> getOffer_ids() {
                return offer_ids;
            }

            public void setOffer_ids(List<MesosV1.OfferID> offer_ids) {
                this.offer_ids = offer_ids;
            }

            public MesosV1.Filters getFilters() {
                return filters;
            }

            public void setFilters(MesosV1.Filters filters) {
                this.filters = filters;
            }
        }

        // Accepts an inverse MesosV1.Offer. Inverse MesosV1.Offers should only be accepted
        // if the resources in the MesosV1.Offer can be safely evacuated before the
        // provided unavailability.
        public static class AcceptInverseOffers {
            List< MesosV1.OfferID> inverse_offer_ids ;
             MesosV1.Filters filters ;
        }

        // Declines an inverse MesosV1.Offer. Inverse MesosV1.Offers should be declined if
        // the resources in the MesosV1.Offer might not be safely evacuated before
        // the provided unavailability.
        public static class DeclineInverseOffers {
            List< MesosV1.OfferID> inverse_offer_ids ;
             MesosV1.Filters filters ;
        }

        // Revive MesosV1.Offers for the specified roles. If `roles` is empty,
        // the `REVIVE` call will revive MesosV1.Offers for all of the roles
        // the framework is currently subscribed to.
        public static class Revive {
            List<String> roles ;

            public List<String> getRoles() {
                return roles;
            }

            public void setRoles(List<String> roles) {
                this.roles = roles;
            }
        }

        // Kills a specific task. If the scheduler has a custom executor,
        // the kill is forwarded to the executor and it is up to the
        // executor to kill the task and send a TASK_KILLED (or TASK_FAILED)
        // update. Note that Mesos releases the resources for a task once it
        // receives a terminal update (See TaskState in v1/mesos.proto) for
        // it. If the task is unknown to the master, a TASK_LOST update is
        // generated.
        //
        // If a task within a task group is killed before the group is
        // delivered to the executor, all tasks in the task group are
        // killed. When a task group has been delivered to the executor,
        // it is up to the executor to decide how to deal with the kill.
        // Note The default Mesos executor will currently kill all the
        // tasks in the task group if it gets a kill for any task.
        public static class Kill {
             MesosV1.TaskID task_id ;
             MesosV1.AgentID agent_id ;

            // If set, overrides any previously specified kill policy for this task.
            // This includes 'TaskInfo.kill_policy' and 'Executor.kill.kill_policy'.
            // Can be used to forcefully kill a task which is already being killed.
             MesosV1.KillPolicy kill_policy ;

            public MesosV1.TaskID getTask_id() {
                return task_id;
            }

            public void setTask_id(MesosV1.TaskID task_id) {
                this.task_id = task_id;
            }

            public MesosV1.AgentID getAgent_id() {
                return agent_id;
            }

            public void setAgent_id(MesosV1.AgentID agent_id) {
                this.agent_id = agent_id;
            }

            public MesosV1.KillPolicy getKill_policy() {
                return kill_policy;
            }

            public void setKill_policy(MesosV1.KillPolicy kill_policy) {
                this.kill_policy = kill_policy;
            }
        }

        // Shuts down a custom executor. When the executor gets a shutdown
        // event, it is expected to kill all its tasks (and send TASK_KILLED
        // updates) and terminate. If the executor doesn’t terminate within
        // a certain timeout (configurable via
        // '--executor_shutdown_grace_period' agent flag), the agent will
        // forcefully destroy the container (executor and its tasks) and
        // transition its active tasks to TASK_LOST.
        public static class Shutdown {
             MesosV1.ExecutorID executor_id ;
             MesosV1.AgentID agent_id ;

            public MesosV1.ExecutorID getExecutor_id() {
                return executor_id;
            }

            public void setExecutor_id(MesosV1.ExecutorID executor_id) {
                this.executor_id = executor_id;
            }

            public MesosV1.AgentID getAgent_id() {
                return agent_id;
            }

            public void setAgent_id(MesosV1.AgentID agent_id) {
                this.agent_id = agent_id;
            }
        }

        // Acknowledges the receipt of status update. Schedulers are
        // responsible for explicitly acknowledging the receipt of status
        // updates that have 'Update.status().uuid()' field set. Such status
        // updates are retried by the agent until they are acknowledged by
        // the scheduler.
        public static class Acknowledge {
             MesosV1.AgentID agent_id ;
             MesosV1.TaskID task_id ;
             byte[] uuid ;

            public MesosV1.AgentID getAgent_id() {
                return agent_id;
            }

            public void setAgent_id(MesosV1.AgentID agent_id) {
                this.agent_id = agent_id;
            }

            public MesosV1.TaskID getTask_id() {
                return task_id;
            }

            public void setTask_id(MesosV1.TaskID task_id) {
                this.task_id = task_id;
            }

            public byte[] getUuid() {
                return uuid;
            }

            public void setUuid(byte[] uuid) {
                this.uuid = uuid;
            }
        }

        // Acknowledges the receipt of an operation status update. Schedulers
        // are responsible for explicitly acknowledging the receipt of updates
        // which have the 'UpdateMesosV1.OperationStatus.status().uuid()' field set.
        // Such status updates are retried by the agent or resource provider
        // until they are acknowledged by the scheduler.
        public static class AcknowledgeOperationStatus {
             MesosV1.AgentID agent_id ;
             MesosV1.ResourceProviderID resource_provider_id ;
             byte[] uuid ;
             MesosV1.OperationID operation_id ;

            public MesosV1.AgentID getAgent_id() {
                return agent_id;
            }

            public void setAgent_id(MesosV1.AgentID agent_id) {
                this.agent_id = agent_id;
            }

            public MesosV1.ResourceProviderID getResource_provider_id() {
                return resource_provider_id;
            }

            public void setResource_provider_id(MesosV1.ResourceProviderID resource_provider_id) {
                this.resource_provider_id = resource_provider_id;
            }

            public byte[] getUuid() {
                return uuid;
            }

            public void setUuid(byte[] uuid) {
                this.uuid = uuid;
            }

            public MesosV1.OperationID getOperation_id() {
                return operation_id;
            }

            public void setOperation_id(MesosV1.OperationID operation_id) {
                this.operation_id = operation_id;
            }
        }

        // Allows the scheduler to query the status for non-terminal tasks.
        // This causes the master to send back the latest task status for
        // each task in 'tasks', if possible. Tasks that are no longer known
        // will result in a TASK_LOST, TASK_UNKNOWN, or TASK_UNREACHABLE update.
        // If 'tasks' is empty, then the master will send the latest status
        // for each task currently known.
        public static class Reconcile {
            // TODO(vinod): Support arbitrary queries than just state of tasks.
            public static class Task {
                 MesosV1.TaskID task_id ;
                 MesosV1.AgentID agent_id ;

                public MesosV1.TaskID getTask_id() {
                    return task_id;
                }

                public void setTask_id(MesosV1.TaskID task_id) {
                    this.task_id = task_id;
                }

                public MesosV1.AgentID getAgent_id() {
                    return agent_id;
                }

                public void setAgent_id(MesosV1.AgentID agent_id) {
                    this.agent_id = agent_id;
                }
            }

            List<Task> tasks ;

            public List<Task> getTasks() {
                return tasks;
            }

            public void setTasks(List<Task> tasks) {
                this.tasks = tasks;
            }
        }

        // Allows the scheduler to query the status of operations. This causes
        // the master to send back the latest status for each operation in
        // 'operations', if possible. If 'operations' is empty, then the
        // master will send the latest status for each operation currently
        // known.
        public static class ReconcileOperations {
            public static class Operation {
                 MesosV1.OperationID operation_id ;
                 MesosV1.AgentID agent_id ;
                 MesosV1.ResourceProviderID resource_provider_id ;
            }

            List<Operation> operations ;
        }

        // Sends arbitrary binary data to the executor. Note that Mesos
        // neither interprets this data nor makes any guarantees about the
        // delivery of this public static class to the executor.
        public static class Message {
             MesosV1.AgentID agent_id ;
             MesosV1.ExecutorID executor_id ;
             byte[] data ;

            public MesosV1.AgentID getAgent_id() {
                return agent_id;
            }

            public void setAgent_id(MesosV1.AgentID agent_id) {
                this.agent_id = agent_id;
            }

            public MesosV1.ExecutorID getExecutor_id() {
                return executor_id;
            }

            public void setExecutor_id(MesosV1.ExecutorID executor_id) {
                this.executor_id = executor_id;
            }

            public byte[] getData() {
                return data;
            }

            public void setData(byte[] data) {
                this.data = data;
            }
        }

        // Requests a specific set of resources from Mesos's allocator. If
        // the allocator has support for this, corresponding MesosV1.Offers will be
        // sent asynchronously via the MesosV1.OfferS event(s).
        //
        // NOTE: The built-in hierarchical allocator doesn't have support
        // for this call and hence simply ignores it.
        public static class Request {
            List<MesosV1.Request> requests ;

            public List<MesosV1.Request> getRequests() {
                return requests;
            }

            public void setRequests(List<MesosV1.Request> requests) {
                this.requests = requests;
            }
        }

        // Suppress MesosV1.Offers for the specified roles. If `roles` is empty,
        // the `SUPPRESS` call will suppress MesosV1.Offers for all of the roles
        // the framework is currently subscribed to.
        public static class Suppress {
            List<String> roles ;

            public List<String> getRoles() {
                return roles;
            }

            public void setRoles(List<String> roles) {
                this.roles = roles;
            }
        }

        // Identifies who generated this call. Master assigns a framework id
        // when a new scheduler subscribes for the first time. Once assigned,
        // the scheduler must set the 'framework_id' here and within its
        // MesosV1.FrameworkInfo (in any further 'Subscribe' calls). This allows the
        // master to identify a scheduler correctly across disconnections,
        // failovers, etc.
         MesosV1.FrameworkID framework_id ;

        // Type of the call, indicates which  field below should be
        // present if that type has a nested public static class definition.
        // See comments on `Event::Type` above on the reasoning behind this
        // field being .
         Type type ;

         Subscribe subscribe ;
         Accept accept ;
         Decline decline ;
         AcceptInverseOffers accept_inverse_offers ;
         DeclineInverseOffers decline_inverse_offers ;
         Revive revive ;
         Kill kill ;
         Shutdown shutdown ;
         Acknowledge acknowledge ;
         AcknowledgeOperationStatus acknowledge_operation_status ;
         Reconcile reconcile ;
         ReconcileOperations reconcile_operations ;
         Message message ;
         Request request ;
         Suppress suppress ;

        public MesosV1.FrameworkID getFramework_id() {
            return framework_id;
        }

        public void setFramework_id(MesosV1.FrameworkID framework_id) {
            this.framework_id = framework_id;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public Subscribe getSubscribe() {
            return subscribe;
        }

        public void setSubscribe(Subscribe subscribe) {
            this.subscribe = subscribe;
        }

        public Accept getAccept() {
            return accept;
        }

        public void setAccept(Accept accept) {
            this.accept = accept;
        }

        public Decline getDecline() {
            return decline;
        }

        public void setDecline(Decline decline) {
            this.decline = decline;
        }

        public AcceptInverseOffers getAccept_inverse_offers() {
            return accept_inverse_offers;
        }

        public void setAccept_inverse_offers(AcceptInverseOffers accept_inverse_offers) {
            this.accept_inverse_offers = accept_inverse_offers;
        }

        public DeclineInverseOffers getDecline_inverse_offers() {
            return decline_inverse_offers;
        }

        public void setDecline_inverse_offers(DeclineInverseOffers decline_inverse_offers) {
            this.decline_inverse_offers = decline_inverse_offers;
        }

        public Revive getRevive() {
            return revive;
        }

        public void setRevive(Revive revive) {
            this.revive = revive;
        }

        public Kill getKill() {
            return kill;
        }

        public void setKill(Kill kill) {
            this.kill = kill;
        }

        public Shutdown getShutdown() {
            return shutdown;
        }

        public void setShutdown(Shutdown shutdown) {
            this.shutdown = shutdown;
        }

        public Acknowledge getAcknowledge() {
            return acknowledge;
        }

        public void setAcknowledge(Acknowledge acknowledge) {
            this.acknowledge = acknowledge;
        }

        public AcknowledgeOperationStatus getAcknowledge_operation_status() {
            return acknowledge_operation_status;
        }

        public void setAcknowledge_operation_status(AcknowledgeOperationStatus acknowledge_operation_status) {
            this.acknowledge_operation_status = acknowledge_operation_status;
        }

        public Reconcile getReconcile() {
            return reconcile;
        }

        public void setReconcile(Reconcile reconcile) {
            this.reconcile = reconcile;
        }

        public ReconcileOperations getReconcile_operations() {
            return reconcile_operations;
        }

        public void setReconcile_operations(ReconcileOperations reconcile_operations) {
            this.reconcile_operations = reconcile_operations;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public Suppress getSuppress() {
            return suppress;
        }

        public void setSuppress(Suppress suppress) {
            this.suppress = suppress;
        }
    }

}
