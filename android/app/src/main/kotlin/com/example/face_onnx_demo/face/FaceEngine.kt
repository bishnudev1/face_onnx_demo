package com.example.face_onnx_demo.face

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import kotlin.math.sqrt

data class FaceEmbeddingResult(
    val embedding: FloatArray,
    val faceDetected: Boolean,
    val confidence: Float
)

data class VerificationResult(
    val isMatch: Boolean,
    val similarity: Float,
    val message: String
)

data class RegistrationResult(
    val success: Boolean,
    val message: String,
    val userId: String? = null
)

class FaceEngine(
    private val context: Context
) {

    companion object {
        private const val TAG = "FACE_NATIVE"
        
        // Default threshold for face verification
        private const val VERIFICATION_THRESHOLD = 0.6f
        
        // Number of images required for registration
        private const val REQUIRED_REGISTRATION_IMAGES = 3
    }

    private val detector = FaceDetector(context)
    private val alignment = FaceAlignment()
    private val recognizer = FaceRecognizer(context)

    private var initialized = false
    
    // Storage for registered faces
    // userId -> List of embeddings (3 images per user)
    private val registeredFaces = mutableMapOf<String, MutableList<FloatArray>>()

    fun initialize() {

        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.initialize() START")
        Log.d(TAG, "========================================")

        if (initialized) {
            Log.d(TAG, "FaceEngine already initialized")
            return
        }

        try {

            Log.d(TAG, "Initializing FaceDetector...")
            detector.initialize()
            Log.d(TAG, "FaceDetector initialized successfully")

            Log.d(TAG, "Initializing FaceRecognizer...")
            recognizer.initialize()
            Log.d(TAG, "FaceRecognizer initialized successfully")

            initialized = true

            Log.d(TAG, "FaceEngine initialization SUCCESS")
            Log.d(TAG, "Detector + Recognizer ready")

        } catch (e: Exception) {

            Log.e(TAG, "FaceEngine initialization FAILED", e)

            initialized = false

            throw e
        }
    }

    /**
     * Complete native face-processing pipeline:
     *
     * image bytes
     *      ↓
     * Bitmap
     *      ↓
     * SCRFD detector
     *      ↓
     * best face + 5 landmarks
     *      ↓
     * ArcFace alignment
     *      ↓
     * 112x112 face
     *      ↓
     * w600k_mbf
     *      ↓
     * 512-D embedding
     */
    fun generateEmbedding(
        imageBytes: ByteArray
    ): FaceEmbeddingResult {

        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.generateEmbedding() START")
        Log.d(TAG, "========================================")

        checkInitialized()

        Log.d(
            TAG,
            "Input image bytes: ${imageBytes.size}"
        )

        if (imageBytes.isEmpty()) {
            Log.e(TAG, "Input image bytes are EMPTY")
            throw IllegalArgumentException("Image bytes are empty.")
        }

        Log.d(TAG, "Decoding image bytes into Bitmap...")

        val bitmap = BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size
        ) ?: throw IllegalArgumentException(
            "Unable to decode image."
        )

        Log.d(
            TAG,
            "Bitmap decoded successfully"
        )

        Log.d(
            TAG,
            "Bitmap size: ${bitmap.width} x ${bitmap.height}"
        )

        Log.d(
            TAG,
            "Bitmap config: ${bitmap.config}"
        )

        return try {

            // ---------------------------------------------------------
            // FACE DETECTION
            // ---------------------------------------------------------

            Log.d(TAG, "----------------------------------------")
            Log.d(TAG, "STEP 1: FACE DETECTION")
            Log.d(TAG, "----------------------------------------")

            Log.d(
                TAG,
                "Calling FaceDetector.detect()..."
            )

            val detectionResult = detector.detect(bitmap)

            Log.d(
                TAG,
                "FaceDetector.detect() returned successfully"
            )

            val detection = detectionResult.detection

            Log.d(
                TAG,
                "Detection object: $detection"
            )

            Log.d(
                TAG,
                "Detection score: ${detection.score}"
            )

            Log.d(
                TAG,
                "Bounding box:"
            )

            Log.d(
                TAG,
                "x1=${detection.x1}"
            )

            Log.d(
                TAG,
                "y1=${detection.y1}"
            )

            Log.d(
                TAG,
                "x2=${detection.x2}"
            )

            Log.d(
                TAG,
                "y2=${detection.y2}"
            )

            Log.d(
                TAG,
                "Landmarks:"
            )

            Log.d(
                TAG,
                detection.landmarks.contentToString()
            )

            // ---------------------------------------------------------
            // FACE ALIGNMENT
            // ---------------------------------------------------------

            Log.d(TAG, "----------------------------------------")
            Log.d(TAG, "STEP 2: FACE ALIGNMENT")
            Log.d(TAG, "----------------------------------------")

            Log.d(
                TAG,
                "Input bitmap: ${bitmap.width}x${bitmap.height}"
            )

            Log.d(
                TAG,
                "Using 5-point landmarks for alignment"
            )

            Log.d(
                TAG,
                "Calling FaceAlignment.align()..."
            )

            val alignedFace = alignment.align(
                bitmap = bitmap,
                landmarks = detection.landmarks
            )

            Log.d(
                TAG,
                "FaceAlignment.align() completed"
            )

            Log.d(
                TAG,
                "Aligned face size: ${alignedFace.width}x${alignedFace.height}"
            )

            Log.d(
                TAG,
                "Aligned face config: ${alignedFace.config}"
            )

            // ---------------------------------------------------------
            // FACE RECOGNITION
            // ---------------------------------------------------------

            Log.d(TAG, "----------------------------------------")
            Log.d(TAG, "STEP 3: FACE RECOGNITION")
            Log.d(TAG, "----------------------------------------")

            Log.d(
                TAG,
                "Expected recognizer input: 112x112"
            )

            Log.d(
                TAG,
                "Actual recognizer input: ${alignedFace.width}x${alignedFace.height}"
            )

            Log.d(
                TAG,
                "Calling FaceRecognizer.generateEmbedding()..."
            )

            val embedding = recognizer.generateEmbedding(
                alignedFace
            )

            Log.d(
                TAG,
                "FaceRecognizer.generateEmbedding() returned"
            )

            // ---------------------------------------------------------
            // EMBEDDING VALIDATION
            // ---------------------------------------------------------

            Log.d(TAG, "----------------------------------------")
            Log.d(TAG, "STEP 4: EMBEDDING VALIDATION")
            Log.d(TAG, "----------------------------------------")

            Log.d(
                TAG,
                "Embedding Java type: ${embedding.javaClass.name}"
            )

            Log.d(
                TAG,
                "Embedding size: ${embedding.size}"
            )

            if (embedding.isEmpty()) {

                Log.e(
                    TAG,
                    "WARNING: Embedding is EMPTY"
                )

            } else {

                Log.d(
                    TAG,
                    "Embedding generated successfully"
                )

                Log.d(
                    TAG,
                    "Embedding[0] = ${embedding[0]}"
                )

                if (embedding.size > 1) {
                    Log.d(
                        TAG,
                        "Embedding[1] = ${embedding[1]}"
                    )
                }

                if (embedding.size > 2) {
                    Log.d(
                        TAG,
                        "Embedding[2] = ${embedding[2]}"
                    )
                }

                if (embedding.size > 3) {
                    Log.d(
                        TAG,
                        "Embedding[3] = ${embedding[3]}"
                    )
                }

                if (embedding.size > 4) {
                    Log.d(
                        TAG,
                        "Embedding[4] = ${embedding[4]}"
                    )
                }

                Log.d(
                    TAG,
                    "Embedding last value = ${embedding.last()}"
                )

                var sumSquares = 0.0

                for (value in embedding) {
                    sumSquares += value.toDouble() * value.toDouble()
                }

                val norm = sqrt(sumSquares)

                Log.d(
                    TAG,
                    "Embedding L2 norm = $norm"
                )

                Log.d(
                    TAG,
                    "Expected embedding dimension = 512"
                )

                Log.d(
                    TAG,
                    "Embedding dimension correct = ${embedding.size == 512}"
                )
            }

            Log.d(
                TAG,
                "Detection confidence = ${detection.score}"
            )

            Log.d(TAG, "========================================")
            Log.d(TAG, "FaceEngine.generateEmbedding() SUCCESS")
            Log.d(TAG, "========================================")

            FaceEmbeddingResult(
                embedding = embedding,
                faceDetected = true,
                confidence = detection.score
            )

        } catch (e: Exception) {

            Log.e(TAG, "========================================")
            Log.e(TAG, "FaceEngine.generateEmbedding() FAILED")
            Log.e(TAG, "========================================")

            Log.e(
                TAG,
                "Exception type: ${e.javaClass.name}"
            )

            Log.e(
                TAG,
                "Exception message: ${e.message}"
            )

            Log.e(
                TAG,
                "Full exception:",
                e
            )

            throw e

        } finally {

            Log.d(
                TAG,
                "Recycling original bitmap..."
            )

            if (!bitmap.isRecycled) {
                bitmap.recycle()
                Log.d(TAG, "Original bitmap recycled")
            } else {
                Log.d(TAG, "Bitmap was already recycled")
            }
        }
    }

    // ================================================================
    // REGISTRATION FUNCTIONS
    // ================================================================

    /**
     * Register a new user with their face embedding.
     * Stores the embedding in memory for later verification.
     */
    fun registerFace(
        embedding: FloatArray,
        userId: String
    ): RegistrationResult {
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.registerFace() START")
        Log.d(TAG, "========================================")
        
        checkInitialized()
        
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "Embedding size: ${embedding.size}")
        
        // Validate embedding
        if (embedding.isEmpty()) {
            Log.e(TAG, "Embedding is empty")
            return RegistrationResult(
                success = false,
                message = "Embedding is empty",
                userId = userId
            )
        }
        
        if (embedding.size != 512) {
            Log.e(TAG, "Invalid embedding size: ${embedding.size}")
            return RegistrationResult(
                success = false,
                message = "Invalid embedding size: ${embedding.size}",
                userId = userId
            )
        }
        
        // Check if user already exists
        if (registeredFaces.containsKey(userId)) {
            val existing = registeredFaces[userId]!!
            
            // If user already has 3 embeddings, reject new registration
            if (existing.size >= REQUIRED_REGISTRATION_IMAGES) {
                Log.w(TAG, "User $userId already has ${existing.size} embeddings")
                return RegistrationResult(
                    success = false,
                    message = "User already registered with ${existing.size} images",
                    userId = userId
                )
            }
            
            // Add new embedding to existing user
            existing.add(embedding)
            Log.d(TAG, "Added embedding ${existing.size}/${REQUIRED_REGISTRATION_IMAGES} for user $userId")
            
            // Check if user now has 3 embeddings
            val isComplete = existing.size == REQUIRED_REGISTRATION_IMAGES
            return RegistrationResult(
                success = true,
                message = if (isComplete) 
                    "Registration complete! All $REQUIRED_REGISTRATION_IMAGES images captured." 
                else 
                    "Image ${existing.size}/${REQUIRED_REGISTRATION_IMAGES} captured successfully. Need ${REQUIRED_REGISTRATION_IMAGES - existing.size} more.",
                userId = userId
            )
        } else {
            // New user - create new list with this embedding
            val embeddings = mutableListOf(embedding)
            registeredFaces[userId] = embeddings
            
            Log.d(TAG, "New user registered: $userId (1/${REQUIRED_REGISTRATION_IMAGES})")
            
            return RegistrationResult(
                success = true,
                message = "User created! Image 1/${REQUIRED_REGISTRATION_IMAGES} captured. Need ${REQUIRED_REGISTRATION_IMAGES - 1} more.",
                userId = userId
            )
        }
    }

    /**
     * Check if a user has completed registration.
     */
    fun isUserRegistered(userId: String): Boolean {
        return registeredFaces.containsKey(userId) && 
               registeredFaces[userId]?.size == REQUIRED_REGISTRATION_IMAGES
    }

    /**
     * Get the number of registered images for a user.
     */
    fun getRegistrationProgress(userId: String): Int {
        return registeredFaces[userId]?.size ?: 0
    }

    /**
     * Get all registered user IDs.
     */
    fun getRegisteredUsers(): List<String> {
        return registeredFaces.keys.toList()
    }

    /**
     * Get all embeddings for a user.
     */
    fun getUserEmbeddings(userId: String): List<FloatArray>? {
        return registeredFaces[userId]
    }

    // ================================================================
    // VERIFICATION FUNCTIONS
    // ================================================================

    /**
     * Verify a face against a registered user.
     * Compares the query embedding against all stored embeddings for the user.
     * Uses the best similarity score (not average).
     */
    fun verifyFace(
        queryEmbedding: FloatArray,
        userId: String
    ): VerificationResult {
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.verifyFace() START")
        Log.d(TAG, "========================================")
        
        checkInitialized()
        
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "Query embedding size: ${queryEmbedding.size}")
        
        // Validate query embedding
        if (queryEmbedding.isEmpty()) {
            Log.e(TAG, "Query embedding is empty")
            return VerificationResult(
                isMatch = false,
                similarity = 0f,
                message = "Query embedding is empty"
            )
        }
        
        // Check if user exists
        if (!registeredFaces.containsKey(userId)) {
            Log.w(TAG, "User $userId not found")
            return VerificationResult(
                isMatch = false,
                similarity = 0f,
                message = "User not registered"
            )
        }
        
        val storedEmbeddings = registeredFaces[userId]!!
        
        if (storedEmbeddings.isEmpty()) {
            Log.e(TAG, "No embeddings stored for user $userId")
            return VerificationResult(
                isMatch = false,
                similarity = 0f,
                message = "No embeddings found for this user"
            )
        }
        
        Log.d(TAG, "Found ${storedEmbeddings.size} embeddings for user $userId")
        
        // Compare query against all stored embeddings
        var bestSimilarity = -1f
        var bestIndex = -1
        
        for ((index, storedEmbedding) in storedEmbeddings.withIndex()) {
            val similarity = cosineSimilarity(queryEmbedding, storedEmbedding)
            Log.d(TAG, "Similarity with embedding $index: $similarity")
            
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestIndex = index
            }
        }
        
        Log.d(TAG, "Best similarity: $bestSimilarity (embedding $bestIndex)")
        Log.d(TAG, "Threshold: $VERIFICATION_THRESHOLD")
        
        // Determine if match
        val isMatch = bestSimilarity >= VERIFICATION_THRESHOLD
        
        val message = if (isMatch) {
            "Face verified! Similarity: ${String.format("%.2f", bestSimilarity * 100)}%"
        } else {
            "Face does not match. Similarity: ${String.format("%.2f", bestSimilarity * 100)}%"
        }
        
        Log.d(TAG, "Verification result: ${if (isMatch) "MATCH" else "NO MATCH"}")
        
        return VerificationResult(
            isMatch = isMatch,
            similarity = bestSimilarity,
            message = message
        )
    }

    /**
     * Verify a face using multiple registered users (1:N matching).
     * Returns the best matching user ID and similarity.
     */
    fun verifyFaceAgainstAll(queryEmbedding: FloatArray): Pair<String?, Float> {
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.verifyFaceAgainstAll() START")
        Log.d(TAG, "========================================")
        
        checkInitialized()
        
        if (registeredFaces.isEmpty()) {
            Log.w(TAG, "No users registered")
            return Pair(null, 0f)
        }
        
        var bestUserId: String? = null
        var bestSimilarity = -1f
        
        for ((userId, embeddings) in registeredFaces) {
            for (embedding in embeddings) {
                val similarity = cosineSimilarity(queryEmbedding, embedding)
                Log.d(TAG, "User: $userId, Similarity: $similarity")
                
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity
                    bestUserId = userId
                }
            }
        }
        
        Log.d(TAG, "Best match: $bestUserId with similarity $bestSimilarity")
        
        return Pair(bestUserId, bestSimilarity)
    }

    // ================================================================
    // DELETION FUNCTIONS
    // ================================================================

    /**
     * Delete a user's registration.
     */
    fun deleteRegistration(userId: String): Boolean {
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.deleteRegistration() START")
        Log.d(TAG, "========================================")
        
        if (!registeredFaces.containsKey(userId)) {
            Log.w(TAG, "User $userId not found")
            return false
        }
        
        registeredFaces.remove(userId)
        Log.d(TAG, "User $userId deleted successfully")
        return true
    }

    /**
     * Clear all registrations.
     */
    fun clearAllRegistrations(): Boolean {
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.clearAllRegistrations() START")
        Log.d(TAG, "========================================")
        
        val count = registeredFaces.size
        registeredFaces.clear()
        Log.d(TAG, "Cleared $count registrations")
        return true
    }

    // ================================================================
    // EXISTING COMPARE FUNCTION (for backward compatibility)
    // ================================================================

    /**
     * Compare a verification selfie against the
     * three enrolled embeddings.
     * (Legacy method - kept for backward compatibility)
     */
    fun compare(
        query: FloatArray,
        front: FloatArray,
        left: FloatArray,
        right: FloatArray
    ): ComparisonResult {

        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.compare() START")
        Log.d(TAG, "========================================")

        Log.d(
            TAG,
            "Query size = ${query.size}"
        )

        Log.d(
            TAG,
            "Front size = ${front.size}"
        )

        Log.d(
            TAG,
            "Left size = ${left.size}"
        )

        Log.d(
            TAG,
            "Right size = ${right.size}"
        )

        val frontScore =
            cosineSimilarity(query, front)

        val leftScore =
            cosineSimilarity(query, left)

        val rightScore =
            cosineSimilarity(query, right)

        val average =
            (frontScore + leftScore + rightScore) / 3f

        val best =
            maxOf(
                frontScore,
                leftScore,
                rightScore
            )

        Log.d(
            TAG,
            "Front similarity = $frontScore"
        )

        Log.d(
            TAG,
            "Left similarity = $leftScore"
        )

        Log.d(
            TAG,
            "Right similarity = $rightScore"
        )

        Log.d(
            TAG,
            "Average similarity = $average"
        )

        Log.d(
            TAG,
            "Best similarity = $best"
        )

        Log.d(TAG, "FaceEngine.compare() SUCCESS")

        return ComparisonResult(
            front = frontScore,
            left = leftScore,
            right = rightScore,
            average = average,
            best = best
        )
    }

    /**
     * Compare two already-generated normalized embeddings.
     *
     * Because both embeddings are L2 normalized,
     * cosine similarity is simply their dot product.
     */
    fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray
    ): Float {

        Log.d(TAG, "----------------------------------------")
        Log.d(TAG, "COSINE SIMILARITY")
        Log.d(TAG, "----------------------------------------")

        Log.d(
            TAG,
            "First embedding size = ${first.size}"
        )

        Log.d(
            TAG,
            "Second embedding size = ${second.size}"
        )

        require(first.size == second.size) {
            "Embedding dimensions do not match."
        }

        if (first.isEmpty()) {
            Log.w(
                TAG,
                "First embedding is empty"
            )

            return 0f
        }

        var similarity = 0f

        for (i in first.indices) {
            similarity += first[i] * second[i]
        }

        Log.d(
            TAG,
            "Cosine similarity = $similarity"
        )

        return similarity
    }

    fun dispose() {

        Log.d(TAG, "========================================")
        Log.d(TAG, "FaceEngine.dispose() START")
        Log.d(TAG, "========================================")

        try {

            Log.d(TAG, "Disposing FaceDetector...")
            detector.dispose()
            Log.d(TAG, "FaceDetector disposed")

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error disposing FaceDetector",
                e
            )
        }

        try {

            Log.d(TAG, "Disposing FaceRecognizer...")
            recognizer.dispose()
            Log.d(TAG, "FaceRecognizer disposed")

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error disposing FaceRecognizer",
                e
            )
        }

        // Clear registered faces
        registeredFaces.clear()
        
        initialized = false

        Log.d(
            TAG,
            "FaceEngine.dispose() COMPLETE"
        )
    }

    private fun checkInitialized() {

        if (!initialized) {

            Log.e(
                TAG,
                "FaceEngine is NOT initialized"
            )

            throw IllegalStateException(
                "FaceEngine has not been initialized."
            )
        }

        Log.d(
            TAG,
            "FaceEngine initialization check PASSED"
        )
    }
}

data class ComparisonResult(
    val front: Float,
    val left: Float,
    val right: Float,
    val average: Float,
    val best: Float
)