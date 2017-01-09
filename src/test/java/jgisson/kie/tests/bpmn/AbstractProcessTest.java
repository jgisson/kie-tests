package jgisson.kie.tests.bpmn;


import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.kie.api.task.model.Task;

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

    protected void displayUserTaskInfos(Task task) {
        System.out.println("User Task '" + task.getName() + "' informations: taskId=" + task.getId() + "; taskStatus=" + task.getTaskData().getStatus()
                + "; taskOwner=" + task.getTaskData().getActualOwner());
        System.out.println("User Task '" + task.getName() + "' assignment informations: taskBusiness=" + task.getPeopleAssignments().getBusinessAdministrators()
                + "; taskPotentielOwner=" + task.getPeopleAssignments().getPotentialOwners());
    }

}
