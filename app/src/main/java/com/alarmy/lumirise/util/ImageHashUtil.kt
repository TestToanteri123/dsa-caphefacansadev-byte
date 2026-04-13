package com.alarmy.lumirise.util

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Utility class for computing perceptual hashes (pHash) of images.
 * pHash is used for image similarity comparison by generating a hash
 * based on the visual content of the image rather than exact pixel values.
 */
object ImageHashUtil {

    /**
     * Hash size for DCT-based pHash (8x8 = 64 bits)
     */
    private const val HASH_SIZE = 8

    /**
     * High-quality factor for DCT computation
     */
    private const val HIGH_QUALITY_FACTOR = 1

    /**
     * Computes a perceptual hash (pHash) of a bitmap.
     * Uses DCT-based algorithm for robust image similarity detection.
     *
     * @param bitmap The input bitmap to hash
     * @return A 64-character hex string representing the pHash, or null if computation fails
     */
    fun computePHash(bitmap: Bitmap): String? {
        return try {
            // Step 1: Resize to small square (8x8 for pHash)
            val resized = resizeBitmap(bitmap, HASH_SIZE, HASH_SIZE)

            // Step 2: Convert to grayscale
            val grayscale = toGrayscale(resized)

            // Step 3: Compute DCT
            val dctResult = applyDCT(grayscale)

            // Step 4: Take top-left 8x8 submatrix
            val dctSubmatrix = getDCTSubmatrix(dctResult, HASH_SIZE)

            // Step 5: Compute median value
            val median = computeMedian(dctSubmatrix)

            // Step 6: Generate hash based on comparison with median
            generateHash(dctSubmatrix, median)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Computes Average Hash (aHash) - simpler and faster alternative to pHash.
     * Less accurate but useful for quick comparisons.
     *
     * @param bitmap The input bitmap to hash
     * @return A 64-character hex string representing the aHash, or null if computation fails
     */
    fun computeAverageHash(bitmap: Bitmap): String? {
        return try {
            // Resize to 8x8
            val resized = resizeBitmap(bitmap, HASH_SIZE, HASH_SIZE)

            // Convert to grayscale and compute average
            var sum = 0L
            val pixels = IntArray(HASH_SIZE * HASH_SIZE)
            for (y in 0 until HASH_SIZE) {
                for (x in 0 until HASH_SIZE) {
                    val pixel = resized.getPixel(x, y)
                    val gray = (Color.red(pixel) * 0.299 +
                            Color.green(pixel) * 0.587 +
                            Color.blue(pixel) * 0.114).toInt()
                    pixels[y * HASH_SIZE + x] = gray
                    sum += gray
                }
            }

            val average = sum / (HASH_SIZE * HASH_SIZE)

            // Generate hash
            buildString {
                for (pixel in pixels) {
                    append(if (pixel > average) "1" else "0")
                }
            }.toBigInteger(2).toString(16).padStart(16, '0')
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Computes Difference Hash (dHash) - based on gradient direction between pixels.
     *
     * @param bitmap The input bitmap to hash
     * @return A 64-character hex string representing the dHash, or null if computation fails
     */
    fun computeDifferenceHash(bitmap: Bitmap): String? {
        return try {
            // Resize to 9x8 (comparing each pixel with next)
            val resized = resizeBitmap(bitmap, HASH_SIZE + 1, HASH_SIZE)

            // Convert to grayscale and compute hash
            buildString {
                for (y in 0 until HASH_SIZE) {
                    for (x in 0 until HASH_SIZE) {
                        val left = resized.getPixel(x, y)
                        val right = resized.getPixel(x + 1, y)
                        val leftGray = (Color.red(left) * 0.299 +
                                Color.green(left) * 0.587 +
                                Color.blue(left) * 0.114).toInt()
                        val rightGray = (Color.red(right) * 0.299 +
                                Color.green(right) * 0.587 +
                                Color.blue(right) * 0.114).toInt()
                        append(if (leftGray > rightGray) "1" else "0")
                    }
                }
            }.toBigInteger(2).toString(16).padStart(16, '0')
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compares two hashes and returns the Hamming distance (number of differing bits).
     *
     * @param hash1 First hash (64-char hex string)
     * @param hash2 Second hash (64-char hex string)
     * @return Hamming distance, or -1 if hashes are invalid
     */
    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length) {
            return -1
        }

        val bigInt1 = hash1.toBigInteger(16)
        val bigInt2 = hash2.toBigInteger(16)
        val xor = bigInt1 xor bigInt2

        // Count bits set to 1 (Hamming distance)
        return xor.bitCount()
    }

    /**
     * Calculates similarity percentage between two hashes.
     *
     * @param hash1 First hash
     * @param hash2 Second hash
     * @return Similarity as percentage (0-100)
     */
    fun getSimilarity(hash1: String, hash2: String): Float {
        val distance = hammingDistance(hash1, hash2)
        if (distance < 0) return 0f

        // With 64 bits, max distance is 64
        val similarity = 100f * (64 - distance) / 64f
        return similarity.coerceIn(0f, 100f)
    }

    /**
     * Resizes a bitmap to the specified dimensions.
     */
    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /**
     * Converts a bitmap to grayscale.
     */
    private fun toGrayscale(bitmap: Bitmap): DoubleArray {
        val size = bitmap.width * bitmap.height
        val grayscale = DoubleArray(size)

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val gray = 0.299 * Color.red(pixel) +
                        0.587 * Color.green(pixel) +
                        0.114 * Color.blue(pixel)
                grayscale[y * bitmap.width + x] = gray
            }
        }

        return grayscale
    }

    /**
     * Applies Discrete Cosine Transform (DCT) to the grayscale image.
     * Uses a simplified 2D DCT implementation.
     */
    private fun applyDCT(grayscale: DoubleArray): DoubleArray {
        val n = HASH_SIZE
        val dct = DoubleArray(n * n)

        for (u in 0 until n) {
            for (v in 0 until n) {
                var sum = 0.0
                for (i in 0 until n) {
                    for (j in 0 until n) {
                        val pixel = grayscale[i * n + j]
                        sum += pixel * cosTerm(u, i, n) * cosTerm(v, j, n)
                    }
                }
                sum *= cu(u) * cv(v)
                dct[u * n + v] = sum
            }
        }

        return dct
    }

    /**
     * Helper function for DCT computation (cosine term).
     */
    private fun cosTerm(u: Int, i: Int, n: Int): Double {
        return kotlin.math.cos((2 * i + 1) * u * kotlin.math.PI / (2 * n))
    }

    /**
     * DCT coefficient multiplier for u=0.
     */
    private fun cu(u: Int): Double {
        return if (u == 0) 1.0 / sqrt(2.0) else 1.0
    }

    /**
     * DCT coefficient multiplier for v=0.
     */
    private fun cv(v: Int): Double {
        return if (v == 0) 1.0 / sqrt(2.0) else 1.0
    }

    /**
     * Gets the top-left submatrix of the DCT result.
     */
    private fun getDCTSubmatrix(dct: DoubleArray, size: Int): DoubleArray {
        val submatrix = DoubleArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                submatrix[y * size + x] = dct[y * size + x]
            }
        }
        return submatrix
    }

    /**
     * Computes the median value of the DCT coefficients.
     */
    private fun computeMedian(values: DoubleArray): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2
        } else {
            sorted[mid]
        }
    }

    /**
     * Generates a hash by comparing each DCT coefficient with the median.
     */
    private fun generateHash(dctSubmatrix: DoubleArray, median: Double): String {
        return buildString {
            for (value in dctSubmatrix) {
                append(if (value > median) "1" else "0")
            }
        }.toBigInteger(2).toString(16).padStart(16, '0')
    }

    /**
     * Converts a bitmap to a byte array (PNG format).
     * Useful for storing images.
     */
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }
}
