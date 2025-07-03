package com.healour.anxiety.core.knn

import android.content.Context
import android.util.Log
import com.healour.anxiety.data.model.DailyDetectionData

class PredictionService(private val context: Context) {
    private val knnClassifier: KNNClassifier by lazy {
        try {
            val trainingData = KNNUtils.loadTrainingData(context)
            Log.d("PredictionService", "Loaded ${trainingData.size} training data points")
            KNNClassifier(trainingData)
        } catch (e: Exception) {
            Log.e("PredictionService", "Error loading training data: ${e.message}", e)
            // Fallback to minimal dataset if there's an error
            KNNClassifier(createMinimalTrainingData())
        }
    }

    // Mapping untuk konversi nilai numerik ke string
    private val emosiStringMapping = mapOf(
        0 to "Senang", 1 to "Sedih", 2 to "Normal", 3 to "Marah", 4 to "Kecewa"
    )

    private val aktivitasStringMapping = mapOf(
        0 to "Belajar/Bekerja", 1 to "Istirahat", 2 to "Hiburan",
        3 to "Sosialisasi", 4 to "Olahraga"
    )

    private val hariStringMapping = mapOf(
        0 to "Senin", 1 to "Selasa", 2 to "Rabu",
        3 to "Kamis", 4 to "Jumat", 5 to "Sabtu", 6 to "Minggu"
    )

    // Mapping string ke numerik (versi terbalik)
    private val emosiMapping = mapOf(
        "Senang" to 0, "Sedih" to 1, "Normal" to 2, "Marah" to 3, "Kecewa" to 4
    )

    private val aktivitasMapping = mapOf(
        "Belajar/Bekerja" to 0, "Istirahat" to 1, "Hiburan" to 2,
        "Sosialisasi" to 3, "Olahraga" to 4
    )

    // Minimal training data untuk fallback
    private fun createMinimalTrainingData(): List<DataPoint> {
        return listOf(
            DataPoint(emosi = 0, aktivitas = 0, hari = 0, gadScore = 3, label = "minimal"),
            DataPoint(emosi = 0, aktivitas = 0, hari = 1, gadScore = 7, label = "ringan"),
            DataPoint(emosi = 2, aktivitas = 0, hari = 2, gadScore = 12, label = "moderate"),
            DataPoint(emosi = 4, aktivitas = 0, hari = 3, gadScore = 17, label = "parah")
        )
    }

    fun predictNextSevenDaysDetail(
        lastDayNumber: Int,
        detectionHistory: List<DailyDetectionData>
    ): List<PredictionDetail> {
        try {
            // Log jumlah data historis
            Log.d("PredictionService", "Predicting with ${detectionHistory.size} historical data points")

            if (detectionHistory.isEmpty()) {
                Log.w("PredictionService", "No historical data available for prediction")
                return emptyList()
            }

            // Calculate trend from history
            val gadScoreTrend = calculateGadScoreTrend(detectionHistory)
            Log.d("PredictionService", "GAD score trend: $gadScoreTrend")

            // Mencari pola dan frekuensi emosi dan aktivitas
            val emosiFrequency = detectionHistory
                .groupingBy { it.emosi }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: "Normal"

            val aktivitasFrequency = detectionHistory
                .groupingBy { it.kegiatan }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: "Istirahat"

            // Map ke nilai numerik (menggunakan default values jika tidak ditemukan)
            val mostCommonEmosi = emosiMapping[emosiFrequency] ?: 2
            val mostCommonAktivitas = aktivitasMapping[aktivitasFrequency] ?: 1

            // Menggunakan juga emosi dan aktivitas terakhir
            val lastEmosi = emosiMapping[detectionHistory.lastOrNull()?.emosi] ?: 2
            val lastAktivitas = aktivitasMapping[detectionHistory.lastOrNull()?.kegiatan] ?: 1

            Log.d("PredictionService", "Most common emotion: $emosiFrequency ($mostCommonEmosi)")
            Log.d("PredictionService", "Most common activity: $aktivitasFrequency ($mostCommonAktivitas)")
            Log.d("PredictionService", "Last emotion: ${detectionHistory.lastOrNull()?.emosi} ($lastEmosi)")
            Log.d("PredictionService", "Last activity: ${detectionHistory.lastOrNull()?.kegiatan} ($lastAktivitas)")

            // Predict next 7 days with more details
            val predictions = mutableListOf<PredictionDetail>()

            for (i in 1..7) {
                val nextDay = lastDayNumber + i
                val dayOfWeek = nextDay % 7 // 0-6, Minggu-Sabtu
                val predictedGadScore = calculatePredictedGadScore(detectionHistory, gadScoreTrend, i)

                Log.d("PredictionService", "Predicting for day $nextDay (dayOfWeek=$dayOfWeek): predicted GAD score = $predictedGadScore")

                // Gunakan kombinasi emosi/aktivitas yang berbeda untuk prediksi yang lebih handal
                val detailsLastEmosi = knnClassifier.getPredictionDetails(
                    lastEmosi, lastAktivitas, dayOfWeek, predictedGadScore
                )

                val detailsFrequentEmosi = knnClassifier.getPredictionDetails(
                    mostCommonEmosi, mostCommonAktivitas, dayOfWeek, predictedGadScore
                )

                // Pilih prediksi dengan confidence lebih tinggi
                val confidenceLast = detailsLastEmosi["confidence"] as Double
                val confidenceFrequent = detailsFrequentEmosi["confidence"] as Double

                Log.d("PredictionService", "Last emotion confidence: $confidenceLast, Most common confidence: $confidenceFrequent")

                val finalDetails = if (confidenceLast >= confidenceFrequent) detailsLastEmosi else detailsFrequentEmosi

                // Ekstrak faktor penting
                val importantFactors = finalDetails["importantFactors"] as Map<String, Any>
                val dominantEmosi = importantFactors["emosi"] as Int
                val dominantAktivitas = importantFactors["aktivitas"] as Int

                val finalPrediction = finalDetails["prediction"] as String
                Log.d("PredictionService", "Final prediction for day $nextDay: $finalPrediction (confidence: ${finalDetails["confidence"]})")

                // Buat prediksi detail
                predictions.add(
                    PredictionDetail(
                        day = nextDay,
                        dayOfWeek = hariStringMapping[dayOfWeek] ?: "Unknown",
                        predictedSeverity = finalPrediction,
                        confidence = finalDetails["confidence"] as Double,
                        predictedGadScore = predictedGadScore,
                        suggestedEmosi = emosiStringMapping[dominantEmosi] ?: "Normal",
                        suggestedAktivitas = aktivitasStringMapping[dominantAktivitas] ?: "Istirahat"
                    )
                )
            }

            return predictions
        } catch (e: Exception) {
            Log.e("PredictionService", "Error in prediction: ${e.message}", e)
            return emptyList()
        }
    }

