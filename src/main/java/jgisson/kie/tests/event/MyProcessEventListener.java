package jgisson.kie.tests.event;


import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.drools.core.marshalling.impl.ProcessMarshallerWriteContext;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.EnvironmentName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyProcessEventListener implements ProcessEventListener {

    private static final String DEFAULT_PROCESS_PRIORITY = "10";

    private static final String VAR_PROCESS_PRIORITY = "piPriority";

    private final Logger logger = LoggerFactory.getLogger(MyProcessEventListener.class);

    public void afterNodeLeft(ProcessNodeLeftEvent event) {
        // intentionally left blank
    }

    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
        // intentionally left blank
    }

    public void afterProcessCompleted(ProcessCompletedEvent event) {
        // intentionally left blank
    }

    public void afterProcessStarted(ProcessStartedEvent event) {
        // intentionally left blank
    }

    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        // intentionally left blank
    }

    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
        // intentionally left blank
    }

    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        // intentionally left blank
    }

    public void beforeProcessCompleted(ProcessCompletedEvent event) {
        logger.debug("Completed process {} with id {}", event.getProcessInstance().getProcessId(), event.getProcessInstance().getId());

        // WorkflowProcessInstanceImpl pi = (WorkflowProcessInstanceImpl) event.getProcessInstance();

        // Be sure all variable are marshal before complete process (i.e. MarshalVariablesProcessEventListener.java)
        marshalVarsOnProcessCompleted(event);
    }

    public void beforeProcessStarted(ProcessStartedEvent event) {
        logger.debug("Start process {} with id {}", event.getProcessInstance().getProcessId(), event.getProcessInstance().getId());

        WorkflowProcessInstanceImpl pi = (WorkflowProcessInstanceImpl) event.getProcessInstance();

        // Update process priority only for main process instance
        if (pi.getParentProcessInstanceId() < 1) {
            pi.setVariable(VAR_PROCESS_PRIORITY, DEFAULT_PROCESS_PRIORITY);
        }
    }

    public void beforeVariableChanged(ProcessVariableChangedEvent event) {
        // intentionally left blank
    }

    private void marshalVarsOnProcessCompleted(ProcessCompletedEvent event) {
        ObjectMarshallingStrategy[] strategies =
                (ObjectMarshallingStrategy[]) event.getKieRuntime().getEnvironment().get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);

        VariableScopeInstance variableScope =
                (VariableScopeInstance) ((WorkflowProcessInstance) event.getProcessInstance()).getContextInstance(VariableScope.VARIABLE_SCOPE);

        Map<String, Object> variables = variableScope.getVariables();

        for (Map.Entry<String, Object> variable : variables.entrySet()) {
            logger.debug("Searching for applicable strategy to handle variable name '{}' value '{}'", variable.getKey(), variable.getValue());
            for (ObjectMarshallingStrategy strategy : strategies) {
                // skip default strategy as it requires context and anyway will not make any effect as variables
                // are removed together with process instance
                if (strategy instanceof SerializablePlaceholderResolverStrategy) {
                    continue;
                }
                if (strategy.accept(variable.getValue())) {
                    logger.debug("Strategy of type {} found to handle variable '{}'", strategy, variable.getKey());
                    try {
                        ProcessMarshallerWriteContext context =
                                new ProcessMarshallerWriteContext(new ByteArrayOutputStream(), null, null, null, null, event.getKieRuntime().getEnvironment());
                        context.setProcessInstanceId(event.getProcessInstance().getId());
                        context.setState(ProcessMarshallerWriteContext.STATE_COMPLETED);

                        strategy.marshal(null, context, variable.getValue());
                        logger.debug("Variable '{}' successfully persisted by strategy {}", variable.getKey(), strategy);
                        break;
                    } catch (Exception e) {
                        logger.warn("Errer while storing process variable {} due to {}", variable.getKey(), e.getMessage());
                        logger.debug("Variable marshal error:", e);
                    }
                }
            }
        }
    }

}
