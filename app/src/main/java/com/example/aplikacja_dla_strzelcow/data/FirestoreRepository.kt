package com.example.aplikacja_dla_strzelcow.data

import androidx.core.net.toUri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun uid(): String? = auth.currentUser?.uid

    fun saveUser(email: String?, name: String?) {
        val userId = uid() ?: return

        val data = hashMapOf(
            "email" to email,
            "name" to name,
            "createdAt" to Timestamp.now(),
            "lastLogin" to Timestamp.now()
        )

        db.collection("users")
            .document(userId)
            .set(data)
    }

    fun createSession(
        location: String,
        notes: String
    ) {
        val userId = uid() ?: return

        val session = hashMapOf(
            "createdAt" to Timestamp.now(),
            "location" to location,
            "notes" to notes
        )

        db.collection("users")
            .document(userId)
            .collection("sessions")
            .add(session)
    }
    fun createSeriesWithShots(
        sessionId: String,
        series: Series, // Przekazujemy cały obiekt serii
        shots: List<Shot>, // Przekazujemy listę strzałów
        onCreated: (String) -> Unit
    ) {
        val userId = uid() ?: return

        // 1. Zapisujemy dokument serii
        db.collection("users").document(userId)
            .collection("sessions").document(sessionId)
            .collection("series")
            .add(series) // Firestore ignoruje pole `id` przy zapisie
            .addOnSuccessListener { seriesDocRef ->
                val seriesId = seriesDocRef.id
                onCreated(seriesId) // Zwracamy ID utworzonej serii

                // 2. W pętli zapisujemy wszystkie strzały do podkolekcji
                val batch = db.batch()
                shots.forEach { shot ->
                    val shotDocRef = seriesDocRef.collection("shots").document()
                    batch.set(shotDocRef, shot)
                }
                batch.commit() // Wykonujemy wszystkie operacje zapisu strzałów naraz
            }
    }
    fun createSeries(
        sessionId: String,
        weapon: String,
        ammo: String,
        distance: Int,
        notes: String,
        targetParams: TargetParams?,
        onCreated: (String) -> Unit
    ) {
        val userId = uid() ?: return

        val series = hashMapOf(
            "weapon" to weapon,
            "ammo" to ammo,
            "distance" to distance,
            "notes" to notes,
            "createdAt" to Timestamp.now(),
            // Jeśli targetParams nie jest null, dodajemy go do mapy.
            // Firestore automatycznie przekonwertuje obiekt `TargetParams` na mapę.
            "targetParams" to targetParams
        )

        db.collection("users")
            .document(userId)
            .collection("sessions")
            .document(sessionId)
            .collection("series")
            .add(series)
            .addOnSuccessListener {
                onCreated(it.id)
            }
    }

    fun addShot(
        sessionId: String,
        seriesId: String,
        x: Float,
        y: Float,
        value: Int
    ) {
        val userId = uid() ?: return

        val shot = hashMapOf(
            "x" to x,
            "y" to y,
            "value" to value,
            "timestamp" to Timestamp.now()
        )

        db.collection("users")
            .document(userId)
            .collection("sessions")
            .document(sessionId)
            .collection("series")
            .document(seriesId)
            .collection("shots")
            .add(shot)
    }

    fun getSessions(
        onResult: (List<Session>) -> Unit
    ) {
        val userId = uid() ?: return

        db.collection("users")
            .document(userId)
            .collection("sessions")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val sessions = result.documents.map { doc ->
                    Session(
                        id = doc.id,
                        createdAt = doc.getTimestamp("createdAt"),
                        location = doc.getString("location") ?: "",
                        notes = doc.getString("notes") ?: ""
                    )
                }
                onResult(sessions)
            }
    }
    fun getSeries(
        sessionId: String,
        onResult: (List<Series>) -> Unit
    ) {
        val userId = uid() ?: return

        db.collection("users")
            .document(userId)
            .collection("sessions")
            .document(sessionId)
            .collection("series")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { result ->
                val series = result.documents.map { doc ->
                    val targetParamsMap = doc.get("targetParams") as? Map<*, *>
                    val targetParams = if (targetParamsMap != null) {
                        TargetParams(
                            centerX = (targetParamsMap["centerX"] as? Double)?.toFloat() ?: 0f,
                            centerY = (targetParamsMap["centerY"] as? Double)?.toFloat() ?: 0f,
                            radius = (targetParamsMap["radius"] as? Double)?.toFloat() ?: 0f
                        )
                    } else {
                        null
                    }
                    Series(
                        id = doc.id,
                        weapon = doc.getString("weapon") ?: "",
                        ammo = doc.getString("ammo") ?: "",
                        distance = (doc.getLong("distance") ?: 0L).toInt(),
                        notes = doc.getString("notes") ?: "", // Odczytujemy opis
                        createdAt = doc.getTimestamp("createdAt"),
                        imageUrl = doc.getString("imageUrl"),
                        targetParams = targetParams
                    )
                }
                onResult(series)
            }
    }

    fun updateSeriesImage(
        sessionId: String,
        seriesId: String,
        imageUrl: String
    ) {
        val userId = uid() ?: return

        db.collection("users")
            .document(userId)
            .collection("sessions")
            .document(sessionId)
            .collection("series")
            .document(seriesId)
            .update("imageUrl", imageUrl)
    }

    fun getShots(
        sessionId: String,
        seriesId: String,
        onResult: (List<Shot>) -> Unit
    ) {
        val userId = uid() ?: return

        db.collection("users")
            .document(userId)
            .collection("sessions")
            .document(sessionId)
            .collection("series")
            .document(seriesId)
            .collection("shots")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                val shots = result.documents.map { doc ->
                    Shot(
                        id = doc.id,
                        x = (doc.getDouble("x") ?: 0.0).toFloat(),
                        y = (doc.getDouble("y") ?: 0.0).toFloat(),
                        value = (doc.getLong("value") ?: 0L).toInt(),
                        timestamp = doc.getTimestamp("timestamp")
                    )
                }
                onResult(shots)
            }
    }
    fun uploadSeriesImage(
        sessionId: String,
        seriesId: String,
        file: File,
        onSuccess: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return

        val storageRef = FirebaseStorage.getInstance().reference
            .child("users/$userId/sessions/$sessionId/series/$seriesId/target.jpg")

        storageRef.putFile(file.toUri())
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
    }


}
