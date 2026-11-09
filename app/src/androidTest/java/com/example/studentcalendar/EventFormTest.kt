package com.example.studentcalendar

import android.view.View
import android.widget.RadioButton
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.studentcalendar.inicio.LoginActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventFormTest {
    @Test fun switchingToTaskHidesRecurrenceAndShowsItsDueDate() {
        ActivityScenario.launch(AnadirClaseTareaActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val recurringDays = activity.findViewById<View>(R.id.ll_days_checkboxes)
                val dueDate = activity.findViewById<View>(R.id.btn_select_date)
                assertEquals(View.VISIBLE, recurringDays.visibility)
                assertEquals(View.GONE, dueDate.visibility)

                activity.findViewById<RadioButton>(R.id.rb_task).performClick()
                assertEquals(View.GONE, recurringDays.visibility)
                assertEquals(View.VISIBLE, dueDate.visibility)

                activity.findViewById<RadioButton>(R.id.rb_class).performClick()
                assertEquals(View.VISIBLE, recurringDays.visibility)
                assertEquals(View.GONE, dueDate.visibility)
            }
        }
    }

    @Test fun unauthenticatedLoginShowsGoogleEntryPoint() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val google = activity.findViewById<View>(R.id.btnGoogleSignIn)
                assertEquals(View.VISIBLE, google.visibility)
                assertTrue(google.isEnabled)
            }
        }
    }
}
