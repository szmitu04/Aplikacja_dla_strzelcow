package com.example.aplikacja_dla_strzelcow.data

import android.util.Log
import androidx.compose.animation.core.copy
import androidx.compose.foundation.gestures.forEach
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.unit.size
import androidx.core.net.toUri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.Date
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

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
            .set(data, SetOptions.merge())
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
        val seriesWithUser = series.copy(userId = userId, sessionId = sessionId)

        // 1. Zapisujemy dokument serii
        db.collection("users").document(userId)
            .collection("sessions").document(sessionId)
            .collection("series")
            .add(seriesWithUser) // Firestore ignoruje pole `id` przy zapisie
            .addOnSuccessListener { seriesDocRef ->
                val seriesId = seriesDocRef.id
                onCreated(seriesId) // Zwracamy ID utworzonej serii

                // 2. W pętli zapisujemy wszystkie strzały do podkolekcji
                val batch = db.batch()
                shots.forEach { shot ->
                    val shotDocRef = seriesDocRef.collection("shots").document()
                    batch.set(shotDocRef, shot.copy(userId = userId))
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
        val userId = uid() //?: return
        if (userId == null) {
            Log.w("FirestoreRepo", "getSessions: Błąd - użytkownik jest niezalogowany. Zwracam pustą listę.")
            onResult(emptyList()) // Zawsze zwróć pustą listę, jeśli nie ma usera
            return
        }
        db.collection("users").document(userId)
            .collection("sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                // Używamy .toObjects, które jest czystsze i obsługuje domyślne wartości
                val sessions = result.toObjects(Session::class.java)
                    .mapIndexed { index, session ->
                        // Ręcznie dodajemy tylko ID dokumentu
                        session.copy(id = result.documents[index].id)
                    }
                onResult(sessions)
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreRepo", "getSessions: Błąd przy pobieraniu sesji.", exception)
                onResult(emptyList()) // Zawsze zwróć pustą listę przy błędzie
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

    fun getEquipmentLists(onResult: (List<String>, List<String>) -> Unit) {
        val userId = uid() ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val weapons = document.get("weapons") as? List<String> ?: emptyList()
                val ammo = document.get("ammo") as? List<String> ?: emptyList()
                onResult(weapons, ammo)
            }
    }
    fun saveWeaponsList(weapons: List<String>) {
        val userId = uid() ?: return
        db.collection("users").document(userId)
            .update("weapons", weapons)
    }

    fun saveAmmoList(ammo: List<String>) {
        val userId = uid() ?: return
        db.collection("users").document(userId)
            .update("ammo", ammo)
    }


    // NOWA FUNKCJA: Pobiera wszystkie serie z danego treningu RAZEM z ich strzałami
    fun getSeriesWithShots(sessionId: String, onResult: (List<Pair<Series, List<Shot>>>) -> Unit) {
        val userId = uid() ?: return

        db.collection("users").document(userId)
            .collection("sessions").document(sessionId)
            .collection("series").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { seriesSnapshot ->
                if (seriesSnapshot.isEmpty) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                val seriesWithShots = mutableListOf<Pair<Series, List<Shot>>>()
                var seriesCounter = seriesSnapshot.size()

                seriesSnapshot.documents.forEach { seriesDoc ->
                    val series = seriesDoc.toObject(Series::class.java)?.copy(
                        id = seriesDoc.id,
                        sessionId = sessionId // Używamy `sessionId` przekazanego do funkcji!
                    ) ?: Series()

                    // Dla każdej serii pobieramy jej podkolekcję strzałów
                    seriesDoc.reference.collection("shots").orderBy("timestamp").get()
                        .addOnSuccessListener { shotsSnapshot ->
                            val shots = shotsSnapshot.toObjects(Shot::class.java)
                            seriesWithShots.add(Pair(series, shots))

                            // Gdy pobierzemy dane dla ostatniej serii, zwracamy wynik
                            if (--seriesCounter == 0) {
                                onResult(seriesWithShots)
                            }
                        }
                }
            }
    }

    // NOWA FUNKCJA: Pobiera jedną, konkretną serię wraz z jej strzałami
    fun getSingleSeriesWithShots(sessionId: String, seriesId: String, onResult: (Series?, List<Shot>) -> Unit) {
        val userId = uid() ?: return
        val seriesDocRef = db.collection("users").document(userId)
            .collection("sessions").document(sessionId)
            .collection("series").document(seriesId)

        seriesDocRef.get().addOnSuccessListener { seriesDoc ->
            val series = seriesDoc.toObject(Series::class.java)?.copy(id = seriesDoc.id)

            seriesDocRef.collection("shots").orderBy("timestamp").get().addOnSuccessListener { shotsSnapshot ->
                val shots = shotsSnapshot.toObjects(Shot::class.java)
                onResult(series, shots)
            }
        }
    }

    fun getShotsSince(startDate: Date, onResult: (List<Shot>) -> Unit) {
        val userId = uid() ?: return
        val startTimestamp = Timestamp(startDate)
        db.collectionGroup("shots") // Przeszukuje wszystkie podkolekcje o nazwie "shots"
            .whereEqualTo("userId", userId) // Filtruje strzały tylko dla zalogowanego użytkownika
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp) // Filtruje po dacie
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val shots = result.toObjects(Shot::class.java)
                onResult(shots)
            }
    }
