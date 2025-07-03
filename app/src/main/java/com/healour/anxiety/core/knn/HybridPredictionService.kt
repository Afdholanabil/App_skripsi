package com.healour.anxiety.core.knn

import android.content.Context
import android.util.Log
import com.healour.anxiety.data.model.DailyDetectionData

class HybridPredictionService(private val context: Context) {

    companion object {
        private const val TAG = "HybridPredictionService"

        // Prediction modes
        const val MODE_GLOBAL = 0    // Use only global training data
        const val MODE_PERSONAL = 1  // Use only personal data
        const val MODE_HYBRID = 2    // Use both global and personal data (default)
    }

    // Mapping for numeric to string conversions
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

    // Reverse mapping (string to numeric)
    private val emosiMapping = mapOf(
        "Senang" to 0, "Sedih" to 1, "Normal" to 2, "Marah" to 3, "Kecewa" to 4
    )

    private val aktivitasMapping = mapOf(
        "Belajar/Bekerja" to 0, "Istirahat" to 1, "Hiburan" to 2,
        "Sosialisasi" to 3, "Olahraga" to 4
    )

    /**
     * Creates a KNN classifier based on the selected mode
     */
    private fun createClassifier(
        detectionHistory: List<DailyDetectionData>,
        mode: Int = MODE_HYBRID
    ): KNNClassifier {
        try {
            when (mode) {
                MODE_GLOBAL -> {
                    // Use only global training data
                    val globalData = KNNUtils.loadTrainingData(context)
                    Log.d(TAG, "Created global classifier with ${globalData.size} training points")
                    return KNNClassifier(globalData)
                }

                MODE_PERSONAL -> {
                    // Use only personal data
                    if (detectionHistory.isEmpty()) {
                        Log.w(TAG, "No personal data available, falling back to minimal dataset")
                        return KNNClassifier(createMinimalTrainingData())
                    }

                    val personalData = convertUserHistoryToDataPoints(detectionHistory)
                    Log.d(TAG, "Created personal classifier with ${personalData.size} training points")
                    return KNNClassifier(personalData, k = minOf(3, personalData.size))
                }

                MODE_HYBRID -> {
                    // Combine global and personal data with personal data weighted more heavily
                    val globalData = KNNUtils.loadTrainingData(context)

                    if (detectionHistory.isEmpty()) {
                        Log.d(TAG, "No personal data available for hybrid mode, using only global data")
                        return KNNClassifier(globalData)
                    }

                    val personalData = convertUserHistoryToDataPoints(detectionHistory)

                    // Duplicate personal data to give it higher weight (3x)
                    val combinedData = globalData + personalData + personalData + personalData
                    Log.d(TAG, "Created hybrid classifier with ${combinedData.size} training points " +
                            "(${globalData.size} global + ${personalData.size} personal)")

                    return KNNClassifier(combinedData)
                }

                else -> {
                    Log.w(TAG, "Invalid mode $mode, using hybrid mode")
                    return createClassifier(detectionHistory, MODE_HYBRID)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating classifier: ${e.message}", e)
            return KNNClassifier(createMinimalTrainingData())
        }
    }

    /**
     * Converts user history data to KNN DataPoint format
     */
    private fun convertUserHistoryToDataPoints(history: List<DailyDetectionData>): List<DataPoint> {
        return history.map { data ->
            DataPoint(
                emosi = emosiMapping[data.emosi] ?: 2,
                aktivitas = aktivitasMapping[data.kegiatan] ?: 1,
                hari = getDayOfWeek(data.tanggal.toDate()),
                gadScore = data.totalSkor,
                label = data.severity.lowercase()
            )
        }
    }

    /**
     * Creates minimal training data as fallback
     */
    private fun createMinimalTrainingData(): List<DataPoint> {
        return listOf(
            DataPoint(emosi = 0, aktivitas = 0, hari = 0, gadScore = 3, label = "minimal"),
            DataPoint(emosi = 0, aktivitas = 0, hari = 1, gadScore = 7, label = "ringan"),
            DataPoint(emosi = 2, aktivitas = 0, hari = 2, gadScore = 12, label = "moderate"),
            DataPoint(emosi = 4, aktivitas = 0, hari = 3, gadScore = 17, label = "parah")
        )
    }

    /**
     * Gets day of week (0-6) from Date
     */
    private fun getDayOfWeek(date: java.util.Date): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        return (calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1) % 7 // 0-6, Sunday-Saturday
    }


    fun predictNextSevenDaysDetail(
        lastDayNumber: Int,
        detectionHistory: List<DailyDetectionData>,
        predictionMode: Int = MODE_HYBRID
    ): List<PredictionDetail> {
        try {
            // Log number of historical data points
            Log.d(TAG, "Predicting with ${detectionHistory.size} historical data points, mode=$predictionMode")

            if (detectionHistory.isEmpty()) {
                Log.w(TAG, "No historical data available for prediction")
                return emptyList()
            }

            // Calculate trend from history
            val gadScoreTrend = calculateGadScoreTrend(detectionHistory)
            Log.d(TAG, "GAD score trend: $gadScoreTrend")

            // Find emotion and activity patterns
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

            // Map to numeric values (using defaults if not found)
            val mostCommonEmosi = emosiMapping[emosiFrequency] ?: 2
            val mostCommonAktivitas = aktivitasMapping[aktivitasFrequency] ?: 1

            // Use last emotion and activity too
            val lastEmosi = emosiMapping[detectionHistory.lastOrNull()?.emosi] ?: 2
            val lastAktivitas = aktivitasMapping[detectionHistory.lastOrNull()?.kegiatan] ?: 1

            Log.d(TAG, "Most common emotion: $emosiFrequency ($mostCommonEmosi)")
            Log.d(TAG, "Most common activity: $aktivitasFrequency ($mostCommonAktivitas)")
            Log.d(TAG, "Last emotion: ${detectionHistory.lastOrNull()?.emosi} ($lastEmosi)")
            Log.d(TAG, "Last activity: ${detectionHistory.lastOrNull()?.kegiatan} ($lastAktivitas)")

            // Create classifier based on the selected mode
            val knnClassifier = createClassifier(detectionHistory, predictionMode)

            // Predict next 7 days with details
            val predictions = mutableListOf<PredictionDetail>()

            for (i in 1..7) {
                val nextDay = lastDayNumber + i
                val dayOfWeek = nextDay % 7 // 0-6, Sunday-Saturday
                val predictedGadScore = calculatePredictedGadScore(detectionHistory, gadScoreTrend, i)

                Log.d(TAG, "Predicting for day $nextDay (dayOfWeek=$dayOfWeek): predicted GAD score = $predictedGadScore")

                // Use both last and most frequent emotion/activity for prediction
                val detailsLastEmosi = knnClassifier.getPredictionDetails(
                    lastEmosi, lastAktivitas, dayOfWeek, predictedGadScore
                )

                val detailsFrequentEmosi = knnClassifier.getPredictionDetails(
                    mostCommonEmosi, mostCommonAktivitas, dayOfWeek, predictedGadScore
                )

                // Choose prediction with higher confidence
                val confidenceLast = detailsLastEmosi["confidence"] as Double
                val confidenceFrequent = detailsFrequentEmosi["confidence"] as Double

                Log.d(TAG, "Last emotion confidence: $confidenceLast, Most common confidence: $confidenceFrequent")

                val finalDetails = if (confidenceLast >= confidenceFrequent) detailsLastEmosi else detailsFrequentEmosi

                // Extract important factors
                val importantFactors = finalDetails["importantFactors"] as Map<String, Any>
                val dominantEmosi = importantFactors["emosi"] as Int
                val dominantAktivitas = importantFactors["aktivitas"] as Int

                val finalPrediction = finalDetails["prediction"] as String
                Log.d(TAG, "Final prediction for day $nextDay: $finalPrediction (confidence: ${finalDetails["confidence"]})")

                // Create detailed prediction
                predictions.add(
                    PredictionDetail(
                        day = nextDay,
                        dayOfWeek = hariStringMapping[dayOfWeek] ?: "Unknown",
                        predictedSeverity = finalPrediction,
                        confidence = finalDetails["confidence"] as Double,
                        predictedGadScore = predictedGadScore,
                        suggestedEmosi = emosiStringMapping[dominantEmosi] ?: "Normal",
                        suggestedAktivitas = aktivitasStringMapping[dominantAktivitas] ?: "Istirahat",
                        predictionMode = getPredictionModeName(predictionMode)
                    )
                )
            }

            return predictions
        } catch (e: Exception) {
            Log.e(TAG, "Error in prediction: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Gets readable name for prediction mode
     */
    private fun getPredictionModeName(mode: Int): String {
        return when (mode) {
            MODE_GLOBAL -> "Data Global"
            MODE_PERSONAL -> "Data Personal"
            MODE_HYBRID -> "Data Hybrid"
            else -> "Data Hybrid"
        }
    }

    /**
     * Data class for detailed prediction results
     */
    data class PredictionDetail(
        val day: Int,
        val dayOfWeek: String,
        val predictedSeverity: String,
        val confidence: Double,
        val predictedGadScore: Int,
        val suggestedEmosi: String,
        val suggestedAktivitas: String,
        val predictionMode: String
    ) {
        /**
         * Gets readable description of prediction
         */
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

        /**
         * Gets recommended activity based on predicted severity
         */
        fun getRecommendedActivity(): String {
            return when (predictedSeverity) {
                "Minimal" -> "Pertahankan aktivitas seperti ${suggestedAktivitas.lowercase()} dan coba aktivitas baru yang menyenangkan."
                "Ringan" -> "Pertimbangkan melakukan aktivitas relaksasi seperti yoga atau meditasi di samping ${suggestedAktivitas.lowercase()}."
                "Sedang" -> "Disarankan untuk menyisihkan waktu istirahat yang cukup dan berbicara dengan teman dekat atau keluarga."
                "Parah" -> "Sangat disarankan untuk berkonsultasi dengan profesional kesehatan mental dan kurangi beban aktivitas."
                else -> "Jaga keseimbangan antara aktivitas dan waktu istirahat Anda."
            }
        }

        /**
         * Gets complete description with recommendations
         */
        fun getCompleteDescription(): String {
            return "${getReadableDescription()} ${getRecommendedActivity()}"
        }
    }

    /**
     * For backward compatibility
     */
    fun predictNextSevenDays(
        lastDayNumber: Int,
        detectionHistory: List<DailyDetectionData>,
        predictionMode: Int = MODE_HYBRID
    ): List<Pair<Int, String>> {
        return predictNextSevenDaysDetail(lastDayNumber, detectionHistory, predictionMode)
            .map { Pair(it.day, it.predictedSeverity) }
    }

    /**
     * Calculates trend in GAD scores from history data
     */
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

    /**
     * Calculates predicted GAD score based on history and trend
     */
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