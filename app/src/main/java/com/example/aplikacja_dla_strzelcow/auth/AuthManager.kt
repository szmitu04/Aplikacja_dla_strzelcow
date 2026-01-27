package com.example.aplikacja_dla_strzelcow.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
//import androidx.privacysandbox.tools.core.generator.build
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class AuthManager(
    context: Context,
    //private val context: Context,
    private val webClientId: String
) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // 1. Stwórz Credential Managera
    private val credentialManager = CredentialManager.create(context)

    // 2. Nowa funkcja rozpoczynająca proces logowania
    suspend fun signIn(activity: Activity): GetCredentialResponse {
        // Skonfiguruj opcje logowania przez Google, używając webClientId
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Pokaż wszystkie konta Google na urządzeniu
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true) // Automatycznie wybierz jedyne konto, jeśli jest dostępne
            .build()

        // Stwórz zapytanie do Credential Managera
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Wywołaj systemowy interfejs logowania
        return credentialManager.getCredential(activity, request)
    }

    // 3. Nowa funkcja do logowania w Firebase za pomocą uzyskanego tokenu
    fun firebaseAuthWithGoogle(
        response: GetCredentialResponse,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        when (val credential = response.credential) {
            is GoogleIdTokenCredential -> {
                // Idealny scenariusz
                Log.d("AuthManager", "Otrzymano typ: GoogleIdTokenCredential")
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener {
                        Log.e("AuthManager", "Błąd logowania do Firebase (GoogleIdTokenCredential): ${it.message}", it)
                        onError(it.message ?: "Błąd logowania Firebase")
                    }
            }
            is CustomCredential -> {
                // 🔴 NOWA, WAŻNA OBSŁUGA BŁĘDU 🔴
                // Ten typ jest często zwracany przez `credentials-play-services-auth`
                Log.d("AuthManager", "Otrzymano typ: CustomCredential. Sprawdzanie, czy to Google...")
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener {
                                Log.e("AuthManager", "Błąd logowania do Firebase (CustomCredential): ${it.message}", it)
                                onError(it.message ?: "Błąd logowania Firebase")
                            }
                    } catch (e: Exception) {
                        Log.e("AuthManager", "Błąd przy konwersji CustomCredential na GoogleIdTokenCredential", e)
                        onError("Błąd przetwarzania odpowiedzi Google.")
                    }
                } else {
                    onError("Nieobsługiwany typ CustomCredential: ${credential.type}")
                }
            }
            else -> {
                onError("Niespodziewany typ poświadczeń: ${credential::class.java.name}")
            }
        }
    }

    // 4. Funkcja wylogowania pozostaje prawie bez zmian
    fun signOut() {
        auth.signOut()
        // Nie ma już potrzeby wylogowywać się z `googleSignInClient`
    }
}
//class AuthManager(
//    context: Context,
//    private val webClientId: String
//) {
//
//    val auth: FirebaseAuth = FirebaseAuth.getInstance()
//
//    private val googleSignInClient: GoogleSignInClient
//
//    init {
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(webClientId)
//            .requestEmail()
//            .build()
//
//        googleSignInClient = GoogleSignIn.getClient(context, gso)
//    }
//
//    fun getSignInIntent(): Intent =
//        googleSignInClient.signInIntent
//
//    fun signOut() {
//        auth.signOut()
//        googleSignInClient.signOut()
//    }
//
//    fun firebaseAuthWithGoogle(
//        idToken: String,
//        onSuccess: () -> Unit,
//        onError: (String) -> Unit
//    ) {
//        val credential = GoogleAuthProvider.getCredential(idToken, null)
//        auth.signInWithCredential(credential)
//            .addOnSuccessListener { onSuccess() }
//            .addOnFailureListener { onError(it.message ?: "Błąd logowania") }
//    }
//}
