package com.example.studentcalendar

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import android.util.Log
import com.example.studentcalendar.horariosemanal.CalendarEvent
import com.example.studentcalendar.inicio.LoginActivity
import com.example.studentcalendar.inicio.SplashActivity
import com.example.studentcalendar.notificaciones.ReminderStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges

class KalendUApplication : Application() {
    private var listener: ListenerRegistration? = null
    private var listeningUid: String? = null

    override fun onCreate() {
        super.onCreate()
        val auth = FirebaseAuth.getInstance()
        val store = ReminderStore(this)
        auth.addAuthStateListener {
            val uid = it.currentUser?.uid
            store.useAccount(uid)
            if (uid == listeningUid) return@addAuthStateListener
            listener?.remove()
            listener = null
            listeningUid = uid
            if (uid == null) return@addAuthStateListener

            listener = FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("events").whereEqualTo("notificacionActivada", true)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.w("KalendUReminders", "Could not synchronize reminders", error)
                        return@addSnapshotListener
                    }
                    if (auth.currentUser?.uid != uid || snapshot == null ||
                        snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()
                    ) return@addSnapshotListener
                    val events = snapshot.documents.mapNotNull { doc ->
                        runCatching { doc.toObject(CalendarEvent::class.java)?.copy(eventId = doc.id, userId = uid) }
                            .onFailure { Log.w("KalendUReminders", "Invalid reminder document", it) }
                            .getOrNull()
                    }
                    store.reconcile(uid, events)
                }
        }
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                auth.currentUser?.uid?.let(store::restore)
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityPostCreated(activity: Activity, state: Bundle?) {
                val content = activity.findViewById<View>(android.R.id.content)
                if (activity is LoginActivity || activity is SplashActivity) {
                    activity.window.setBackgroundDrawableResource(R.drawable.kalendu_auth_background)
                    WindowCompat.getInsetsController(activity.window, content).isAppearanceLightStatusBars = false
                }
                if (Build.VERSION.SDK_INT < 35) return
                ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
                    val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                    val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
                    view.setPadding(safe.left, safe.top, safe.right, maxOf(safe.bottom, keyboard.bottom))
                    WindowInsetsCompat.CONSUMED
                }
                ViewCompat.requestApplyInsets(content)
            }
            override fun onActivityStarted(activity: Activity) {
                val nightMode = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val authScreen = activity is LoginActivity || activity is SplashActivity
                val window = activity.window
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
                    !authScreen && nightMode != Configuration.UI_MODE_NIGHT_YES
            }
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
