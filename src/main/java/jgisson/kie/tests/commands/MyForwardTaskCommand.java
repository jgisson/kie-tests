package jgisson.kie.tests.commands;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.services.task.commands.TaskContext;
import org.jbpm.services.task.commands.UserGroupCallbackTaskCommand;
import org.jbpm.services.task.impl.model.GroupImpl;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.internal.command.Context;
import org.kie.internal.task.api.TaskPersistenceContext;
import org.kie.internal.task.api.model.InternalPeopleAssignments;
import org.kie.internal.task.api.model.InternalTaskData;

public class MyForwardTaskCommand extends UserGroupCallbackTaskCommand<Void> {

    /**
     * Auto generate UID
     */
    private static final long serialVersionUID = -2324740436917602666L;

    public MyForwardTaskCommand(long taskId, List<String> groupIds) {
        this.taskId = taskId;
        this.groupIds = groupIds;
    }

    @Override
    public Void execute(Context context) {
        TaskContext taskContext = (TaskContext) context;
        TaskPersistenceContext persistenceContext = taskContext.getPersistenceContext();

        Task task = persistenceContext.findTask(taskId);
        InternalTaskData taskData = (InternalTaskData) task.getTaskData();

        // Move to ready status
        taskData.setActualOwner(null);
        taskData.setStatus(Status.Ready);

        // Reassign task
        task.getPeopleAssignments().getPotentialOwners().clear();
        List<OrganizationalEntity> potentialOwners = new ArrayList<OrganizationalEntity>(groupIds.size());
        for (String groupId : groupIds) {
            potentialOwners.add(new GroupImpl(groupId));
        }
        task.getPeopleAssignments().getPotentialOwners().addAll(potentialOwners);

        doCallbackOperationForPeopleAssignments((InternalPeopleAssignments) task.getPeopleAssignments(), taskContext);

        return null;
    }

}
