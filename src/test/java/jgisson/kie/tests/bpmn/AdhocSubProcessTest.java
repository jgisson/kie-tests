package jgisson.kie.tests.bpmn;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.process.instance.event.listeners.RuleAwareProcessEventLister;
import org.junit.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;

public class AdhocSubProcessTest extends AbstractProcessTest {

    private static final String PROCESS_FILE = "jgisson/kie/tests/bpmn/adhoc-sub-process.bpmn";
    private static final String PROCESS_ADHOC_FILE = "jgisson/kie/tests/bpmn/adhoc-sub-process.drl";
    private static final String PROCESS_NAME = "adhoc-sub-process";

    private static final String TASK_USER_1 = "Task User 1";
    private static final String TASK_USER_3 = "Task User 3";
    private static final String TASK_AFTER_ADHOC = "After";

    private static final String USER_ID = "john";

    public AdhocSubProcessTest() {
        super(true, true);
    }

    @Test
    public void adhocSubProcessMainEnd() {
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>(2);
        resources.put(PROCESS_FILE, ResourceType.BPMN2);
        resources.put(PROCESS_ADHOC_FILE, ResourceType.DRL);
        createRuntimeManager(resources);
        RuntimeEngine engine = getRuntimeEngine();
        final KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new RuleAwareProcessEventLister());
        ksession.addEventListener(new DefaultAgendaEventListener() {
            @Override
            public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
                System.out.println("Drools Before RuleFlowGroupActivated " + event.getRuleFlowGroup().getName());
            }

            @Override
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
                System.out.println("Drools After RuleFlowGroupActivated " + event.getRuleFlowGroup().getName());
                ksession.fireAllRules();
            }

            @Override
            public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
                System.out.println("Drools Before RuleFlowGroupDeactivated " + event.getRuleFlowGroup().getName());
            }

            @Override
            public void matchCreated(MatchCreatedEvent event) {
                System.out.println("Drools Rule Match Created Event " + event.getMatch().getRule().getName());
                ksession.fireAllRules();
            }

            @Override
            public void matchCancelled(MatchCancelledEvent event) {
                System.out.println("Drools Rule Cancelled Event " + event.getMatch().getRule().getName());
            }

            @Override
            public void afterMatchFired(AfterMatchFiredEvent event) {
                System.out.println("Drools Rule Fired Event " + event.getMatch().getRule().getName());
            }


        });

        // Get TaskService
        TaskService taskService = engine.getTaskService();

        // Start process
        ProcessInstance pi = startProcess(ksession);

        assertNodeActive(pi.getId(), ksession, TASK_USER_1);

        // check user task
        List<Long> taskIds = taskService.getTasksByProcessInstanceId(pi.getId());
        Task task = taskService.getTaskById(taskIds.get(0));
        displayUserTaskInfos(task);

        // User Task 1
        TaskSummary userTask = taskService.getTasksAssignedAsPotentialOwner(USER_ID, null).get(0);
        assertEquals(userTask.getName(), TASK_USER_1);
        taskService.start(userTask.getId(), USER_ID);
        Map<String, Object> taskData = new HashMap<String, Object>(1);
        taskData.put("taskCaseEvaluation", "TO_USER_TASK_3");
        taskService.complete(userTask.getId(), USER_ID, taskData);

        // ksession.signalEvent("To User Task 3", null, pi.getId());

        assertNodeActive(pi.getId(), ksession, TASK_USER_3);

        // User Task 3
        TaskSummary userTask3 = taskService.getTasksAssignedAsPotentialOwner(USER_ID, null).get(0);
        assertEquals(userTask3.getName(), TASK_USER_3);
        taskService.start(userTask3.getId(), USER_ID);
        Map<String, Object> taskData3 = new HashMap<String, Object>(1);
        taskData3.put("taskCaseEvaluation", "ADHOC-END");
        taskService.complete(userTask3.getId(), USER_ID, taskData3);

        assertNodeTriggered(pi.getId(), TASK_AFTER_ADHOC);

        assertProcessInstanceCompleted(pi.getId());
    }

    private ProcessInstance startProcess(KieSession ksession) {
        Map<String, Object> params = new HashMap<String, Object>();

        ProcessInstance processInstance = ksession.startProcess(PROCESS_NAME, params);

        return processInstance;
    }

}
