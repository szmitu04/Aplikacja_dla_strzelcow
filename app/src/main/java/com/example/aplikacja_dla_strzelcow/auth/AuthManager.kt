package com.example.aplikacja_dla_strzelcow.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthManager(
    context: Context,
    private val webClientId: String
) {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent =
        googleSignInClient.signInIntent

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }

    fun firebaseAuthWithGoogle(
        idToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Błąd logowania") }
    }
}
