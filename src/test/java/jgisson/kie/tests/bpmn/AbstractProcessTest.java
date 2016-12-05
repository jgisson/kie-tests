package jgisson.kie.tests.bpmn;


import org.jbpm.test.JbpmJUnitBaseTestCase;

/**
 * JUnit tests for jBPM processes.
 */
public abstract class AbstractProcessTest extends JbpmJUnitBaseTestCase {


    /**
     * Call super class constructor with persistence config.
     */
    public AbstractProcessTest(boolean setupDataSource, boolean sessionPersistence) {
        super(setupDataSource, sessionPersistence);
    }

}
