package com.alarmy.lumirise.mission

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.alarmy.lumirise.util.ImageHashUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class PhotoMatcher(private val context: Context) {

    companion object {
        private const val TARGET_PHOTO_FILENAME = "target_photo.jpg"
        private const val TARGET_HASH_KEY = "target_photo_hash"
        private const val SIMILARITY_THRESHOLD = 70f
    }

    private val prefs = context.getSharedPreferences("photo_mission_prefs", Context.MODE_PRIVATE)

    fun hasTargetPhoto(): Boolean {
        val targetFile = getTargetPhotoFile()
        return targetFile.exists() && targetFile.length() > 0
    }

    fun saveTargetPhoto(bitmap: Bitmap): Boolean {
        return try {
            val file = getTargetPhotoFile()
            file.parentFile?.mkdirs()

            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val hash = ImageHashUtil.computePHash(bitmap)
            if (hash != null) {
                prefs.edit().putString(TARGET_HASH_KEY, hash).apply()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadTargetPhoto(): Bitmap? {
        return try {
            val file = getTargetPhotoFile()
            if (file.exists()) {
                FileInputStream(file).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun comparePhotos(capturedBitmap: Bitmap): MatchResult {
        if (!hasTargetPhoto()) {
            return MatchResult.NoTarget
        }

        val targetHash = prefs.getString(TARGET_HASH_KEY, null)
        if (targetHash == null) {
            return MatchResult.Error("Target hash not found")
        }

        val capturedHash = ImageHashUtil.computePHash(capturedBitmap)
        if (capturedHash == null) {
            return MatchResult.Error("Failed to compute hash for captured photo")
        }

        val similarity = ImageHashUtil.getSimilarity(targetHash, capturedHash)
        val hammingDistance = ImageHashUtil.hammingDistance(targetHash, capturedHash)

        return if (similarity >= SIMILARITY_THRESHOLD) {
            MatchResult.Match(similarity.toInt(), hammingDistance)
        } else {
            MatchResult.NoMatch(similarity.toInt(), hammingDistance)
        }
    }

    fun comparePhotosWithFallback(capturedBitmap: Bitmap): MatchResult {
        if (!hasTargetPhoto()) {
            return MatchResult.NoTarget
        }

        val targetHash = prefs.getString(TARGET_HASH_KEY, null)
        if (targetHash == null) {
            return MatchResult.Error("Target hash not found")
        }

        val capturedPHash = ImageHashUtil.computePHash(capturedBitmap)
        val capturedAHash = ImageHashUtil.computeAverageHash(capturedBitmap)
        val capturedDHash = ImageHashUtil.computeDifferenceHash(capturedBitmap)

        val pHashSimilarity = capturedPHash?.let {
            ImageHashUtil.getSimilarity(targetHash, it).toInt()
        } ?: 0

        if (pHashSimilarity >= SIMILARITY_THRESHOLD) {
            return MatchResult.Match(pHashSimilarity, 
                capturedPHash?.let { ImageHashUtil.hammingDistance(targetHash, it) } ?: -1)
        }

        val avgSimilarity = capturedAHash?.let {
            ImageHashUtil.getSimilarity(targetHash, it).toInt()
        } ?: 0

        if (avgSimilarity >= SIMILARITY_THRESHOLD) {
            return MatchResult.Match(avgSimilarity, 
                capturedAHash?.let { ImageHashUtil.hammingDistance(targetHash, it) } ?: -1)
        }

        val diffSimilarity = capturedDHash?.let {
            ImageHashUtil.getSimilarity(targetHash, it).toInt()
        } ?: 0

        if (diffSimilarity >= SIMILARITY_THRESHOLD) {
            return MatchResult.Match(diffSimilarity, 
                capturedDHash?.let { ImageHashUtil.hammingDistance(targetHash, it) } ?: -1)
        }

        val maxSimilarity = maxOf(pHashSimilarity, avgSimilarity, diffSimilarity)
        val avgAll = (pHashSimilarity + avgSimilarity + diffSimilarity) / 3

        return if (avgAll >= SIMILARITY_THRESHOLD) {
            MatchResult.Match(avgAll, -1)
        } else {
            MatchResult.NoMatch(maxSimilarity, -1)
        }
    }

    fun clearTargetPhoto() {
        try {
            val file = getTargetPhotoFile()
            if (file.exists()) {
                file.delete()
            }
            prefs.edit().remove(TARGET_HASH_KEY).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTargetPhotoFile(): File {
        return File(context.filesDir, TARGET_PHOTO_FILENAME)
    }

    fun getSimilarityThreshold(): Float = SIMILARITY_THRESHOLD

    sealed class MatchResult {
        data class Match(val similarity: Int, val hammingDistance: Int) : MatchResult()
        data class NoMatch(val similarity: Int, val hammingDistance: Int) : MatchResult()
        data class Error(val message: String) : MatchResult()
        data object NoTarget : MatchResult()
    }
}
