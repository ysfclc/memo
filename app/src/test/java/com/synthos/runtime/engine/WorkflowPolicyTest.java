package com.synthos.runtime.engine;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.synthos.runtime.data.Workflow;
import com.synthos.runtime.data.WorkflowStep;

import org.junit.Test;

public final class WorkflowPolicyTest {
    private Workflow validWorkflow() {
        Workflow workflow = new Workflow("Open settings", "com.android.settings", "Settings");
        workflow.steps.add(new WorkflowStep(WorkflowStep.LAUNCH_APP, "Settings", 0));
        workflow.steps.add(new WorkflowStep(WorkflowStep.WAIT_TEXT, "Settings", 0));
        return workflow;
    }

    @Test public void finalVisiblePostconditionIsRequired() {
        Workflow workflow = validWorkflow();
        workflow.steps.remove(workflow.steps.size() - 1);
        assertNotNull(WorkflowPolicy.validate(workflow));
    }

    @Test public void ordinaryBoundedWorkflowPassesPolicy() {
        assertNull(WorkflowPolicy.validate(validWorkflow()));
    }

    @Test public void sensitiveTextIsRejected() {
        Workflow workflow = validWorkflow();
        workflow.steps.add(1, new WorkflowStep(WorkflowStep.TAP_TEXT, "Submit payment", 0));
        String error = WorkflowPolicy.validate(workflow);
        assertNotNull(error);
        assertTrue(error.contains("does not automate"));
    }

    @Test public void workflowHasMaximumOfThirtySteps() {
        Workflow workflow = new Workflow("Too many steps", "com.android.settings", "Settings");
        workflow.steps.add(new WorkflowStep(WorkflowStep.LAUNCH_APP, "Settings", 0));
        for (int i = 0; i < 30; i++) workflow.steps.add(new WorkflowStep(WorkflowStep.DELAY, "", 0));
        workflow.steps.add(new WorkflowStep(WorkflowStep.WAIT_TEXT, "Settings", 0));
        assertNotNull(WorkflowPolicy.validate(workflow));
    }
}
