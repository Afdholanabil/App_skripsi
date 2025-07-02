package com.example.app_skripsi.data.repository

import android.util.Log
import com.example.app_skripsi.data.firebase.FirebaseService
import com.example.app_skripsi.data.local.RoutineSessionManager
import com.example.app_skripsi.data.model.DailyDetectionData
import com.example.app_skripsi.data.model.RoutineDetectionModel
import com.example.app_skripsi.data.model.ShortDetectionModel
import com.google.firebase.Timestamp

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class AnxietyRepository(private val firebaseService: FirebaseService) {

    // Fungsi deteksi singkat yang sudah ada
    suspend fun addShortDetection(
        emotion: String,
        activity: String,
        gadAnswers: List<Int>,
        totalScore: Int
    ): Result<Unit> {
        Log.d("AnxietyRepository", "addShortDetection called with: emotion=$emotion, activity=$activity, totalScore=$totalScore")

        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        Log.d("AnxietyRepository", "User ID: $userId")

        try {
            // Mendapatkan hari dan tanggal saat ini
            val calendar = Calendar.getInstance()
            val dayOfWeek = getDayOfWeek(calendar)
            val currentDate = Date()

            // Menentukan tingkat keparahan berdasarkan total skor
            val severity = getSeverityLevel(totalScore)

            Log.d("AnxietyRepository", "Creating ShortDetectionModel with severity: $severity")

            // Membuat model data untuk deteksi singkat
            val shortDetection = ShortDetectionModel(
                emosi = emotion,
                kegiatan = activity,
                tanggal = Timestamp(currentDate),
                hari = dayOfWeek,
                gad1 = gadAnswers.getOrElse(0) { 0 },
                gad2 = gadAnswers.getOrElse(1) { 0 },
                gad3 = gadAnswers.getOrElse(2) { 0 },
                gad4 = gadAnswers.getOrElse(3) { 0 },
                gad5 = gadAnswers.getOrElse(4) { 0 },
                gad6 = gadAnswers.getOrElse(5) { 0 },
                gad7 = gadAnswers.getOrElse(6) { 0 },
                total_skor = totalScore,
                severity = severity
            )

            Log.d("AnxietyRepository", "Calling firebaseService.addShortDetection")
            val result = firebaseService.addShortDetection(userId, shortDetection)

            if (result.isSuccess) {
                Log.d("AnxietyRepository", "Successfully saved short detection to Firestore")
            } else {
                Log.e("AnxietyRepository", "Failed to save to Firestore: ${result.exceptionOrNull()?.message}")
            }

            return result

        } catch (e: Exception) {
            Log.e("AnxietyRepository", "Exception in addShortDetection: ${e.message}", e)
            return Result.failure(e)
        }
    }

    suspend fun getShortDetections(): Result<List<ShortDetectionModel>> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        return firebaseService.getShortDetections(userId)
    }

    /**
     * Membuat dokumen deteksi rutin baru dengan data hari pertama
     */
    suspend fun createNewRoutineWithFirstDay(
        sessionType: String,
        emotion: String,
        activity: String,
        gadAnswers: List<Int>,
        totalScore: Int
    ): Result<String> {
        val userId = firebaseService.getCurrentUserId() ?:
        return Result.failure(Exception("User tidak login"))

        try {
            Log.d("AnxietyRepository", "Membuat deteksi rutin baru dengan data hari pertama")

            // Setup tanggal
            val startDate = Calendar.getInstance()
            val endDate = Calendar.getInstance()
            val currentDate = Date()
            val dayOfWeek = getDayOfWeek(Calendar.getInstance())

            // Set tanggal akhir
            when(sessionType) {
                "1_WEEK" -> endDate.add(Calendar.DATE, 7)
                "2_WEEKS" -> endDate.add(Calendar.DATE, 14)
                "1_MONTH" -> endDate.add(Calendar.DATE, 30)
                else -> endDate.add(Calendar.DATE, 7) // Default 1 minggu
            }

            // Buat data hari pertama
            val dailyData = DailyDetectionData(
                emosi = emotion,
                kegiatan = activity,
                gad1 = gadAnswers.getOrElse(0) { 0 },
                gad2 = gadAnswers.getOrElse(1) { 0 },
                gad3 = gadAnswers.getOrElse(2) { 0 },
                gad4 = gadAnswers.getOrElse(3) { 0 },
                gad5 = gadAnswers.getOrElse(4) { 0 },
                gad6 = gadAnswers.getOrElse(5) { 0 },
                gad7 = gadAnswers.getOrElse(6) { 0 },
                tanggal = Timestamp(currentDate),
                totalSkor = totalScore,
                severity = getSeverityLevel(totalScore)
            )

            // Buat model deteksi rutin dengan data hari pertama
            val routineDetection = RoutineDetectionModel(
                aktif = true,
                deteksiHarian = mapOf("1" to dailyData),
                hariSkorRendah = dayOfWeek,
                hariSkorTinggi = dayOfWeek,
                skorRendah = totalScore,
                skorTinggi = totalScore,
                periode = sessionType,
                tanggalMulai = Timestamp(startDate.time),
                tanggalSelesai = Timestamp(endDate.time)
            )

            // Simpan ke Firestore dengan method khusus
            val result = firebaseService.createRoutineDetectionWithFirstDay(userId, routineDetection)

            if (result.isSuccess) {
                Log.d("AnxietyRepository", "Berhasil membuat deteksi rutin dengan ID: ${result.getOrNull()}")
            } else {
                Log.e("AnxietyRepository", "Gagal membuat deteksi rutin: ${result.exceptionOrNull()?.message}")
            }

            return result
        } catch (e: Exception) {
            Log.e("AnxietyRepository", "Error saat membuat deteksi rutin: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * Menambahkan data hari ke-N ke deteksi rutin yang sudah ada
     */
    suspend fun addDailyDataToExistingRoutine(
        routineDocId: String,
        emotion: String,
        activity: String,
        gadAnswers: List<Int>,
        totalScore: Int,
        routineSessionManager: RoutineSessionManager
    ): Result<Unit> {
        val userId = firebaseService.getCurrentUserId() ?:
        return Result.failure(Exception("User tidak login"))

        try {
            // Dapatkan hari saat ini dalam sesi
            val currentDayResult = getCurrentDayInRoutineSession(routineDocId)
            if (currentDayResult.isFailure) {
                return Result.failure(currentDayResult.exceptionOrNull()!!)
            }

            val currentDay = currentDayResult.getOrNull() ?:
            return Result.failure(Exception("Gagal mendapatkan hari saat ini"))

            Log.d("AnxietyRepository", "Menambahkan data untuk hari ke-$currentDay di deteksi rutin $routineDocId")

            // Buat data harian
            val dailyData = DailyDetectionData(
                emosi = emotion,
                kegiatan = activity,
                gad1 = gadAnswers.getOrElse(0) { 0 },
                gad2 = gadAnswers.getOrElse(1) { 0 },
                gad3 = gadAnswers.getOrElse(2) { 0 },
                gad4 = gadAnswers.getOrElse(3) { 0 },
                gad5 = gadAnswers.getOrElse(4) { 0 },
                gad6 = gadAnswers.getOrElse(5) { 0 },
                gad7 = gadAnswers.getOrElse(6) { 0 },
                tanggal = Timestamp(Date()),
                totalSkor = totalScore,
                severity = getSeverityLevel(totalScore)
            )

            // Update dengan method khusus
            val result = firebaseService.updateRoutineDetectionDailyData(
                userId, routineDocId, currentDay, dailyData
            )

            // Jika berhasil, tandai selesai di local storage
            if (result.isSuccess) {
                routineSessionManager.saveFormCompletionForToday()

                // Verifikasi status completion
                val completionStatus = routineSessionManager.hasCompletedFormToday()
                Log.d("AnxietyRepository", "Data hari ke-$currentDay berhasil disimpan. Status completion: $completionStatus")

                // Verifikasi data tersimpan di Firestore
                val verifyResult = firebaseService.getRoutineDetections(userId)
                if (verifyResult.isSuccess) {
                    val routineData = verifyResult.getOrNull()?.find { it.first == routineDocId }?.second
                    if (routineData != null) {
                        val hasDayData = routineData.deteksiHarian.containsKey(currentDay.toString())
                        Log.d("AnxietyRepository", "Verifikasi di Firestore - data hari $currentDay ada: $hasDayData")
                    }
                }
            } else {
                Log.e("AnxietyRepository", "Gagal menyimpan data hari ke-$currentDay: ${result.exceptionOrNull()?.message}")
            }

            return result
        } catch (e: Exception) {
            Log.e("AnxietyRepository", "Error menambahkan data harian: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * Mendapatkan hari saat ini dalam sesi dengan perhitungan yang lebih akurat
     */
    suspend fun getCurrentDayInRoutineSession(routineDocId: String): Result<Int> {
        val userId = firebaseService.getCurrentUserId() ?:
        return Result.failure(Exception("User tidak login"))

        try {
            val routineResult = firebaseService.getRoutineDetections(userId)
            if (routineResult.isFailure)
                return Result.failure(routineResult.exceptionOrNull()!!)

            val routineList = routineResult.getOrNull()
            val routineData = routineList?.find { it.first == routineDocId }?.second
                ?: return Result.failure(Exception("Deteksi rutin tidak ditemukan"))

            // Ambil tanggal mulai dari Firestore
            val startDate = routineData.tanggalMulai.toDate()
            val currentDate = Date()

            // Log untuk debugging
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            Log.d("AnxietyRepository", "Tanggal mulai sesi: ${dateFormat.format(startDate)}")
            Log.d("AnxietyRepository", "Tanggal hari ini: ${dateFormat.format(currentDate)}")

            // Hitung selisih hari dengan reset komponen waktu
            val startCalendar = Calendar.getInstance()
            startCalendar.time = startDate
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            startCalendar.set(Calendar.MILLISECOND, 0)

            val currentCalendar = Calendar.getInstance()
            currentCalendar.time = currentDate
            currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
            currentCalendar.set(Calendar.MINUTE, 0)
            currentCalendar.set(Calendar.SECOND, 0)
            currentCalendar.set(Calendar.MILLISECOND, 0)

            val diffInMillis = currentCalendar.timeInMillis - startCalendar.timeInMillis
            val diffInDays = (diffInMillis / (24 * 60 * 60 * 1000)).toInt()

            // Hari dalam sesi dimulai dari 1
            val dayInSession = diffInDays + 1
            Log.d("AnxietyRepository", "Hari dalam sesi: $dayInSession")

            return Result.success(dayInSession)
        } catch (e: Exception) {
            Log.e("AnxietyRepository", "Error menghitung hari saat ini: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * Membuat sesi deteksi rutin baru
     */
    suspend fun createRoutineDetection(sessionType: String): Result<String> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        try {
            val startDate = Calendar.getInstance()
            val endDate = Calendar.getInstance()

            // Menentukan tanggal berakhir berdasarkan tipe sesi
            when(sessionType) {
                "1_WEEK" -> endDate.add(Calendar.DATE, 7)
                "2_WEEKS" -> endDate.add(Calendar.DATE, 14)
                "1_MONTH" -> endDate.add(Calendar.DATE, 30)
                else -> endDate.add(Calendar.DATE, 7) // Default 1 minggu
            }

            // Membuat model deteksi rutin baru
            val routineDetection = RoutineDetectionModel(
                aktif = true,
                deteksiHarian = mapOf(), // Kosong di awal
                hariSkorRendah = "",
                hariSkorTinggi = "",
                skorRendah = 0,
                skorTinggi = 0,
                periode = sessionType,
                tanggalMulai = Timestamp(startDate.time),
                tanggalSelesai = Timestamp(endDate.time)
            )

            return firebaseService.addRoutineDetection(userId, routineDetection)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Menambahkan data harian ke deteksi rutin yang sedang aktif
     */
    suspend fun addDailyRoutineDetection(
        routineDocId: String,
        dayNumber: Int,
        emotion: String,
        activity: String,
        gadAnswers: List<Int>,
        totalScore: Int,
        routineSessionManager: RoutineSessionManager // Add this parameter
    ): Result<Unit> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        try {
            // Create the daily data
            val dailyData = DailyDetectionData(
                emosi = emotion,
                kegiatan = activity,
                gad1 = gadAnswers.getOrElse(0) { 0 },
                gad2 = gadAnswers.getOrElse(1) { 0 },
                gad3 = gadAnswers.getOrElse(2) { 0 },
                gad4 = gadAnswers.getOrElse(3) { 0 },
                gad5 = gadAnswers.getOrElse(4) { 0 },
                gad6 = gadAnswers.getOrElse(5) { 0 },
                gad7 = gadAnswers.getOrElse(6) { 0 },
                tanggal = Timestamp(Date()),
                totalSkor = totalScore,
                severity = getSeverityLevel(totalScore)
            )

            // Update Firestore document
            val result = firebaseService.addDailyDataToRoutineDetection(
                userId,
                routineDocId,
                dayNumber,
                dailyData
            )

            // If successful, mark this day as completed in the local session manager
            if (result.isSuccess) {
                routineSessionManager.saveFormCompletionForToday()
                Log.d("AnxietyRepository", "Successfully saved routine data for day $dayNumber")

                // Verify the save was actually successful by checking the data
                val verifyResult = firebaseService.getRoutineDetections(userId)
                if (verifyResult.isSuccess) {
                    val routineData = verifyResult.getOrNull()
                        ?.find { it.first == routineDocId }?.second

                    if (routineData != null) {
                        val hasDayData = routineData.deteksiHarian.containsKey(dayNumber.toString())
                        Log.d("AnxietyRepository", "Verification - data for day $dayNumber exists: $hasDayData")
                    }
                }
            }

            return result
        } catch (e: Exception) {
            Log.e("AnxietyRepository", "Error adding daily routine data", e)
            return Result.failure(e)
        }
    }

    /**
     * Membuat sesi deteksi rutin baru sekaligus dengan data hari pertama
     */
    suspend fun createRoutineDetectionWithFirstDayData(
        sessionType: String,
        emotion: String,
        activity: String,
        gadAnswers: List<Int>,
        totalScore: Int
    ): Result<String> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        try {
            // Setup waktu dan tanggal
            val startDate = Calendar.getInstance()
            val endDate = Calendar.getInstance()
            val currentDate = Date()
            val dayOfWeek = getDayOfWeek(Calendar.getInstance())

            // Menentukan tanggal berakhir berdasarkan tipe sesi
            when(sessionType) {
                "1_WEEK" -> endDate.add(Calendar.DATE, 7)
                "2_WEEKS" -> endDate.add(Calendar.DATE, 14)
                "1_MONTH" -> endDate.add(Calendar.DATE, 30)
                else -> endDate.add(Calendar.DATE, 7) // Default 1 minggu
            }

            // Menentukan tingkat keparahan
            val severity = getSeverityLevel(totalScore)

            // Data untuk hari pertama
            val dailyData = DailyDetectionData(
                emosi = emotion,
                kegiatan = activity,
                gad1 = gadAnswers.getOrElse(0) { 0 },
                gad2 = gadAnswers.getOrElse(1) { 0 },
                gad3 = gadAnswers.getOrElse(2) { 0 },
                gad4 = gadAnswers.getOrElse(3) { 0 },
                gad5 = gadAnswers.getOrElse(4) { 0 },
                gad6 = gadAnswers.getOrElse(5) { 0 },
                gad7 = gadAnswers.getOrElse(6) { 0 },
                tanggal = Timestamp(currentDate),
                totalSkor = totalScore,
                severity = severity
            )

            // Map untuk data harian dengan hari 1
            val dailyDataMap = mapOf("1" to dailyData)

            // Membuat model deteksi rutin dengan data hari pertama
            val routineDetection = RoutineDetectionModel(
                aktif = true,
                deteksiHarian = dailyDataMap,
                hariSkorRendah = dayOfWeek,
                hariSkorTinggi = dayOfWeek,
                skorRendah = totalScore,
                skorTinggi = totalScore,
                periode = sessionType,
                tanggalMulai = Timestamp(startDate.time),
                tanggalSelesai = Timestamp(endDate.time)
            )

            Log.d("AnxietyRepository", "Creating routine detection with first day data")
            return firebaseService.addRoutineDetection(userId, routineDetection)
        } catch (e: Exception) {
            Log.e("AnxietyRepository", "Error creating routine with first day data", e)
            return Result.failure(e)
        }
    }

    /**
     * Mendapatkan deteksi rutin yang aktif saat ini
     */
    suspend fun getActiveRoutineDetection(): Result<Pair<String, RoutineDetectionModel>?> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        return firebaseService.getActiveRoutineDetection(userId)
    }

    /**
     * Mengakhiri sesi deteksi rutin yang aktif
     */
    suspend fun endRoutineDetection(routineDocId: String): Result<Unit> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        return firebaseService.updateRoutineDetectionStatus(userId, routineDocId, false)
    }

    /**
     * Memeriksa apakah deteksi rutin masih valid berdasarkan tanggal berakhir
     */
    suspend fun isRoutineDetectionStillValid(routineDocId: String): Result<Boolean> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        try {
            val routineResult = firebaseService.getRoutineDetections(userId)
            if (routineResult.isFailure) return Result.failure(routineResult.exceptionOrNull()!!)

            val routineList = routineResult.getOrNull()
            val routineData = routineList?.find { it.first == routineDocId }?.second
                ?: return Result.success(false)

            if (!routineData.aktif) return Result.success(false)

            val endDate = routineData.tanggalSelesai.toDate()
            val currentDate = Date()

            return Result.success(currentDate.before(endDate))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

//    /**
//     * Mendapatkan hari saat ini dalam sesi deteksi rutin (dimulai dari 1)
//     */
//    suspend fun getCurrentDayInRoutineSession(routineDocId: String): Result<Int> {
//        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
//
//        try {
//            val routineResult = firebaseService.getRoutineDetections(userId)
//            if (routineResult.isFailure) return Result.failure(routineResult.exceptionOrNull()!!)
//
//            val routineList = routineResult.getOrNull()
//            val routineData = routineList?.find { it.first == routineDocId }?.second
//                ?: return Result.failure(Exception("Deteksi rutin tidak ditemukan"))
//
//            val startDate = routineData.tanggalMulai.toDate()
//            val currentDate = Date()
//
//            // Menghitung selisih hari
//            val diffInMillis = currentDate.time - startDate.time
//            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
//
//            // Hari dalam sesi dimulai dari 1
//            return Result.success(diffInDays + 1)
//        } catch (e: Exception) {
//            return Result.failure(e)
//        }
//    }

    /**
     * Mendapatkan durasi total sesi deteksi rutin dalam hari
     */
    suspend fun getRoutineSessionDuration(routineDocId: String): Result<Int> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        try {
            val routineResult = firebaseService.getRoutineDetections(userId)
            if (routineResult.isFailure) return Result.failure(routineResult.exceptionOrNull()!!)

            val routineList = routineResult.getOrNull()
            val routineData = routineList?.find { it.first == routineDocId }?.second
                ?: return Result.failure(Exception("Deteksi rutin tidak ditemukan"))

            val startDate = routineData.tanggalMulai.toDate()
            val endDate = routineData.tanggalSelesai.toDate()

            // Menghitung selisih hari
            val diffInMillis = endDate.time - startDate.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

            return Result.success(diffInDays + 1)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Memeriksa apakah pengguna sudah melakukan deteksi hari ini dalam sesi rutin
     */
    suspend fun hasCompletedRoutineDetectionToday(routineDocId: String): Result<Boolean> {
        val userId = firebaseService.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        try {
            val routineResult = firebaseService.getRoutineDetections(userId)
            if (routineResult.isFailure) return Result.failure(routineResult.exceptionOrNull()!!)

            val routineList = routineResult.getOrNull()
            val routineData = routineList?.find { it.first == routineDocId }?.second
                ?: return Result.failure(Exception("Deteksi rutin tidak ditemukan"))

            // Mendapatkan hari saat ini dalam sesi
            val currentDayResult = getCurrentDayInRoutineSession(routineDocId)
            if (currentDayResult.isFailure) return Result.failure(currentDayResult.exceptionOrNull()!!)

            val currentDay = currentDayResult.getOrNull()!!

            // Cek apakah ada data untuk hari ini
            val dailyData = routineData.deteksiHarian[currentDay.toString()]

            return Result.success(dailyData != null)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // Fungsi helper untuk mendapatkan hari dalam seminggu
    private fun getDayOfWeek(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
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

    // Fungsi helper untuk menentukan tingkat keparahan
    private fun getSeverityLevel(totalScore: Int): String {
        return when {
            totalScore in 0..4 -> "Minimal"
            totalScore in 5..9 -> "Ringan"
            totalScore in 10..14 -> "Sedang"
            totalScore >= 15 -> "Parah"
            else -> "Tidak diketahui"
        }
    }


}