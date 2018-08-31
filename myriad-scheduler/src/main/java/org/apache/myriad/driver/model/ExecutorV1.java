package org.apache.myriad.driver.model;

import java.util.List;

public class ExecutorV1 {
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
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


    /**
     * Executor event API.
     *
     * An event is described using the standard protocol buffer "union"
     * trick, see https://developers.google.com/protocol-buffers/docs/techniques#union.
     */
    public static class Event {
        // Possible event types, followed by message definitions if
        // applicable.
        public enum Type {
            // This must be the first enum value in this list, to
            // ensure that if 'type' is not set, the default value
            // is UNKNOWN. This enables enum values to be added
            // in a backwards-compatible way. See: MESOS-4997.
            UNKNOWN,
            SUBSCRIBED,   // See 'Subscribed' below.
            LAUNCH,       // See 'Launch' below.
            LAUNCH_GROUP, // See 'LaunchGroup' below.
            KILL,      // See 'Kill' below.
            ACKNOWLEDGED, // See 'Acknowledged' below.
            MESSAGE,      // See 'Message' below.
            ERROR,        // See 'Error' below.

            // Received when the agent asks the executor to shutdown/kill itself.
            // The executor is then required to kill all its active tasks, send
            // `TASK_KILLED` status updates and gracefully exit. The executor
            // should terminate within a `MESOS_EXECUTOR_SHUTDOWN_GRACE_PERIOD`
            // (an environment variable set by the agent upon executor startup);
            // it can be configured via `ExecutorInfo.shutdown_grace_period`. If
            // the executor fails to do so, the agent will forcefully destroy the
            // container where the executor is running. The agent would then send
            // `TASK_LOST` updates for any remaining active tasks of this executor.
            //
            // NOTE: The executor must not assume that it will always be allotted
            // the full grace period, as the agent may decide to allot a shorter
            // period and failures / forcible terminations may occur.
            //
            // TODO(alexr): Consider adding a duration field into the `Shutdown`
            // message so that the agent can communicate when a shorter period
            // has been allotted.
            SHUTDOWN
        }

        // First event received when the executor subscribes.
        // The 'id' field in the 'framework_info' will be set.
        public static class Subscribed {
            MesosV1.ExecutorInfo executor_info;
            MesosV1.FrameworkInfo framework_info ;
            MesosV1.AgentInfo agent_info;

            // Uniquely identifies the container of an executor run.
            MesosV1.ContainerID container_id;
        }

        // Received when the framework attempts to launch a task. Once
        // the task is successfully launched, the executor must respond with
        // a TASK_RUNNING update (See TaskState in v1/mesos.proto).
        public static class Launch {
            MesosV1.TaskInfo task;
        }

        // Received when the framework attempts to launch a group of tasks atomically.
        // Similar to `Launch` above the executor must send TASK_RUNNING updates for
        // tasks that are successfully launched.
        public static class LaunchGroup {
            MesosV1.TaskGroupInfo task_group;
        }

        // Received when the scheduler wants to kill a specific task. Once
        // the task is terminated, the executor should send a TASK_KILLED
        // (or TASK_FAILED) update. The terminal update is necessary so
        // Mesos can release the resources associated with the task.
        public static class Kill {
            MesosV1.TaskID task_id;

            // If set, overrides any previously specified kill policy for this task.
            // This includes 'TaskInfo.kill_policy' and 'Executor.kill.kill_policy'.
            // Can be used to forcefully kill a task which is already being killed.
            MesosV1.KillPolicy kill_policy;
        }

        // Received when the agent acknowledges the receipt of status
        // update. Schedulers are responsible for explicitly acknowledging
        // the receipt of status updates that have 'update.status().uuid()'
        // field set. Unacknowledged updates can be retried by the executor.
        // They should also be sent by the executor whenever it
        // re-subscribes.
        public static class Acknowledged {
            MesosV1.TaskID task_id;
            byte[] uuid;
        }

        // Received when a custom message generated by the scheduler is
        // forwarded by the agent. Note that this message is not
        // interpreted by Mesos and is only forwarded (without reliability
        // guarantees) to the executor. It is up to the scheduler to retry
        // if the message is dropped for any reason.
        public static class Message {
            byte[] data;
        }

        // Received in case the executor sends invalid calls (e.g.,
        // required values not set).
        // TODO(arojas): Remove this once the old executor driver is no
        // longer supported. With HTTP API all errors will be signaled via
        // HTTP response codes.
        public static class Error {
            String message;
        }

        // Type of the event, indicates which optional field below should be
        // present if that type has a nested message definition.
        // Enum fields should be optional, see: MESOS-4997.
        Type type;

        Subscribed subscribed;
        Acknowledged acknowledged;
        Launch launch;
        LaunchGroup launch_group ;
        Kill kill;
        Message message ;
        Error error ;
    }


    /**
     * Executor call API.
     *
     * Like Event, a Call is described using the standard protocol buffer
     * "union" trick (see above).
     */
    public static class Call {
        // Possible call types, followed by message definitions if
        // applicable.
        public enum Type {
            // See comments above on `Event::Type` for more details on this enum value.
            UNKNOWN,
            SUBSCRIBE,    // See 'Subscribe' below.
            UPDATE,       // See 'Update' below.
            MESSAGE      // See 'Message' below.
        }

        // Request to subscribe with the agent. If subscribing after a disconnection,
        // it must include a list of all the tasks and updates which haven't been
        // acknowledged by the scheduler.
        public static class Subscribe {
            List<MesosV1.TaskInfo> unacknowledged_tasks;
            List<Update> unacknowledged_updates;
        }

        // Notifies the scheduler that a task has transitioned from one
        // state to another. Status updates should be used by executors
        // to reliably communicate the status of the tasks that they
        // manage. It is crucial that a terminal update (see TaskState
        // in v1/mesos.proto) is sent to the scheduler as soon as the task
        // terminates, in order for Mesos to release the resources allocated
        // to the task. It is the responsibility of the scheduler to
        // explicitly acknowledge the receipt of a status update. See
        // 'Acknowledged' in the 'Events' section above for the semantics.
        public static class Update {
            MesosV1.TaskStatus status;
        }

        // Sends arbitrary binary data to the scheduler. Note that Mesos
        // neither interprets this data nor makes any guarantees about the
        // delivery of this message to the scheduler.
        // See 'Message' in the 'Events' section.
        public static class Message {
            byte[] data;
        }

        // Identifies the executor which generated this call.
        MesosV1.ExecutorID executor_id;
        MesosV1.FrameworkID framework_id;

        // Type of the call, indicates which optional field below should be
        // present if that type has a nested message definition.
        // In case type is SUBSCRIBED, no message needs to be set.
        // See comments on `Event::Type` above on the reasoning behind this
        // field being optional.
        Type type;

        Subscribe subscribe;
        Update update;
        Message message;
    }

}
