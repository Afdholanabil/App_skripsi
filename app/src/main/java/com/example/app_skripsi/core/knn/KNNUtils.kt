package com.example.app_skripsi.core.knn

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

/**
 * Utility class for KNN operations, including loading training data
 * and analyzing classification results.
 */
object KNNUtils {
    private const val TAG = "KNNUtils"
    private const val TRAINING_DATA_FILENAME = "TrainingData.json"

    /**
     * Loads training data from a JSON file in assets
     */
    fun loadTrainingData(context: Context): List<DataPoint> {
        try {
            val inputStream = context.assets.open(TRAINING_DATA_FILENAME)
            val json = inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<DataPoint>>() {}.type

            val data = Gson().fromJson<List<DataPoint>>(json, type)
            Log.d(TAG, "Successfully loaded ${data.size} training data points")

            // Validate and log summary of training data
            logTrainingDataSummary(data)

            return data
        } catch (e: IOException) {
            Log.e(TAG, "Error loading training data: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Logs summary of loaded training data for debugging
     */
    private fun logTrainingDataSummary(data: List<DataPoint>) {
        if (data.isEmpty()) {
            Log.w(TAG, "Training data is empty!")
            return
        }

        // Count occurrences of each severity level
        val severityCounts = data.groupingBy { it.label.lowercase() }.eachCount()

        // Get statistics on GAD scores
        val gadScores = data.map { it.gadScore }
        val minScore = gadScores.minOrNull() ?: 0
        val maxScore = gadScores.maxOrNull() ?: 0
        val avgScore = gadScores.average()

        // Log summary
        Log.d(TAG, "Training data summary:")
        Log.d(TAG, "- Total data points: ${data.size}")
        Log.d(TAG, "- Severity distribution: $severityCounts")
        Log.d(TAG, "- GAD scores: min=$minScore, max=$maxScore, avg=$avgScore")

        // Verify proper mapping of severity labels to GAD score ranges
        val incorrectMappings = data.filter {
            !isGadScoreConsistentWithLabel(it.gadScore, it.label)
        }

        if (incorrectMappings.isNotEmpty()) {
            Log.w(TAG, "Found ${incorrectMappings.size} data points with inconsistent severity labels:")
            incorrectMappings.take(5).forEach {
                Log.w(TAG, "  - GAD score: ${it.gadScore}, Label: ${it.label}")
            }
        }
    }

    /**
     * Checks if a GAD score is consistent with the given severity label
     */
    private fun isGadScoreConsistentWithLabel(gadScore: Int, label: String): Boolean {
        return when (label.lowercase()) {
            "minimal" -> gadScore <= 4
            "ringan" -> gadScore in 5..9
            "moderate", "sedang" -> gadScore in 10..14
            "parah" -> gadScore >= 15
            else -> false
        }
    }

    /**
     * Converts a GAD-7 score to the appropriate severity label
     */
    fun getSeverityFromScore(score: Int): String {
        return when {
            score <= 4 -> "Minimal"
            score <= 9 -> "Ringan"
            score <= 14 -> "Sedang"
            else -> "Parah"
        }
    }
}