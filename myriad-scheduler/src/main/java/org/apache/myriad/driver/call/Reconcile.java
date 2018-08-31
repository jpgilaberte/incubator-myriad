package org.apache.myriad.driver.call;

import org.apache.myriad.driver.MesosDriverHttpApi;
import org.apache.myriad.driver.model.MesosV1;
import org.apache.myriad.driver.model.SchedulerV1;

import java.util.ArrayList;
import java.util.List;

public class Reconcile extends PostAndAcceptResponse{

    private final List<MesosV1.TaskStatus> statuses;

    public Reconcile(MesosDriverHttpApi driver, List<MesosV1.TaskStatus> statuses) {
        super(driver);
        this.statuses = statuses;
    }

    @Override
    protected SchedulerV1.Call createCall() {
        List<SchedulerV1.Call.Reconcile.Task> listTasks = new ArrayList<>();
        for (MesosV1.TaskStatus taskStatus : statuses) {
            SchedulerV1.Call.Reconcile.Task t = new SchedulerV1.Call.Reconcile.Task();
            t.setTask_id(taskStatus.getTask_id());
            t.setAgent_id(taskStatus.getAgent_id());
            listTasks.add(t);
        }
        SchedulerV1.Call call = new SchedulerV1.Call();
        SchedulerV1.Call.Reconcile reconcile = new SchedulerV1.Call.Reconcile();
        reconcile.setTasks(listTasks);
        call.setType(SchedulerV1.Call.Type.RECONCILE);
        call.setReconcile(reconcile);
        call.setFramework_id(driver.getFrameworkId());
        return call;
    }
}
