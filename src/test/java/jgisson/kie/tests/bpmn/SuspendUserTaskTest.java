package jgisson.kie.tests.bpmn;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.services.task.exception.PermissionDeniedException;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;

import jgisson.kie.tests.event.MyProcessEventListener;

/**
 * Tests suspend User Task
 * 
 * @see usergroups.properties in test resources file for users and groups list
 * 
 * @author jgisson
 *
 */
public class SuspendUserTaskTest extends AbstractProcessTest {

    private static final String PROCESS_FILE = "jgisson/kie/tests/bpmn/user-task-assignment.bpmn";
    private static final String PROCESS_ID = "jgisson.kie.tests.user-task-assignment";

    private static final String USER_TASK_1 = "User Task 1";
    private static final String USER_TASK_2 = "User Task 2";

    // User in group GROUP1, GROUP2
    private static final String USER_ID = "john";

    private static final List<Status> TASK_STATUS_WORKING =
            Collections.unmodifiableList(Arrays.asList(Status.Created, Status.Ready, Status.Reserved, Status.InProgress));

    private static final List<Status> TASK_STATUS_ASSIGNED =
            Collections.unmodifiableList(Arrays.asList(Status.Ready, Status.Reserved, Status.InProgress, Status.Suspended));

    public SuspendUserTaskTest() {
        super(true, true);
    }

    @Test
    public void suspendUserTask() {
        System.out.println("Start suspend User Task test ...");

        createRuntimeManager(PROCESS_FILE);
        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new MyProcessEventListener());

        // Get TaskService
        TaskService taskService = engine.getTaskService();

        // Start process
        ProcessInstance pi = startProcess(ksession);
        assertNodeActive(pi.getId(), ksession, USER_TASK_1);

        // Check and suspend User Task 1
        List<TaskSummary> actualTasks = taskService.getTasksByStatusByProcessInstanceId(pi.getId(), TASK_STATUS_WORKING, null);
        Task task = taskService.getTaskById(actualTasks.get(0).getId());
        displayUserTaskInfos(task);
        assertEquals(task.getName(), USER_TASK_1);
        taskService.suspend(task.getId(), USER_ID);
        task = taskService.getTaskById(task.getId());
        assertEquals(task.getTaskData().getStatus(), Status.Suspended);
        displayUserTaskInfos(task);
        List<TaskSummary> userTasks = taskService.getTasksOwnedByStatus(USER_ID, TASK_STATUS_ASSIGNED, null);
        assertTrue(userTasks.size() > 0);

        // Check ops on User Task 1
        boolean permissionDenied = false;
        try {
            taskService.complete(task.getId(), USER_ID, null);
        } catch (PermissionDeniedException e) {
            permissionDenied = true;
        }
        assertTrue(permissionDenied);
        permissionDenied = false;
        try {
            taskService.release(task.getId(), USER_ID);
        } catch (PermissionDeniedException e) {
            permissionDenied = true;
        }
        assertTrue(permissionDenied);

        // resume and complete User Task 1
        taskService.resume(task.getId(), USER_ID);
        taskService.start(task.getId(), USER_ID);
        taskService.complete(task.getId(), USER_ID, null);

        // Check and complete User Task 2
        actualTasks = taskService.getTasksByStatusByProcessInstanceId(pi.getId(), TASK_STATUS_WORKING, null);
        task = taskService.getTaskById(actualTasks.get(0).getId());
        assertEquals(task.getName(), USER_TASK_2);
        taskService.start(task.getId(), USER_ID);
        taskService.complete(task.getId(), USER_ID, null);

        assertProcessInstanceCompleted(pi.getId());

        System.out.println("User Task default assignment test done.");
    }

    private ProcessInstance startProcess(KieSession ksession) {
        Map<String, Object> params = new HashMap<String, Object>();

        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID, params);

        return processInstance;
    }

}
