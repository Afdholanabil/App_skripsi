package com.example.app_skripsi.data.firebase

import com.example.app_skripsi.data.model.DiaryModel
import com.example.app_skripsi.data.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /** 🔹 Register User dengan menyimpan ke Firestore */
    suspend fun registerUser(email: String, password: String, user: UserModel): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("Gagal mendapatkan userId"))

            // 🔹 Path user dalam Firestore
            val userRef = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)

            // 🔹 Simpan data user ke Firestore
            userRef.set(user.copy(email = email)).await()

            // 🔥 Buat koleksi kosong untuk DeteksiRutin, DeteksiSingkat, dan Diary
            val initialData = hashMapOf("initialized" to true) // Placeholder untuk buat collection

            firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiRutin")
                .document("init").set(initialData).await()

            firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiSingkat")
                .document("init").set(initialData).await()

            firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("Diary")
                .document("init").set(initialData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /** 🔹 Login User */
    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Mengirim Email Reset Password */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /** 🔹 Logout User */
    suspend fun logoutUser() {
        auth.signOut()
    }

    /** 🔹 Dapatkan User ID Saat Ini */
    suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun addDiary(userId: String, diaryData: DiaryModel): Result<Unit> {
        return try {
            val diaryCollection = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("Diary")

            // Generate diaryId dengan format "diary_1", "diary_2", ...
            val diaryCount = diaryCollection.get().await().size()
            val diaryId = "diary_${diaryCount + 1}"

            diaryCollection.document(diaryId).set(diaryData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiaries(userId: String): Result<List<DiaryModel>> {
        return try {
            val diaryCollection = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("Diary")

            val snapshot = diaryCollection.get().await()

            // Filter untuk mengabaikan dokumen "init"
            val diaryList = snapshot.documents
                .filterNot { it.id == "init" }
                .mapNotNull { it.toObject(DiaryModel::class.java) }

            Result.success(diaryList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    /** 🔹 Menambahkan Dokumen ke Firestore (Bisa ID Manual atau Otomatis) */
    suspend fun <T> addDocument(collection: String, documentId: String? = null, data: T): Result<Unit> {
        return try {
            val collectionRef = firestore.collection(collection)

            if (documentId != null) {
                // Jika ID manual diberikan
                collectionRef.document(documentId).set(data!!).await()
            } else {
                // Jika ID otomatis
                collectionRef.add(data!!).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Mengambil Semua Dokumen dari Koleksi Firestore */
    suspend fun <T> getDocuments(collection: String, clazz: Class<T>): Result<List<T>> {
        return try {
            val snapshot = firestore.collection(collection).get().await()
            val list = snapshot.documents.mapNotNull { it.toObject(clazz) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Mengambil Dokumen Spesifik dengan ID */
    suspend fun <T> getDocumentById(collection: String, documentId: String, clazz: Class<T>): Result<T?> {
        return try {
            val snapshot = firestore.collection(collection).document(documentId).get().await()
            val data = snapshot.toObject(clazz)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Menghapus Dokumen di Firestore */
    suspend fun deleteDocument(collection: String, documentId: String): Result<Unit> {
        return try {
            firestore.collection(collection).document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
