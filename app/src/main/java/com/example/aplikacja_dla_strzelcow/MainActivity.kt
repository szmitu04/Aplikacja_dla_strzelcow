package com.example.aplikacja_dla_strzelcow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aplikacja_dla_strzelcow.ui.theme.Aplikacja_dla_strzelcowTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val db = FirebaseFirestore.getInstance()

    private var userEmail by mutableStateOf<String?>(null)
    private var statusMessage by mutableStateOf("Nie zalogowano")
    private var isLoading by mutableStateOf(false)

    private fun saveUserToFirestore() {
        val user = auth.currentUser ?: return

        val userData = hashMapOf(
            "email" to user.email,
            "name" to user.displayName,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "lastLogin" to com.google.firebase.Timestamp.now()
        )

        db.collection("users")
            .document(user.uid)
            .set(userData)
    }

    private fun createSession(
        date: String,
        location: String,
        notes: String
    ) {
        val user = auth.currentUser ?: return

        val sessionData = hashMapOf(
            "date" to date,
            "location" to location,
            "notes" to notes,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users")
            .document(user.uid)
            .collection("sessions")
            .add(sessionData)
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        statusMessage = "Powrót z Google Sign-In"

        if (result.resultCode == RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                statusMessage = "Pobieranie konta Google"
                val account = task.getResult(Exception::class.java)

                if (account.idToken == null) {
                    statusMessage = "BŁĄD: idToken == null"
                    isLoading = false
                    return@registerForActivityResult
                }

                statusMessage = "Logowanie do Firebase"
                val credential =
                    GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        userEmail = it.user?.email
                        statusMessage = "ZALOGOWANO POPRAWNIE"
                        isLoading = false
                        saveUserToFirestore()

                        createSession(
                            date = "2025-03-12",
                            location = "Strzelnica testowa",
                            notes = "Pierwsza sesja testowa"
                        )


                    }
                    .addOnFailureListener {
                        statusMessage = "BŁĄD Firebase: ${it.message}"
                        isLoading = false
                    }

            } catch (e: Exception) {
                statusMessage = "BŁĄD Google: ${e.message}"
                isLoading = false
            }

        } else {
            statusMessage = "Anulowano logowanie"
            isLoading = false
        }
    }
    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()

        userEmail = null
        statusMessage = "Wylogowano"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        userEmail = currentUser?.email
        statusMessage = if (currentUser != null) {
            "Zalogowano"
        } else {
            "Nie zalogowano"
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            Aplikacja_dla_strzelcowTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text("Status:")
                    Text(statusMessage)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (userEmail == null) {

                        Button(
                            onClick = {
                                isLoading = true
                                statusMessage = "Rozpoczynam logowanie..."
                                launcher.launch(googleSignInClient.signInIntent)
                            },
                            enabled = !isLoading
                        ) {
                            Text("Zaloguj się przez Google")
                        }

                    } else {
                        Text("Zalogowano jako:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(userEmail!!)

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { signOut() }) {
                            Text("Wyloguj się")
                        }
                    }
                }
            }
        }
    }
}