/*
    // 🔴 NOWA, ZAAWANSOWANA FUNKCJA DO FILTROWANIA 🔴
    fun getFilteredShots(
        // Filtr czasu LUB treningu
        startDate: Date? = null,
        sessionId: String? = null,
        // Filtry opcjonalne
        weapon: String? = null,
        ammo: String? = null,
        distance: Int? = null,
        onResult: (List<Pair<Series, List<Shot>>>) -> Unit
    ) {
        val userId = uid() ?: return

        // Zaczynamy od zapytania o SERIE
        var seriesQuery: Query = db.collectionGroup("series")
            .whereEqualTo("userId", userId) // WAŻNE: Potrzebujemy userId w seriach!

        // Aplikujemy filtry
        if (startDate != null) {
            seriesQuery = seriesQuery.whereGreaterThanOrEqualTo("createdAt", Timestamp(startDate))
        }
        if (sessionId != null) {
            // To jest bardziej skomplikowane, bo seria jest w podkolekcji.
            // Na razie upraszczamy, zakładając, że ID treningu jest gdzieś dostępne.
            // W praktyce, trzeba by najpierw pobrać serie dla danego treningu.
            // Zostawmy to na razie, skupiając się na filtrach czasu.
        }
        if (weapon != null && weapon != "Wszystkie") {
            seriesQuery = seriesQuery.whereEqualTo("weapon", weapon)
        }
        if (ammo != null && ammo != "Wszystkie") {
            seriesQuery = seriesQuery.whereEqualTo("ammo", ammo)
        }
        if (distance != null) {
            // Można dodać filtrowanie po dystansie, np. w zakresie
        }

        seriesQuery.orderBy("createdAt", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { seriesSnapshot ->
                if (seriesSnapshot.isEmpty) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }


                val seriesWithShots = mutableListOf<Pair<Series, List<Shot>>>()
                val documents = seriesSnapshot.documents
                var processedCount = 0

                documents.forEach { seriesDoc ->
                    seriesDoc.reference.collection("shots").get()
                        .addOnCompleteListener { task ->
                            val series = seriesDoc.toObject(Series::class.java)?.copy(id = seriesDoc.id) ?: Series()

                            if (task.isSuccessful) {
                                // Tutaj używamy toObjects(), które rozumie adnotację @PropertyName
                                val shots = task.result.toObjects(Shot::class.java)
                                seriesWithShots.add(Pair(series, shots))
                            } else {
                                // Nawet przy błędzie, dodajemy serię z pustą listą strzałów
                                seriesWithShots.add(Pair(series, emptyList()))
                            }

                            processedCount++
                            if (processedCount == documents.size) {
                                // Zwracamy wynik dopiero, gdy wszystkie podzapytania się zakończą
                                onResult(seriesWithShots)
                            }
                        }
                }
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }


    fun getAllSeriesWithShots(onResult: (List<Pair<Series, List<Shot>>>) -> Unit) {
        val userId = uid() ?: return

        // 1. Pobierz wszystkie serie dla użytkownika
        db.collectionGroup("series")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { seriesSnapshot ->
                if (seriesSnapshot.isEmpty) {
                    Log.d("FirestoreDebug", "Nie znaleziono ŻADNYCH serii dla użytkownika.")
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                Log.d("FirestoreDebug", "Znaleziono ${seriesSnapshot.size()} serii. Pobieranie strzałów...")
                val seriesWithShots = mutableListOf<Pair<Series, List<Shot>>>()
                val documents = seriesSnapshot.documents
                var processedCount = 0

                documents.forEach { seriesDoc ->
                    // 2. Dla każdej serii pobierz jej strzały
                    seriesDoc.reference.collection("shots").get()
                        .addOnCompleteListener { task ->
                            val series = seriesDoc.toObject(Series::class.java)?.copy(id = seriesDoc.id) ?: Series()

                            if (task.isSuccessful) {
                                val shots = task.result.toObjects(Shot::class.java)
                                seriesWithShots.add(Pair(series, shots))
                                Log.d("FirestoreDebug", "   - Seria '${series.id}': pobrano ${shots.size} strzałów.")
                            } else {
                                seriesWithShots.add(Pair(series, emptyList()))
                                Log.w("FirestoreDebug", "   - BŁĄD przy pobieraniu strzałów dla serii '${series.id}'.")
                            }

                            processedCount++
                            if (processedCount == documents.size) {
                                // 3. Zwróć kompletne dane dopiero, gdy wszystko zostanie przetworzone
                                Log.d("FirestoreDebug", "Zakończono pobieranie. Zwracam ${seriesWithShots.size} serii ze strzałami.")
                                onResult(seriesWithShots)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreDebug", "BŁĄD KRYTYCZNY przy pobieraniu serii: ", exception)
                onResult(emptyList())
            }
    }
*/
fun getAllTrainingsWithSeriesAndShots(onResult: (List<Pair<Series, List<Shot>>>) -> Unit) {
    val userId = uid() ?: return

    // 1. Pobierz wszystkie treningi (sessions)
    getSessions { sessions ->
        if (sessions.isEmpty()) {
            Log.d("FirestoreDebug", "Krok 1: Nie znaleziono żadnych treningów (sessions).")
            onResult(emptyList())
            return@getSessions
        }
        Log.d("FirestoreDebug", "Krok 1: Znaleziono ${sessions.size} treningów. Pobieranie serii...")

        val allSeriesWithShots = mutableListOf<Pair<Series, List<Shot>>>()
        var sessionsCounter = sessions.size

        // 2. Dla każdego treningu, pobierz jego serie i strzały
        sessions.forEach { session ->
            getSeriesWithShots(session.id) { seriesWithShots ->
                allSeriesWithShots.addAll(seriesWithShots)
                sessionsCounter--

                // 3. Gdy przetworzymy ostatni trening, zwróć wszystkie zebrane dane
                if (sessionsCounter == 0) {
                    Log.d("FirestoreDebug", "Krok 2: Zakończono. Łącznie zebrano ${allSeriesWithShots.size} serii ze strzałami.")
                    onResult(allSeriesWithShots)
                }
            }
        }
    }
}
}
