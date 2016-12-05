package jgisson.kie.tests.bpmn;


import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;

import jgisson.kie.tests.event.MyProcessEventListener;

public class ProcessOneProcessTest extends AbstractProcessTest {

    private static final String PROCESS_FILE = "jgisson/kie/tests/bpmn/process-one.bpmn";
    private static final String PROCESS_NAME = "process-one";

    public ProcessOneProcessTest() {
        super(true, true);
    }

    @Test
    public void testProcessOne() {
        createRuntimeManager(PROCESS_FILE);
        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ksession.addEventListener(new MyProcessEventListener());

        // Start process
        ProcessInstance processInstance = startProcess(ksession);

        assertProcessInstanceCompleted(processInstance.getId());
    }

    private ProcessInstance startProcess(KieSession ksession) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "John");

        ProcessInstance processInstance = ksession.startProcess(PROCESS_NAME, params);

        return processInstance;
    }

}
