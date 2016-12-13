package jgisson.kie.tests.bpmn;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jgisson.kie.tests.event.MyProcessEventListener;

public class EmbeddedSubProcessTest extends AbstractProcessTest {

    private static final String PROCESS_FILE = "jgisson/kie/tests/bpmn/embedded-sub-process.bpmn";
    private static final String PROCESS_NAME = "embedded-sub-process";

    private static final String TASK_USER_1 = "Task User 1";
    private static final String TASK_MAIN_END = "Main end";
    private static final String TASK_TIMER_END = "Timer end";

    private static final String USER_ID = "john";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EmbeddedSubProcessTest() {
        super(true, true);
    }

    @Test
    public void embeddedSubProcessMainEnd() {
        createRuntimeManager(PROCESS_FILE);
        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new MyProcessEventListener());

        // Get TaskService
        TaskService taskService = engine.getTaskService();

        // Start process
        ProcessInstance pi = startProcess(ksession);

        assertNodeActive(pi.getId(), ksession, TASK_USER_1);

        // check user task
        List<Long> taskIds = taskService.getTasksByProcessInstanceId(pi.getId());
        Task task = taskService.getTaskById(taskIds.get(0));
        logger.info("Task informations: taskId={}; taskStatus={}; taskOwner={}", task.getId(), task.getTaskData().getStatus(),
                task.getTaskData().getActualOwner());
        logger.info("Task assignment informations: taskBusiness={}; taskPotentielOwner={}; ", task.getPeopleAssignments().getBusinessAdministrators(),
                task.getPeopleAssignments().getPotentialOwners());

        // User Task 1
        TaskSummary userTask = taskService.getTasksAssignedAsPotentialOwner("john", null).get(0);
        assertEquals(userTask.getName(), TASK_USER_1);
        taskService.start(userTask.getId(), USER_ID);
        taskService.complete(userTask.getId(), USER_ID, null);
        
        assertNodeTriggered(pi.getId(), TASK_MAIN_END);

        assertProcessInstanceCompleted(pi.getId());
    }
    
    @Test
    public void embeddedSubProcessTimerEnd() {
        createRuntimeManager(PROCESS_FILE);
        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new MyProcessEventListener());

        // Start process
        ProcessInstance pi = startProcess(ksession);
        assertNodeActive(pi.getId(), ksession, TASK_USER_1);

        // Wait timer event
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // Blank
        }
        
        assertNodeTriggered(pi.getId(), TASK_TIMER_END);

        assertProcessInstanceCompleted(pi.getId());
    }

    private ProcessInstance startProcess(KieSession ksession) {
        Map<String, Object> params = new HashMap<String, Object>();

        ProcessInstance processInstance = ksession.startProcess(PROCESS_NAME, params);

        return processInstance;
    }

}
