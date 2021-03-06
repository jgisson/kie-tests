package jgisson.kie.tests.bpmn;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.services.task.exception.PermissionDeniedException;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;

import jgisson.kie.tests.commands.MyForwardTaskCommand;
import jgisson.kie.tests.event.MyProcessEventListener;

/**
 * Tests User Task assigment
 * 
 * @see usergroups.properties in test resources file for users and groups list
 * 
 * @author jgisson
 *
 */
public class UserTaskAssignmentTest extends AbstractProcessTest {

    private static final String PROCESS_FILE = "jgisson/kie/tests/bpmn/user-task-assignment.bpmn";
    private static final String PROCESS_ID = "jgisson.kie.tests.user-task-assignment";

    private static final String USER_TASK_1 = "User Task 1";
    private static final String USER_TASK_2 = "User Task 2";

    // User in group GROUP1, GROUP2
    private static final String USER_ID = "john";
    // User in group GROUP1
    private static final String FORWARD_USER_ID = "mary";
    // User in group GROUP3
    private static final String USER_BOB_ID = "bob";

    private static final String FORWARD_GROUP = "GROUP3";

    private static final List<Status> TASK_STATUS_WORKING =
            Collections.unmodifiableList(Arrays.asList(Status.Created, Status.Ready, Status.Reserved, Status.InProgress));

    public UserTaskAssignmentTest() {
        super(true, true);
    }

    @Test
    public void defaultUserTaskAssignment() {
        System.out.println("Start User Task default assignment test ...");

        createRuntimeManager(PROCESS_FILE);
        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new MyProcessEventListener());

        // Get TaskService
        TaskService taskService = engine.getTaskService();

        // Start process
        ProcessInstance pi = startProcess(ksession);
        assertNodeActive(pi.getId(), ksession, USER_TASK_1);

        // Check and complete User Task 1
        List<TaskSummary> actualTasks = taskService.getTasksByStatusByProcessInstanceId(pi.getId(), TASK_STATUS_WORKING, null);
        Task task = taskService.getTaskById(actualTasks.get(0).getId());
        displayUserTaskInfos(task);
        assertEquals(task.getName(), USER_TASK_1);
        taskService.start(task.getId(), USER_ID);
        task = taskService.getTaskById(task.getId());
        displayUserTaskInfos(task);
        taskService.complete(task.getId(), USER_ID, null);

        // Check and complete User Task 2
        actualTasks = taskService.getTasksByStatusByProcessInstanceId(pi.getId(), TASK_STATUS_WORKING, null);
        task = taskService.getTaskById(actualTasks.get(0).getId());
        displayUserTaskInfos(task);
        assertEquals(task.getName(), USER_TASK_2);
        taskService.start(task.getId(), USER_ID);
        task = taskService.getTaskById(task.getId());
        displayUserTaskInfos(task);
        taskService.complete(task.getId(), USER_ID, null);

        assertProcessInstanceCompleted(pi.getId());

