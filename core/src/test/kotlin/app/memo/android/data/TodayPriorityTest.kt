package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayPriorityTest {
    @Test fun prioritiesRoundTripForTasksAndPlans() {
        val day = todayPriorityDay()
        val task = TaskRecord(title = "Capture research", todayPriority = "MUST", todayPriorityDate = day)
        val job = WorkJobRecord(title = "Prepare launch", goal = "Ship the reviewed launch plan", todayPriority = "NOT_TODAY", todayPriorityDate = day)
        val restored = AppSnapshot(tasks = listOf(task), jobs = listOf(job)).let { AppSnapshot.fromJson(it.toJson()) }
        assertEquals("MUST", restored.tasks.single().todayPriority)
        assertEquals(day, restored.tasks.single().todayPriorityDate)
        assertEquals("NOT_TODAY", restored.jobs.single().todayPriority)
        assertEquals(day, restored.jobs.single().todayPriorityDate)
    }

    @Test fun oldRecordsAndUnknownImportedPrioritiesBecomeUnsorted() {
        val root = org.json.JSONObject(AppSnapshot(
            tasks = listOf(TaskRecord(title = "Legacy task", todayPriority = "MUST")),
            jobs = listOf(WorkJobRecord(title = "Legacy plan", goal = "Old backup", todayPriority = "SHOULD"))
        ).toJson())
        root.getJSONArray("tasks").getJSONObject(0).remove("todayPriorityDate")
        root.getJSONArray("jobs").getJSONObject(0).put("todayPriority", "URGENT")
        val restored = AppSnapshot.fromJson(root.toString())
        assertEquals("UNSORTED", restored.tasks.single().todayPriority)
        assertEquals("", restored.tasks.single().todayPriorityDate)
        assertEquals("UNSORTED", restored.jobs.single().todayPriority)
    }

    @Test fun yesterdayPriorityDoesNotRemainInToday() {
        val task = TaskRecord(title = "Review notes", todayPriority = "MUST", todayPriorityDate = "2000-01-01")
        val restored = AppSnapshot.fromJson(AppSnapshot(tasks = listOf(task)).toJson())
        assertEquals("MUST", restored.tasks.single().todayPriority)
        assertEquals("2000-01-01", restored.tasks.single().todayPriorityDate)
        assertEquals(false, restored.tasks.single().todayPriorityDate == todayPriorityDay())
    }
}
