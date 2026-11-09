package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayPriorityTest {
    @Test fun prioritiesRoundTripForTasksAndPlans() {
        val task = TaskRecord(title = "Capture research", todayPriority = "MUST")
        val job = WorkJobRecord(title = "Prepare launch", goal = "Ship the reviewed launch plan", todayPriority = "NOT_TODAY")
        val restored = AppSnapshot(tasks = listOf(task), jobs = listOf(job)).let { AppSnapshot.fromJson(it.toJson()) }
        assertEquals("MUST", restored.tasks.single().todayPriority)
        assertEquals("NOT_TODAY", restored.jobs.single().todayPriority)
    }

    @Test fun oldRecordsAndUnknownImportedPrioritiesBecomeUnsorted() {
        val root = org.json.JSONObject(AppSnapshot(
            tasks = listOf(TaskRecord(title = "Legacy task")),
            jobs = listOf(WorkJobRecord(title = "Legacy plan", goal = "Old backup"))
        ).toJson())
        root.getJSONArray("tasks").getJSONObject(0).remove("todayPriority")
        root.getJSONArray("jobs").getJSONObject(0).put("todayPriority", "URGENT")
        val restored = AppSnapshot.fromJson(root.toString())
        assertEquals("UNSORTED", restored.tasks.single().todayPriority)
        assertEquals("UNSORTED", restored.jobs.single().todayPriority)
    }
}
