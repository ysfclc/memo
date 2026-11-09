package com.synthos.runtime.engine;

import static org.junit.Assert.assertEquals;
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

    @Test public void nativeTapIsBlockedUntilExactApprovalExists() {
        Workflow workflow = validWorkflow();
        workflow.steps.add(1, new WorkflowStep(WorkflowStep.TAP_TEXT, "Continue", 0));
        String error = WorkflowPolicy.validate(workflow);
        assertNotNull(error);
        assertTrue(error.contains("exact, one-use approval"));
    }

    @Test public void nativeBackIsBlockedBecauseItCanDiscardOrSubmitState() {
        Workflow workflow = validWorkflow();
        workflow.steps.add(1, new WorkflowStep(WorkflowStep.BACK, "", 0));
        String error = WorkflowPolicy.validate(workflow);
        assertNotNull(error);
        assertTrue(error.contains("exact, one-use approval"));
    }

    @Test public void capabilityClassificationFailsClosedForUnknownActions() {
        assertEquals("native.app.launch", WorkflowPolicy.capabilityFor(WorkflowStep.LAUNCH_APP));
        assertEquals("native.ui.observe_text", WorkflowPolicy.capabilityFor(WorkflowStep.WAIT_TEXT));
        assertEquals("native.runtime.delay", WorkflowPolicy.capabilityFor(WorkflowStep.DELAY));
        assertEquals("native.ui.click", WorkflowPolicy.capabilityFor(WorkflowStep.TAP_TEXT));
        assertEquals("native.ui.navigate_back", WorkflowPolicy.capabilityFor(WorkflowStep.BACK));
        assertEquals("native.unknown", WorkflowPolicy.capabilityFor("INVENTED_ACTION"));
        assertNull(WorkflowPolicy.validate(validWorkflow()));
    }

    @Test public void sensitiveTextIsRejectedEvenInObservationStep() {
        Workflow workflow = validWorkflow();
        workflow.steps.set(1, new WorkflowStep(WorkflowStep.WAIT_TEXT, "Confirm payment", 0));
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
