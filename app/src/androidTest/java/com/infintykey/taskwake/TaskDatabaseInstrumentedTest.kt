package com.infintykey.taskwake

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDatabaseInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "taskwake-instrumented-test.db"
    private lateinit var database: TaskDatabase

    @Before
    fun createDatabase() {
        context.deleteDatabase(databaseName)
        database = Room.databaseBuilder(context, TaskDatabase::class.java, databaseName).build()
    }

    @After
    fun closeDatabase() {
        if (::database.isInitialized && database.isOpen) database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun tasksCanBeCreatedUpdatedPersistedAndDeleted() = runBlocking {
        val dao = database.taskDao()
        val taskId = dao.upsert(TaskEntity(title = "اختبار", description = "وصف", reminderAtMillis = 9_999_999_999L))
        assertTrue(taskId > 0L)

        val inserted = dao.observeAll().first().single()
        assertEquals("اختبار", inserted.title)
        assertEquals("وصف", inserted.description)

        database.close()
        database = Room.databaseBuilder(context, TaskDatabase::class.java, databaseName).build()
        val restoredDao = database.taskDao()
        val restored = restoredDao.observeAll().first().single()
        assertEquals(taskId, restored.id)

        restoredDao.update(restored.copy(isCompleted = true))
        assertTrue(restoredDao.observeAll().first().single().isCompleted)

        restoredDao.deleteById(taskId)
        assertTrue(restoredDao.observeAll().first().isEmpty())
    }

    @Test
    fun onlyIncompleteFutureRemindersAreReturnedForBootRestore() = runBlocking {
        val dao = database.taskDao()
        dao.upsert(TaskEntity(title = "قادمة", reminderAtMillis = 2_000L))
        dao.upsert(TaskEntity(title = "مكتملة", reminderAtMillis = 3_000L, isCompleted = true))
        dao.upsert(TaskEntity(title = "بلا تذكير"))

        val pending = dao.getFutureReminders(nowMillis = 1_000L)
        assertEquals(listOf("قادمة"), pending.map { it.title })
    }
}
