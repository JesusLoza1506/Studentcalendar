package com.example.studentcalendar.ajustes

import com.example.studentcalendar.notificaciones.ReminderStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studentcalendar.R
import com.example.studentcalendar.databinding.ActivityAjustesBinding
import com.example.studentcalendar.inicio.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.studentcalendar.inicio.MainActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class AjustesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAjustesBinding
    private lateinit var auth: FirebaseAuth
    private var isDarkMode: Boolean = false
    private val PICK_IMAGE_REQUEST = 1001
    private var selectedImageUri: Uri? = null
    private var imageViewDialog: ImageView? = null
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private var deletionUid: String? = null
    private var deleting = false

    override fun attachBaseContext(newBase: Context) {
        val context = LocaleHelper.establecerLocalizacion(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.aplicarTema(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAjustesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
        isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        inicializarTextosUI()
        mostrarInformacionUsuario()
        configurarAcciones()
    }

    private fun inicializarTextosUI() {
        binding.cardPerfil.text = getString(R.string.btn_ver_editar_perfil)
        binding.cardModoOscuro.text = getString(R.string.btn_cambiar_modo_oscuro_claro)
        binding.cardIdioma.text = getString(R.string.btn_cambiar_idioma)
        binding.cardSincronizados.text = getString(R.string.btn_ver_datos_sincronizados)
        binding.cardTerminos.text = getString(R.string.btn_terminos_condiciones)
        binding.cardCerrarSesion.text = getString(R.string.btn_cerrar_sesion_eliminar_cuenta)
    }

    private fun mostrarInformacionUsuario() {
        val user: FirebaseUser? = auth.currentUser
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val localUri = prefs.getString("localPhotoUri_${user?.uid}", null)
        binding.tvUserName.text = "👤 ${user?.displayName ?: getString(R.string.default_username_placeholder)}"
        if (localUri != null) {
            Glide.with(this).load(Uri.parse(localUri)).circleCrop().into(binding.imgUserPhoto)
        } else if (user?.photoUrl != null) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(binding.imgUserPhoto)
        } else {
            binding.imgUserPhoto.setImageResource(R.drawable.user)
        }
    }

    private fun configurarAcciones() {
        binding.cardPerfil.setOnClickListener {
            mostrarDialogoEditarPerfil()
        }
        binding.cardModoOscuro.setOnClickListener {
            alternarModoOscuro()
        }
        binding.cardIdioma.setOnClickListener {
            mostrarSelectorIdioma()
        }
        binding.cardSincronizados.setOnClickListener {
            startActivity(Intent(this, DatosSincronizadosActivity::class.java))
        }
        binding.cardTerminos.setOnClickListener {
            startActivity(Intent(this, TerminosCondicionesActivity::class.java))
        }
        binding.cardCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }
    }

    private fun mostrarDialogoEditarPerfil() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_editar_perfil, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imgNuevaFoto)
        val editText = dialogView.findViewById<EditText>(R.id.etNuevoNombre)
        imageViewDialog = imageView
        val user = auth.currentUser
        editText.setText(user?.displayName ?: "")
        val textColor = if (!isDarkMode) Color.BLACK else Color.WHITE
        editText.setTextColor(textColor)
        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val localUri = prefs.getString("localPhotoUri_${user?.uid}", null)
        if (localUri != null) {
            Glide.with(this).load(Uri.parse(localUri)).circleCrop().into(imageView)
        } else if (user?.photoUrl != null) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(imageView)
        } else {
            imageView.setImageResource(R.drawable.user)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_edit_profile_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.dialog_save_button)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, getString(R.string.toast_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                actualizarPerfil(newName)
            }
            .setNegativeButton(getString(R.string.dialog_cancel_button), null)
            .create()
        imageView.setOnClickListener { abrirGaleria() }
        dialog.show()
    }

    private fun alternarModoOscuro() {
        isDarkMode = !isDarkMode
        ThemeHelper.establecerModoOscuro(this, isDarkMode)
        recreate()
    }

    private fun mostrarSelectorIdioma() {
        val opciones = arrayOf(
            getString(R.string.dialog_change_language_option_es),
            getString(R.string.dialog_change_language_option_en),
            getString(R.string.dialog_change_language_option_pt_rBR),
            getString(R.string.dialog_change_language_option_it),
            getString(R.string.dialog_change_language_option_fr)
        )
        val codigos = arrayOf(
            "es",
            "en",
            "pt-rBR",
            "it",
            "fr"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_change_language_title))
            .setItems(opciones) { _, which ->
                cambiarIdioma(codigos[which])
            }
            .setNegativeButton(getString(R.string.dialog_cancel_button), null)
            .show()
    }

    private fun cambiarIdioma(codigoIdioma: String) {
        LocaleHelper.cambiarIdioma(this, codigoIdioma)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image_chooser_title)), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            selectedImageUri = data.data
            imageViewDialog?.setImageURI(selectedImageUri)
            val user = auth.currentUser
            user?.uid?.let { uid ->
                getSharedPreferences("UserProfile", MODE_PRIVATE)
                    .edit()
                    .putString("localPhotoUri_$uid", selectedImageUri.toString())
                    .apply()
            }
        }
    }

    private fun actualizarPerfil(nombre: String) {
        val user = auth.currentUser
        val builder = UserProfileChangeRequest.Builder().setDisplayName(nombre)
        user?.updateProfile(builder.build())?.addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, getString(R.string.toast_profile_updated), Toast.LENGTH_SHORT).show()
                mostrarInformacionUsuario()
                selectedImageUri = null
            } else {
                Toast.makeText(this, getString(R.string.toast_error_updating_profile), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCerrarSesion() {
        val opciones = arrayOf(
            getString(R.string.dialog_logout_option),
            getString(R.string.dialog_delete_data_option)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_what_to_do_title))
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> cerrarSesion()
                    1 -> eliminarDatosUsuario()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel_button), null)
            .show()
    }

    private fun cerrarSesion() {
        if (deleting) return
        auth.signOut()
        oneTapClient.signOut()
        Toast.makeText(this, getString(R.string.toast_session_closed), Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun eliminarDatosUsuario() {
        if (deleting || deletionUid != null) return
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, getString(R.string.toast_no_active_user), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_confirm_delete_data_title))
            .setMessage(getString(R.string.dialog_confirm_delete_data_message))
            .setPositiveButton(getString(R.string.dialog_confirm_delete_data_button)) { _, _ ->
                deletionUid = user.uid
                mostrarDialogoReautenticacion()
            }
            .setNegativeButton(getString(R.string.dialog_cancel_button), null)
            .show()
    }

    private fun mostrarDialogoReautenticacion() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_reauth_title))
            .setMessage(getString(R.string.dialog_reauth_message))
            .setPositiveButton(getString(R.string.dialog_reauth_confirm_button)) { _, _ ->
                oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener { result ->
                        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        reauthLauncher.launch(intentSenderRequest)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.toast_reauth_flow_failed, e.message), Toast.LENGTH_LONG).show()
                        Log.e("AjustesActivity", "Error al iniciar flujo de reautenticación: ${e.message}", e)
                        deletionUid = null
                    }
            }
            .setNegativeButton(getString(R.string.dialog_cancel_button)) { _, _ ->
                Toast.makeText(this, getString(R.string.toast_deletion_cancelled), Toast.LENGTH_SHORT).show()
                deletionUid = null
            }
            .setOnCancelListener { deletionUid = null }
            .create()
        dialog.show()
    }

    private fun ejecutarEliminacionDeDatos(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val events = db.collection("users").document(uid).collection("events")
        fun fail(error: Exception) {
            deleting = false
            Log.w("AjustesActivity", "Could not delete all event data", error)
            Toast.makeText(this, R.string.delete_data_retry, Toast.LENGTH_LONG).show()
        }
        fun deleteNextBatch() {
            if (auth.currentUser?.uid != uid) {
                fail(IllegalStateException("Account changed during deletion"))
                return
            }
            events.limit(400).get(Source.SERVER).addOnSuccessListener { snapshot ->
                if (auth.currentUser?.uid != uid) {
                    fail(IllegalStateException("Account changed during deletion"))
                    return@addOnSuccessListener
                }
                if (snapshot.isEmpty) {
                    getSharedPreferences("UserProfile", MODE_PRIVATE).edit()
                        .remove("localPhotoUri_$uid").apply()
                    deleting = false
                    Toast.makeText(this, R.string.toast_data_deleted_successfully, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val batch = db.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit().addOnSuccessListener {
                        ReminderStore(this).remove(uid, snapshot.documents.map { it.id })
                        deleteNextBatch()
                    }.addOnFailureListener { fail(it) }
                }
            }.addOnFailureListener { fail(it) }
        }
        deleting = true
        deleteNextBatch()
    }

    private val reauthLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val expectedUid = deletionUid ?: return@registerForActivityResult
        deletionUid = null
        val user = auth.currentUser
        if (result.resultCode != RESULT_OK || user == null || user.uid != expectedUid) {
            Toast.makeText(this, R.string.toast_reauthentication_cancelled, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val idToken = runCatching { oneTapClient.getSignInCredentialFromIntent(result.data).googleIdToken }.getOrNull()
        if (idToken == null) {
            Toast.makeText(this, R.string.toast_reauthentication_id_token_null, Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        deleting = true
        user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null))
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && auth.currentUser?.uid == expectedUid) {
                    ejecutarEliminacionDeDatos(expectedUid)
                } else {
                    deleting = false
                    Toast.makeText(this, R.string.delete_data_wrong_account, Toast.LENGTH_LONG).show()
                }
            }
    }
}
