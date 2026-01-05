package com.example.aplikacja_dla_strzelcow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.aplikacja_dla_strzelcow.auth.AuthManager
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository
import com.example.aplikacja_dla_strzelcow.data.Session
import com.example.aplikacja_dla_strzelcow.ui.*
import com.example.aplikacja_dla_strzelcow.ui.theme.Aplikacja_dla_strzelcowTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private val repository = FirestoreRepository()

    private var userEmail by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(false)
    private var sessions by mutableStateOf<List<Session>>(emptyList())

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode != RESULT_OK || result.data == null) {
            isLoading = false
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {
            val account = task.getResult(Exception::class.java)
            val idToken = account.idToken ?: return@registerForActivityResult

            authManager.firebaseAuthWithGoogle(
                idToken = idToken,
                onSuccess = {
                    val user = authManager.auth.currentUser
                    userEmail = user?.email
                    isLoading = false

                    repository.saveUser(
                        email = user?.email,
                        name = user?.displayName
                    )

                    repository.getSessions {
                        sessions = it
                    }
                },
                onError = {
                    isLoading = false
                }
            )

        } catch (_: Exception) {
            isLoading = false
        }
    }

    private fun signOut() {
        authManager.signOut()
        userEmail = null
        sessions = emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authManager = AuthManager(
            context = this,
            webClientId = getString(R.string.default_web_client_id)
        )

        val currentUser = authManager.auth.currentUser
        userEmail = currentUser?.email

        if (currentUser != null) {
            repository.getSessions {
                sessions = it
            }
        }

        setContent {
            val context = LocalContext.current

            Aplikacja_dla_strzelcowTheme {

                if (userEmail == null) {
                    // EKRAN LOGOWANIA
                    LoginScreen(
                        isLoading = isLoading,
                        onLoginClick = {
                            isLoading = true
                            launcher.launch(authManager.getSignInIntent())
                        }
                    )
                } else {
                    // GŁÓWNY EKRAN APLIKACJI
                    MainScreen(
                        onLogout = { signOut() },
                        onNewTraining = {
                            context.startActivity(
                                Intent(context, AddSessionActivity::class.java)
                            )
                        },
                        homeContent = {
                            HomeScreen(
                                sessions = sessions,
                                onSessionClick = { session ->
                                    context.startActivity(
                                        Intent(
                                            context,
                                            SessionDetailsActivity::class.java
                                        ).apply {
                                            putExtra("sessionId", session.id)
                                        }
                                    )
                                },
                                onNewTraining = {
                                    context.startActivity(
                                        Intent(context, AddSessionActivity::class.java)
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}


//    private lateinit var auth: FirebaseAuth
//    private lateinit var googleSignInClient: GoogleSignInClient
//    private val db = FirebaseFirestore.getInstance()

//    private fun saveUserToFirestore() {
//        val user = auth.currentUser ?: return
//
//        val userData = hashMapOf(
//            "email" to user.email,
//            "name" to user.displayName,
//            "createdAt" to com.google.firebase.Timestamp.now(),
//            "lastLogin" to com.google.firebase.Timestamp.now()
//        )
//
//        db.collection("users")
//            .document(user.uid)
//            .set(userData)
//    }

//    private fun createSession(
//        date: String,
//        location: String,
//        notes: String
//    ) {
//        val user = auth.currentUser ?: return
//
//        val sessionData = hashMapOf(
//            "date" to date,
//            "location" to location,
//            "notes" to notes,
//            "createdAt" to com.google.firebase.Timestamp.now()
//        )
//
//        db.collection("users")
//            .document(user.uid)
//            .collection("sessions")
//            .add(sessionData)
//    }
//    private fun createSeries(
//        sessionId: String,
//        weapon: String,
//        ammo: String,
//        distance: Int
//    ) {
//        val user = auth.currentUser ?: return
//        val testSessionId = "6sMbgdX9tkrTNqFZYVuR"
//        val seriesData = hashMapOf(
//            "weapon" to weapon,
//            "ammo" to ammo,
//            "distance" to distance,
//            "createdAt" to com.google.firebase.Timestamp.now()
//        )
//
//        db.collection("users")
//            .document(user.uid)
//            .collection("sessions")
//            .document(sessionId)
//            .collection("series")
//            .add(seriesData)
//    }
//
//    private fun addShot(
//        sessionId: String,
//        seriesId: String,
//        x: Float,
//        y: Float,
//        value: Int
//    ) {
//        val user = auth.currentUser ?: return
//
//        val shotData = hashMapOf(
//            "x" to x,
//            "y" to y,
//            "value" to value,
//            "timestamp" to com.google.firebase.Timestamp.now()
//        )
//
//        db.collection("users")
//            .document(user.uid)
//            .collection("sessions")
//            .document(sessionId)
//            .collection("series")
//            .document(seriesId)
//            .collection("shots")
//            .add(shotData)
//    }