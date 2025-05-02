package com.example.app_skripsi.core.knn

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import android.util.Log

/**
 * Enhanced K-Nearest Neighbors classifier for anxiety severity prediction.
 * Features include:
 * - Feature normalization
 * - Weighted distance calculations
 * - Confidence scoring
 * - Threshold-based severity classification
 * - Important factor identification
 */
class KNNClassifier(
    private val trainingData: List<DataPoint>,
    private val k: Int = 5,
    private val weights: List<Double> = listOf(1.5, 1.0, 0.5, 3.0) // Emotion, Activity, Day, GAD Score
) {
    companion object {
        private const val TAG = "KNNClassifier"

        // GAD-7 severity thresholds
        const val MINIMAL_MAX = 4
        const val MILD_MAX = 9
        const val MODERATE_MAX = 14
        // Anything above MODERATE_MAX is considered "parah"
    }

    // Store min and max values for normalization
    private val featureRanges: List<Pair<Double, Double>> = calculateFeatureRanges()

    private fun calculateFeatureRanges(): List<Pair<Double, Double>> {
        // For each feature, find min and max values
        val features = listOf(
            trainingData.map { it.emosi },
            trainingData.map { it.aktivitas },
            trainingData.map { it.hari },
            trainingData.map { it.gadScore }
        )

        return features.map { values ->
            Pair(values.minOrNull()?.toDouble() ?: 0.0, values.maxOrNull()?.toDouble() ?: 1.0)
        }
    }

    /**
     * Predicts anxiety severity using the KNN algorithm with manual score override
     */
    fun predict(emosi: Int, aktivitas: Int, hari: Int, gadScore: Int): String {
        // First predict using KNN algorithm
        val knnPrediction = predictBasedOnNeighbors(emosi, aktivitas, hari, gadScore)

        // Override prediction based on actual GAD score if it's significantly different
        val scoreBasedSeverity = getSeverityFromScore(gadScore)

        // Log both predictions for debugging
        Log.d(TAG, "KNN Prediction: $knnPrediction, Score-based severity: $scoreBasedSeverity for score $gadScore")

        // If GAD score is in "Sedang" or "Parah" range, prioritize it over KNN prediction
        // This ensures high anxiety scores aren't predicted as lower severities
        if ((scoreBasedSeverity == "Sedang" || scoreBasedSeverity == "Parah") &&
            (knnPrediction == "Minimal" || knnPrediction == "Ringan")) {
            Log.d(TAG, "Overriding KNN prediction with score-based severity: $scoreBasedSeverity")
            return scoreBasedSeverity
        }

        // Similarly, if GAD score is very low, make sure we don't predict high anxiety
        if (scoreBasedSeverity == "Minimal" &&
            (knnPrediction == "Sedang" || knnPrediction == "Parah")) {
            Log.d(TAG, "Overriding KNN prediction with score-based severity: $scoreBasedSeverity")
            return scoreBasedSeverity
        }

        return knnPrediction
    }

    /**
     * Get severity level based on GAD-7 score
     */
    private fun getSeverityFromScore(gadScore: Int): String {
        return when {
            gadScore <= MINIMAL_MAX -> "Minimal"
            gadScore <= MILD_MAX -> "Ringan"
            gadScore <= MODERATE_MAX -> "Sedang"
            else -> "Parah"
        }
    }

    /**
     * Make prediction based on k-nearest neighbors
     */
    private fun predictBasedOnNeighbors(emosi: Int, aktivitas: Int, hari: Int, gadScore: Int): String {
        val input = listOf(emosi, aktivitas, hari, gadScore)
        val distances = trainingData.map {
            val dist = euclideanDistance(
                normalizeFeatures(input),
                normalizeFeatures(listOf(it.emosi, it.aktivitas, it.hari, it.gadScore))
            )
            Pair(dist, it.label)
        }.sortedBy { it.first }

        val kNearest = distances.take(k).map { it.second }
        val prediction = kNearest.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "ringan"

        // Normalize label before returning
        return normalizeLabel(prediction)
    }

    /**
     * Normalizes features to a 0-1 range for better comparison
     */
    private fun normalizeFeatures(features: List<Int>): List<Double> {
        return features.mapIndexed { index, value ->
            val (min, max) = featureRanges[index]
            if (max == min) 0.0 else (value - min) / (max - min)
        }
    }

    /**
     * Calculates weighted Euclidean distance between two feature vectors
     */
    private fun euclideanDistance(a: List<Double>, b: List<Double>): Double {
        return sqrt(a.zip(b).zip(weights).sumOf { (pair, weight) ->
            val (ai, bi) = pair
            weight * (ai - bi).pow(2)
        })
    }

    /**
     * Evaluates the model using test data
     */
    fun evaluateModel(testData: List<DataPoint>): Double {
        var correctPredictions = 0

        testData.forEach { point ->
            val prediction = predict(point.emosi, point.aktivitas, point.hari, point.gadScore)
            if (normalizeLabel(point.label) == prediction) {
                correctPredictions++
            }
        }

        return correctPredictions.toDouble() / testData.size
    }

    /**
     * Predicts anxiety severity with confidence score
     */
    fun predictWithConfidence(emosi: Int, aktivitas: Int, hari: Int, gadScore: Int): Pair<String, Double> {
        val input = listOf(emosi, aktivitas, hari, gadScore)
        val distances = trainingData.map {
            val dist = euclideanDistance(
                normalizeFeatures(input),
                normalizeFeatures(listOf(it.emosi, it.aktivitas, it.hari, it.gadScore))
            )
            Pair(dist, it.label)
        }.sortedBy { it.first }

        val kNearest = distances.take(k)
        val labelCounts = kNearest.groupingBy { it.second }.eachCount()
        val rawPrediction = labelCounts.maxByOrNull { it.value }?.key ?: "ringan"

        // Get prediction based on KNN with scoring override
        val finalPrediction = predict(emosi, aktivitas, hari, gadScore)

        // Calculate confidence
        val knnConfidence = labelCounts[rawPrediction]?.toDouble()?.div(k) ?: 0.0

        // Adjust confidence based on how close GAD score is to threshold boundaries
        val scoreBasedSeverity = getSeverityFromScore(gadScore)
        val adjustedConfidence = if (finalPrediction == scoreBasedSeverity) {
            // Boost confidence if KNN and score prediction match
            min(1.0, knnConfidence * 1.2)
        } else {
            // Reduce confidence if they don't match
            max(0.3, knnConfidence * 0.8)
        }

        return Pair(finalPrediction, adjustedConfidence)
    }

    /**
     * Gets detailed prediction information including important factors
     */
    fun getPredictionDetails(emosi: Int, aktivitas: Int, hari: Int, gadScore: Int): Map<String, Any> {
        val input = listOf(emosi, aktivitas, hari, gadScore)
        val distances = trainingData.map {
            val dist = euclideanDistance(
                normalizeFeatures(input),
                normalizeFeatures(listOf(it.emosi, it.aktivitas, it.hari, it.gadScore))
            )
            Pair(dist, it)
        }.sortedBy { it.first }

        val kNearest = distances.take(k)
        val labelCounts = kNearest.groupingBy { it.second.label }.eachCount()
        val rawPrediction = labelCounts.maxByOrNull { it.value }?.key ?: "ringan"

        // Use improved prediction that considers GAD score directly
        val finalPrediction = predict(emosi, aktivitas, hari, gadScore)

        // Log details of k-nearest neighbors for debugging
        Log.d(TAG, "K-Nearest Neighbors for input (e=$emosi, a=$aktivitas, h=$hari, g=$gadScore):")
        kNearest.forEachIndexed { index, (distance, dataPoint) ->
            Log.d(TAG, "$index: Distance=$distance, Label=${dataPoint.label}, " +
                    "e=${dataPoint.emosi}, a=${dataPoint.aktivitas}, h=${dataPoint.hari}, g=${dataPoint.gadScore}")
        }

        // Log label counts for debugging
        Log.d(TAG, "Label counts: $labelCounts")
        Log.d(TAG, "KNN raw prediction: ${normalizeLabel(rawPrediction)}, Final prediction: $finalPrediction")

        // Most influential factors
        val importantFactors = determineImportantFactors(kNearest.map { it.second })

        // Calculate confidence based on label distribution
        val confidence = if (finalPrediction == normalizeLabel(rawPrediction)) {
            labelCounts[rawPrediction]?.toDouble()?.div(k) ?: 0.0
        } else {
            // If we overrode the KNN prediction, adjust confidence
            val scoreBasedConfidence = 0.7 // Base confidence for score-based prediction
            // Further adjust if we're close to a threshold
            when (gadScore) {
                4, 5, 9, 10, 14, 15 -> 0.6 // Near thresholds, slightly less confident
                else -> scoreBasedConfidence
            }
        }

        return mapOf(
            "prediction" to finalPrediction,
            "confidence" to confidence,
            "importantFactors" to importantFactors,
            "originalKnnPrediction" to normalizeLabel(rawPrediction),
            "scoreBasedSeverity" to getSeverityFromScore(gadScore),
            "gadScore" to gadScore
        )
    }

    /**
     * Determines the most important factors in the prediction
     */
    private fun determineImportantFactors(neighbors: List<DataPoint>): Map<String, Any> {
        // Calculate average and mode for each feature
        val emosiCounts = neighbors.groupingBy { it.emosi }.eachCount()
        val aktivityCounts = neighbors.groupingBy { it.aktivitas }.eachCount()
        val hariCounts = neighbors.groupingBy { it.hari }.eachCount()

        val dominantEmosi = emosiCounts.maxByOrNull { it.value }?.key ?: 0
        val dominantAktivitas = aktivityCounts.maxByOrNull { it.value }?.key ?: 0
        val dominantHari = hariCounts.maxByOrNull { it.value }?.key ?: 0

        return mapOf(
            "emosi" to dominantEmosi,
            "aktivitas" to dominantAktivitas,
            "hari" to dominantHari
        )
    }

    /**
     * Normalizes label text to standardized format
     */
    private fun normalizeLabel(label: String): String {
        return when (label.lowercase()) {
            "minimal" -> "Minimal"
            "ringan" -> "Ringan"
            "moderate" -> "Sedang"
            "sedang" -> "Sedang"
            "parah" -> "Parah"
            else -> "Ringan" // Default if unknown
        }
    }
}