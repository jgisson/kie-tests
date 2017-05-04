package jgisson.kie.tests.bpmn;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;

import jgisson.kie.tests.event.MyProcessEventListener;

public class TimersProcessTest extends AbstractProcessTest {

    private static final String PROCESS_FILE = "jgisson/kie/tests/bpmn/timers-tests.bpmn";
    private static final String PROCESS_NAME = "timers-tests";

    public TimersProcessTest() {
        super(true, true);
    }

    @Test
    public void testTimersProcess() {
        createRuntimeManager(PROCESS_FILE);
        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new MyProcessEventListener());

        // Start process
        WorkflowProcessInstance pi = (WorkflowProcessInstance) startProcess(ksession);

        String reminderTimer = (String) pi.getVariable("reminderTimer");
        System.out.println("Reminder timer: " + reminderTimer);

        // Wait timer event
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            // Blank
        }
        assertNodeTriggered(pi.getId(), "Trigger flow");

        assertProcessInstanceCompleted(pi.getId());
    }

    @Test
    public void isoDatetimeJodaTests() {
        Date now = new Date();

        System.out.println("Now in ISO8601 format: " + ISODateTimeFormat.dateTime().print(now.getTime()));
    }

    @Test
    public void durationJodaTests() {
        Date now = new Date();

        Duration duration = new Duration(now.getTime(), now.getTime() + 5000);
        System.out.println("Duration format: " + duration.toString());

        Duration duration2 = new Duration(now.getTime(), now.getTime() + (2 * 24 * 60 * 1000));
        System.out.println("Duration2 format: " + duration2.toString());
        
        Duration duration3 = Duration.standardDays(2);
        System.out.println("Duration3 format: " + duration3.toString());
    }

    private ProcessInstance startProcess(KieSession ksession) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "John");

        ProcessInstance processInstance = ksession.startProcess(PROCESS_NAME, params);

        return processInstance;
    }

}