    // Data class untuk detail prediksi
    data class PredictionDetail(
        val day: Int,
        val dayOfWeek: String,
        val predictedSeverity: String,
        val confidence: Double,
        val predictedGadScore: Int,
        val suggestedEmosi: String,
        val suggestedAktivitas: String
    ) {
        // Fungsi untuk menghasilkan prediksi dalam format yang mudah dibaca
        fun getReadableDescription(): String {
            val severityLabel = when (predictedSeverity) {
                "Minimal" -> "tingkat kecemasan minimal"
                "Ringan" -> "tingkat kecemasan ringan"
                "Sedang" -> "tingkat kecemasan sedang"
                "Parah" -> "tingkat kecemasan parah"
                else -> "tingkat kecemasan yang tidak dapat diprediksi"
            }

            return "Pada hari $dayOfWeek, Anda cenderung mengalami $severityLabel " +
                    "ketika melakukan aktivitas $suggestedAktivitas dengan emosi $suggestedEmosi."
        }

        // Fungsi untuk mendapatkan rekomendasi aktivitas
        fun getRecommendedActivity(): String {
            return when (predictedSeverity) {
                "Minimal" -> "Pertahankan aktivitas seperti ${suggestedAktivitas.lowercase()} dan coba aktivitas baru yang menyenangkan."
                "Ringan" -> "Pertimbangkan melakukan aktivitas relaksasi seperti yoga atau meditasi di samping ${suggestedAktivitas.lowercase()}."
                "Sedang" -> "Disarankan untuk menyisihkan waktu istirahat yang cukup dan berbicara dengan teman dekat atau keluarga."
                "Parah" -> "Sangat disarankan untuk berkonsultasi dengan profesional kesehatan mental dan kurangi beban aktivitas."
                else -> "Jaga keseimbangan antara aktivitas dan waktu istirahat Anda."
            }
        }

        // Fungsi untuk prediksi yang lebih lengkap dengan rekomendasi
        fun getCompleteDescription(): String {
            return "${getReadableDescription()} ${getRecommendedActivity()}"
        }
    }

    // Function for backward compatibility
    fun predictNextSevenDays(
        lastDayNumber: Int,
        detectionHistory: List<DailyDetectionData>
    ): List<Pair<Int, String>> {
        return predictNextSevenDaysDetail(lastDayNumber, detectionHistory)
            .map { Pair(it.day, it.predictedSeverity) }
    }

    private fun calculateGadScoreTrend(history: List<DailyDetectionData>): Double {
        if (history.size < 2) return 0.0

        // Simple linear regression for GAD score trend
        val gadScores = history.map { it.totalSkor }
        val x = (0 until gadScores.size).toList()

        val sumX = x.sum().toDouble()
        val sumY = gadScores.sum().toDouble()
        val sumXY = x.zip(gadScores).sumOf { (x, y) -> x * y }.toDouble()
        val sumXX = x.sumOf { it * it }.toDouble()
        val n = x.size

        return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
    }

    private fun calculatePredictedGadScore(
        history: List<DailyDetectionData>,
        trend: Double,
        daysAhead: Int
    ): Int {
        if (history.isEmpty()) return 5 // Default value

        val lastGadScore = history.last().totalSkor
        val predictedScore = lastGadScore + (trend * daysAhead)

        // Keep score within GAD-7 range (0-21)
        return predictedScore.coerceIn(0.0, 21.0).toInt()
    }
}