        System.out.println("User Task default assignment test done.");
    }

    @Test
    public void forwardUserTaskAssignment() {
        System.out.println("Start User Task forward assignment test ...");

        createRuntimeManager(PROCESS_FILE);
        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new MyProcessEventListener());

        // Get TaskService
        TaskService taskService = engine.getTaskService();

        // Start process
        ProcessInstance pi = startProcess(ksession);
        assertNodeActive(pi.getId(), ksession, USER_TASK_1);

        // Forward "User Task 1" to user
        List<TaskSummary> actualTasks = taskService.getTasksByStatusByProcessInstanceId(pi.getId(), TASK_STATUS_WORKING, null);
        Task task = taskService.getTaskById(actualTasks.get(0).getId());
        displayUserTaskInfos(task);
        assertEquals(task.getName(), USER_TASK_1);
        taskService.forward(task.getId(), USER_ID, FORWARD_USER_ID);
        task = taskService.getTaskById(task.getId());
        displayUserTaskInfos(task);
        Assert.assertEquals(FORWARD_USER_ID, task.getPeopleAssignments().getPotentialOwners().get(0).getId());
        // complete User Task 1
        taskService.start(task.getId(), FORWARD_USER_ID);
        taskService.complete(task.getId(), FORWARD_USER_ID, null);


        // Forward "User Task 2" to user or group
        try {
            actualTasks = taskService.getTasksByStatusByProcessInstanceId(pi.getId(), TASK_STATUS_WORKING, null);
            task = taskService.getTaskById(actualTasks.get(0).getId());
            displayUserTaskInfos(task);
            assertEquals(task.getName(), USER_TASK_2);
            taskService.forward(task.getId(), USER_ID, FORWARD_USER_ID);
            task = taskService.getTaskById(task.getId());
            displayUserTaskInfos(task);
        } catch (PermissionDeniedException pde) {
            // Forward failed because jBPM does not support forward when task is assigned to a group
            // Check OASIS WS-Human Task specification:
            // - Potential owners can only forward tasks that are in the Ready state.
            // - Forwarding is possible if the task has a set of individually assigned potential owners, not if its potential owners are assigned using one or
            // many groups
            System.out.println("Forward failed because jBPM does not support forward when task assigned to a group");
        }

        // Just complete User Task 2
        taskService.start(task.getId(), USER_ID);
        taskService.complete(task.getId(), USER_ID, null);

        assertProcessInstanceCompleted(pi.getId());

        System.out.println("User Task forward assignment test done.");
    }

    @Test
    public void myForwardUserTaskAssignment() {
        System.out.println("Start User Task users and groups management for User Task test ...");

        createRuntimeManager(PROCESS_FILE);
        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new MyProcessEventListener());

        // Get TaskService
        TaskService taskService = engine.getTaskService();

        // Start process
        ProcessInstance pi = startProcess(ksession);
        assertNodeActive(pi.getId(), ksession, USER_TASK_1);

        // Assign "User Task 1" to "GROUP2" and "GROUP3" instead of user "john"
        List<TaskSummary> actualTasks = taskService.getTasksByStatusByProcessInstanceId(pi.getId(), TASK_STATUS_WORKING, null);
        Task task = taskService.getTaskById(actualTasks.get(0).getId());
        displayUserTaskInfos(task);
        assertEquals(task.getName(), USER_TASK_1);
        List<String> groups = Arrays.asList("GROUP2", "GROUP3");
        taskService.execute(new MyForwardTaskCommand(task.getId(), groups));
        task = taskService.getTaskById(task.getId());
        displayUserTaskInfos(task);
        // Start and complete "User Task 1" with user in group GROUP3
        taskService.start(task.getId(), USER_BOB_ID);
        taskService.complete(task.getId(), USER_BOB_ID, null);

        // Assign "User Task 1" to "GROUP3" instead of group "GROUP1"
        // User in GROUP1 has a task
        List<TaskSummary> potentialsTasks = taskService.getTasksAssignedAsPotentialOwner(USER_ID, null);
        Assert.assertEquals(1, potentialsTasks.size());
        actualTasks = taskService.getTasksByStatusByProcessInstanceId(pi.getId(), TASK_STATUS_WORKING, null);
        task = taskService.getTaskById(actualTasks.get(0).getId());
        displayUserTaskInfos(task);
        assertEquals(task.getName(), USER_TASK_2);
        List<String> usertaskTwoGroups = Arrays.asList(FORWARD_GROUP);
        taskService.execute(new MyForwardTaskCommand(task.getId(), usertaskTwoGroups));
        task = taskService.getTaskById(task.getId());
        displayUserTaskInfos(task);
        // User in GROUP1 hasn't task
        potentialsTasks = taskService.getTasksAssignedAsPotentialOwner(USER_ID, null);
        Assert.assertEquals(0, potentialsTasks.size());
        // Just complete User Task 2 with user in group GROUP3
        taskService.start(task.getId(), USER_BOB_ID);
        taskService.complete(task.getId(), USER_BOB_ID, null);

        assertProcessInstanceCompleted(pi.getId());

        System.out.println("Manage users and groups for User Task test done.");
    }

    private ProcessInstance startProcess(KieSession ksession) {
        Map<String, Object> params = new HashMap<String, Object>();

        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID, params);

        return processInstance;
    }

}
