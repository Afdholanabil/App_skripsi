package com.example.app_skripsi.data.firebase

import android.util.Log
import com.example.app_skripsi.R
import com.example.app_skripsi.data.model.DailyDetectionData
import com.example.app_skripsi.data.model.DiaryModel
import com.example.app_skripsi.data.model.RoutineDetectionModel
import com.example.app_skripsi.data.model.ShortDetectionModel
import com.example.app_skripsi.data.model.UserModel
import com.example.app_skripsi.data.model.VideoModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FirebaseService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    init {
        android.util.Log.d("FirebaseService", "🔥 FirebaseService Initialized")
    }

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

    suspend fun getUserData(userId: String): Result<UserModel?> {
        return try {
            val snapshot = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .get().await()

            val userData = snapshot.toObject(UserModel::class.java)
            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun addShortDetection(userId: String, detectionData: ShortDetectionModel): Result<Unit> {
        return try {
            val detectionCollection = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiSingkat")

            // Generate detectionId dengan format "deteksi_singkat_1", "deteksi_singkat_2", ...
            val detectionCount = detectionCollection.get().await().size()
            val detectionId = "deteksi_singkat_${detectionCount + 1}"

            detectionCollection.document(detectionId).set(detectionData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getShortDetections(userId: String): Result<List<ShortDetectionModel>> {
        return try {
            val detectionCollection = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiSingkat")

            val snapshot = detectionCollection.get().await()

            // Filter untuk mengabaikan dokumen "init"
            val detectionList = snapshot.documents
                .filterNot { it.id == "init" }
                .mapNotNull { it.toObject(ShortDetectionModel::class.java) }

            Result.success(detectionList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Menambahkan Deteksi Rutin Baru */
    suspend fun addRoutineDetection(userId: String, routineData: RoutineDetectionModel): Result<String> {
        return try {
            val routineCollection = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiRutin")

            // Generate detectionId dengan format "deteksi_rutin_1", "deteksi_rutin_2", ...
            val detectionCount = routineCollection.get().await()
                .documents.filterNot { it.id == "init" }.size
            val detectionId = "deteksi_rutin_${detectionCount + 1}"

            routineCollection.document(detectionId).set(routineData).await()
            Result.success(detectionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Mendapatkan Semua Deteksi Rutin */
    suspend fun getRoutineDetections(userId: String): Result<List<Pair<String, RoutineDetectionModel>>> {
        return try {
            val routineCollection = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiRutin")

            val snapshot = routineCollection.get().await()

            // Filter untuk mengabaikan dokumen "init"
            val detectionList = snapshot.documents
                .filterNot { it.id == "init" }
                .mapNotNull {
                    val model = it.toObject(RoutineDetectionModel::class.java)
                    if (model != null) Pair(it.id, model) else null
                }

            Result.success(detectionList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Mendapatkan Deteksi Rutin yang Aktif */
    suspend fun getActiveRoutineDetection(userId: String): Result<Pair<String, RoutineDetectionModel>?> {
        return try {
            val routineCollection = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiRutin")

            val snapshot = routineCollection.whereEqualTo("aktif", true).get().await()

            // Mengambil deteksi rutin aktif pertama yang ditemukan
            val activeDetection = snapshot.documents
                .filterNot { it.id == "init" }
                .firstOrNull()
                ?.let {
                    val model = it.toObject(RoutineDetectionModel::class.java)
                    if (model != null) Pair(it.id, model) else null
                }

            Result.success(activeDetection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Menambahkan Deteksi Rutin Baru (dengan data hari pertama) */
    suspend fun createRoutineDetectionWithFirstDay(
        userId: String,
        routineData: RoutineDetectionModel
    ): Result<String> {
        return try {
            Log.d("FirebaseService", "Membuat deteksi rutin baru dengan data hari pertama")

            val routineCollection = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiRutin")

            // Generate detectionId dengan format "deteksi_rutin_1", "deteksi_rutin_2", ...
            val detectionCount = routineCollection.get().await()
                .documents.filterNot { it.id == "init" }.size
            val detectionId = "deteksi_rutin_${detectionCount + 1}"

            // Log untuk debugging
            Log.d("FirebaseService", "Menyimpan deteksi rutin dengan ID: $detectionId")
            Log.d("FirebaseService", "Data hari pertama: ${routineData.deteksiHarian["1"]?.emosi}")

            // Simpan dokumen baru
            routineCollection.document(detectionId).set(routineData).await()

            // Verifikasi dokumen tersimpan
            val verifyDoc = routineCollection.document(detectionId).get().await()
            if (verifyDoc.exists()) {
                Log.d("FirebaseService", "Dokumen berhasil dibuat dan terverifikasi")
            } else {
                Log.e("FirebaseService", "Dokumen gagal dibuat")
            }

            Result.success(detectionId)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error membuat deteksi rutin: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** 🔹 Menambahkan Data Harian ke Deteksi Rutin yang Ada */
    suspend fun updateRoutineDetectionDailyData(
        userId: String,
        routineDocId: String,
        dayNumber: Int,
        dailyData: DailyDetectionData
    ): Result<Unit> {
        return try {
            Log.d("FirebaseService", "Mengupdate deteksi rutin $routineDocId untuk hari ke-$dayNumber")

            // Referensi dokumen
            val routineRef = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiRutin")
                .document(routineDocId)

            // Verifikasi dokumen ada sebelum transaksi
            val docCheck = routineRef.get().await()
            if (!docCheck.exists()) {
                Log.e("FirebaseService", "Dokumen $routineDocId tidak ditemukan!")
                return Result.failure(Exception("Dokumen deteksi rutin tidak ditemukan"))
            }

            // Ambil model saat ini untuk debugging
            val currentModel = docCheck.toObject(RoutineDetectionModel::class.java)
            Log.d("FirebaseService", "SEBELUM UPDATE: Jumlah data harian: ${currentModel?.deteksiHarian?.size}, Key: ${currentModel?.deteksiHarian?.keys}")

            // Jalankan transaksi update
            firestore.runTransaction { transaction ->
                // Ambil data terbaru
                val snapshot = transaction.get(routineRef)
                val routineData = snapshot.toObject(RoutineDetectionModel::class.java)
                    ?: throw Exception("Gagal konversi dokumen ke RoutineDetectionModel")

                // Buat Map baru dengan semua data yang ada + data hari ini
                val updatedDailyData = HashMap<String, DailyDetectionData>()
                updatedDailyData.putAll(routineData.deteksiHarian)
                updatedDailyData[dayNumber.toString()] = dailyData

                Log.d("FirebaseService", "Jumlah data saat ini: ${routineData.deteksiHarian.size}")
                Log.d("FirebaseService", "Jumlah data setelah update: ${updatedDailyData.size}")
                Log.d("FirebaseService", "Hari baru: $dayNumber")

                // Update koleksi data harian
                transaction.update(routineRef, "deteksiHarian", updatedDailyData)

                // Update skor tertinggi/terendah jika perlu
                val currentScore = dailyData.totalSkor
                val dayOfWeek = getDayName()

                if (routineData.skorTinggi < currentScore) {
                    transaction.update(routineRef, "skorTinggi", currentScore)
                    transaction.update(routineRef, "hariSkorTinggi", dayOfWeek)
                }

                if (routineData.skorRendah > currentScore || routineData.skorRendah == 0) {
                    transaction.update(routineRef, "skorRendah", currentScore)
                    transaction.update(routineRef, "hariSkorRendah", dayOfWeek)
                }
            }.await()

            // Verifikasi update berhasil
            val verifiedDoc = routineRef.get().await().toObject(RoutineDetectionModel::class.java)
            Log.d("FirebaseService", "SETELAH UPDATE: Jumlah data harian: ${verifiedDoc?.deteksiHarian?.size}, Key: ${verifiedDoc?.deteksiHarian?.keys}")

            // Cek apakah data untuk hari ini ada
            val hasTodayData = verifiedDoc?.deteksiHarian?.containsKey(dayNumber.toString()) ?: false
            Log.d("FirebaseService", "Verifikasi data hari $dayNumber ada: $hasTodayData")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error mengupdate deteksi rutin: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Fungsi helper untuk mendapatkan nama hari ini
    private fun getDayName(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            Calendar.SUNDAY -> "Minggu"
            else -> ""
        }
    }

    /** 🔹 Menambahkan Data Harian ke Deteksi Rutin yang Ada */
    suspend fun addDailyDataToRoutineDetection(
        userId: String,
        routineDocId: String,
        dayNumber: Int,
        dailyData: DailyDetectionData
    ): Result<Unit> {
        return try {
            Log.d("FirebaseService", "Adding daily data to routine $routineDocId for day $dayNumber")

            // Get reference to the routine document
            val routineRef = firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiRutin")
                .document(routineDocId)

            // In FirebaseService.addDailyDataToRoutineDetection
// Add before the transaction:
            val beforeDoc = routineRef.get().await().toObject(RoutineDetectionModel::class.java)
            Log.d("FirebaseService", "BEFORE UPDATE: Daily data size: ${beforeDoc?.deteksiHarian?.size}, Keys: ${beforeDoc?.deteksiHarian?.keys}")

// After the transaction:
            val afterDoc = routineRef.get().await().toObject(RoutineDetectionModel::class.java)
            Log.d("FirebaseService", "AFTER UPDATE: Daily data size: ${afterDoc?.deteksiHarian?.size}, Keys: ${afterDoc?.deteksiHarian?.keys}")

            // Run a transaction to update the document atomically
            firestore.runTransaction { transaction ->
                // Get current document state
                val snapshot = transaction.get(routineRef)
                if (!snapshot.exists()) {
                    throw Exception("Routine document does not exist")
                }

                val routineData = snapshot.toObject(RoutineDetectionModel::class.java)
                    ?: throw Exception("Could not convert document to RoutineDetectionModel")

                // Create updated daily data map
                val updatedDailyData = HashMap<String, DailyDetectionData>()
                updatedDailyData.putAll(routineData.deteksiHarian)
                updatedDailyData[dayNumber.toString()] = dailyData

                // Log data for debugging
                Log.d("FirebaseService", "Current daily data size: ${routineData.deteksiHarian.size}")
                Log.d("FirebaseService", "Updated daily data size: ${updatedDailyData.size}")
                Log.d("FirebaseService", "New day number: $dayNumber")



                // Update fields that need to be updated
                transaction.update(routineRef, "deteksiHarian", updatedDailyData)

                // Update min/max scores if needed
                val currentScore = dailyData.totalSkor
                val dayOfWeek = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Senin"
                    Calendar.TUESDAY -> "Selasa"
                    Calendar.WEDNESDAY -> "Rabu"
                    Calendar.THURSDAY -> "Kamis"
                    Calendar.FRIDAY -> "Jumat"
                    Calendar.SATURDAY -> "Sabtu"
                    Calendar.SUNDAY -> "Minggu"
                    else -> ""
                }

                // Update high score if needed
                if (routineData.skorTinggi < currentScore) {
                    transaction.update(routineRef, "skorTinggi", currentScore)
                    transaction.update(routineRef, "hariSkorTinggi", dayOfWeek)
                }

                // Update low score if needed (only if it's the first entry or lower than current min)
                if (routineData.skorRendah > currentScore || routineData.skorRendah == 0) {
                    transaction.update(routineRef, "skorRendah", currentScore)
                    transaction.update(routineRef, "hariSkorRendah", dayOfWeek)
                }
            }.await()

            // Verify the update was successful
            val updatedDoc = routineRef.get().await().toObject(RoutineDetectionModel::class.java)
            Log.d("FirebaseService", "After update - daily data size: ${updatedDoc?.deteksiHarian?.size ?: 0}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error updating routine document: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** 🔹 Mengubah Status Aktif Deteksi Rutin */
    suspend fun updateRoutineDetectionStatus(userId: String, routineDocId: String, isActive: Boolean): Result<Unit> {
        return try {
            firestore.collection("users_students")
                .document("data")
                .collection("userId")
                .document(userId)
                .collection("DeteksiRutin")
                .document(routineDocId)
                .update("aktif", isActive)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Mendapatkan Semua Video */
    suspend fun getVideos(): Result<List<VideoModel>> {
        return try {
            // Akses dokumen yoga langsung karena tidak ada kategori lain
            val videoDocument = firestore.collection("video_store")
                .document("yoga")
                .get().await()

            val videoList = mutableListOf<VideoModel>()
            val videoData = videoDocument.get("yoga_video") as? Map<*, *>

            if (videoData != null) {
                for ((key, value) in videoData) {
                    if (value is Map<*, *>) {
                        val videoMap = value
                        val title = videoMap["title"] as? String ?: ""
                        val desc = videoMap["desc"] as? String ?: ""
                        val linkVideo = videoMap["link_video"] as? String ?: ""
                        val linkSource = videoMap["link_source"] as? String ?: ""
                        val hasCopyright = videoMap["cr"] as? Boolean ?: false

                        val video = VideoModel(
                            id = "$key",
                            title = title,
                            description = desc,
                            videoUrl = linkVideo,
                            sourceUrl = linkSource,
                            hasCopyright = hasCopyright,
                            thumbnailUrl = R.drawable.video_thumbnail // Gunakan thumbnail default
                        )

                        videoList.add(video)
                    }
                }
            }

            Log.d("FirebaseService", "Loaded ${videoList.size} videos from yoga collection")
            Result.success(videoList)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error getting videos: ${e.message}", e)
            Result.failure(e)
        }
    }



//    /** 🔹 Mendapatkan Video Berdasarkan Kategori */
//    suspend fun getVideosByCategory(category: String): Result<List<VideoModel>> {
//        return try {
//            val videoDocument = firestore.collection("video_store")
//                .document(category)
//                .get().await()
//
//            val videoList = mutableListOf<VideoModel>()
//            val videoData = videoDocument.get("yoga_video") as? Map<*, *>
//
//            if (videoData != null) {
//                for ((key, value) in videoData) {
//                    if (value is Map<*, *>) {
//                        val videoMap = value
//                        val title = videoMap["title"] as? String ?: ""
//                        val desc = videoMap["desc"] as? String ?: ""
//                        val linkVideo = videoMap["link_video"] as? String ?: ""
//                        val linkSource = videoMap["link_source"] as? String ?: ""
//                        val hasCopyright = videoMap["cr"] as? Boolean ?: false
//
//                        val video = VideoModel(
//                            id = "$category-$key",
//                            title = title,
//                            description = desc,
//                            videoUrl = linkVideo,
//                            sourceUrl = linkSource,
//                            hasCopyright = hasCopyright,
//                            category = category
//                        )
//
//                        videoList.add(video)
//                    }
//                }
//            }
//
//            Result.success(videoList)
//        } catch (e: Exception) {
//            Log.e("FirebaseService", "Error getting videos by category: ${e.message}", e)
//            Result.failure(e)
//        }
//    }

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
