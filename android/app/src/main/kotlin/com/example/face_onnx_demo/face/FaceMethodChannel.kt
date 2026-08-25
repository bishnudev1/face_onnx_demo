package com.example.face_onnx_demo

import android.content.Context
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.example.face_onnx_demo.face.FaceEngine
import com.example.face_onnx_demo.face.RegistrationResult
import com.example.face_onnx_demo.face.VerificationResult

class FaceMethodChannel(
    private val context: Context,
    flutterEngine: FlutterEngine
) {

    companion object {
        private const val CHANNEL = "com.example.face_onnx_demo/face"
        private const val TAG = "FACE_NATIVE"
    }

    private val faceEngine = FaceEngine(context)

    init {

        Log.d(TAG, "========================================")
        Log.d(TAG, "Creating FaceMethodChannel")
        Log.d(TAG, "Channel: $CHANNEL")
        Log.d(TAG, "========================================")

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->

            Log.d(TAG, "MethodChannel call received")
            Log.d(TAG, "Method: ${call.method}")
            Log.d(TAG, "Arguments type: ${call.arguments?.javaClass?.name}")
            Log.d(TAG, "Arguments: ${call.arguments}")

            when (call.method) {

                "initialize" -> {
                    initialize(result)
                }

                "generateEmbedding" -> {
                    generateEmbedding(call, result)
                }

                "registerFace" -> {
                    registerFace(call, result)
                }

                "verifyFace" -> {
                    verifyFace(call, result)
                }

                "verifyFaceAgainstAll" -> {
                    verifyFaceAgainstAll(call, result)
                }

                "isUserRegistered" -> {
                    isUserRegistered(call, result)
                }

                "getRegistrationProgress" -> {
                    getRegistrationProgress(call, result)
                }

                "getRegisteredUsers" -> {
                    getRegisteredUsers(result)
                }

                "deleteRegistration" -> {
                    deleteRegistration(call, result)
                }

                "clearAllRegistrations" -> {
                    clearAllRegistrations(result)
                }

                "compareEmbeddings" -> {
                    compareEmbeddings(call, result)
                }

                "dispose" -> {
                    dispose(result)
                }

                else -> {
                    Log.w(TAG, "Unknown method: ${call.method}")
                    result.notImplemented()
                }
            }
        }
    }

    // ================================================================
    // INITIALIZATION
    // ================================================================

    private fun initialize(
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "----------------------------------------")
        Log.d(TAG, "INITIALIZE START")
        Log.d(TAG, "----------------------------------------")

        try {

            Log.d(TAG, "Calling FaceEngine.initialize()")

            faceEngine.initialize()

            Log.d(TAG, "FaceEngine.initialize() SUCCESS")

            result.success(
                mapOf(
                    "success" to true
                )
            )

            Log.d(TAG, "Initialize result sent to Flutter")

        } catch (e: Exception) {

            Log.e(TAG, "INITIALIZATION ERROR", e)

            result.error(
                "INITIALIZATION_ERROR",
                e.message,
                null
            )
        }
    }

    // ================================================================
    // EMBEDDING GENERATION
    // ================================================================

    private fun generateEmbedding(
        call: MethodCall,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "GENERATE EMBEDDING START")
        Log.d(TAG, "========================================")

        try {

            Log.d(TAG, "Reading image argument...")

            val rawImage = call.argument<Any>("image")

            Log.d(
                TAG,
                "Image argument type: ${rawImage?.javaClass?.name}"
            )

            Log.d(
                TAG,
                "Image argument size: ${
                    when (rawImage) {
                        is ByteArray -> rawImage.size
                        else -> "NOT_BYTE_ARRAY"
                    }
                }"
            )

            if (rawImage !is ByteArray) {

                Log.e(
                    TAG,
                    "Image is NOT ByteArray!"
                )

                result.error(
                    "INVALID_IMAGE",
                    "Expected ByteArray but received ${rawImage?.javaClass?.name}",
                    null
                )

                return
            }

            val imageBytes = rawImage

            Log.d(
                TAG,
                "Image successfully received: ${imageBytes.size} bytes"
            )

            Log.d(TAG, "Calling FaceEngine.generateEmbedding()")

            val output = faceEngine.generateEmbedding(imageBytes)

            Log.d(TAG, "FaceEngine.generateEmbedding() returned")

            Log.d(
                TAG,
                "faceDetected = ${output.faceDetected}"
            )

            Log.d(
                TAG,
                "confidence = ${output.confidence}"
            )

            Log.d(
                TAG,
                "embedding type = ${output.embedding.javaClass.name}"
            )

            Log.d(
                TAG,
                "embedding size = ${output.embedding.size}"
            )

            if (output.embedding.isNotEmpty()) {

                Log.d(
                    TAG,
                    "embedding[0] = ${output.embedding[0]}"
                )

                Log.d(
                    TAG,
                    "embedding[1] = ${output.embedding.getOrNull(1)}"
                )

                Log.d(
                    TAG,
                    "embedding[2] = ${output.embedding.getOrNull(2)}"
                )
            }

            if (!output.faceDetected) {

                Log.d(
                    TAG,
                    "No face detected. Sending empty result."
                )

                result.success(
                    mapOf(
                        "success" to true,
                        "faceDetected" to false,
                        "embedding" to null,
                        "confidence" to output.confidence.toDouble()
                    )
                )

                Log.d(TAG, "No-face result sent successfully")

                return
            }

            Log.d(TAG, "Converting FloatArray -> List<Double>")

            val embeddingList = output.embedding
                .map { value ->
                    value.toDouble()
                }
                .toList()

            Log.d(
                TAG,
                "embeddingList type = ${embeddingList.javaClass.name}"
            )

            Log.d(
                TAG,
                "embeddingList size = ${embeddingList.size}"
            )

            Log.d(
                TAG,
                "embeddingList first value = ${embeddingList.firstOrNull()}"
            )

            val response = mapOf(
                "success" to true,
                "faceDetected" to true,
                "embedding" to embeddingList,
                "confidence" to output.confidence.toDouble()
            )

            Log.d(
                TAG,
                "Response map created"
            )

            Log.d(
                TAG,
                "Response embedding type = ${
                    response["embedding"]?.javaClass?.name
                }"
            )

            Log.d(
                TAG,
                "Calling result.success()..."
            )

            result.success(response)

            Log.d(
                TAG,
                "result.success() completed"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            )

            Log.e(
                TAG,
                "GENERATE EMBEDDING ERROR",
                e
            )

            Log.e(
                TAG,
                "Exception class = ${e.javaClass.name}"
            )

            Log.e(
                TAG,
                "Exception message = ${e.message}"
            )

            Log.e(
                TAG,
                "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            )

            result.error(
                "EMBEDDING_ERROR",
                e.message,
                null
            )
        }
    }

    // ================================================================
    // REGISTRATION METHODS
    // ================================================================

    private fun registerFace(
        call: MethodCall,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "REGISTER FACE START")
        Log.d(TAG, "========================================")

        try {
            val userId = call.argument<String>("userId")
            val embeddingList = call.argument<List<Any>>("embedding")

            Log.d(TAG, "userId: $userId")
            Log.d(TAG, "embedding size: ${embeddingList?.size}")

            if (userId.isNullOrEmpty()) {
                Log.e(TAG, "userId is null or empty")
                result.error(
                    "INVALID_USER_ID",
                    "userId is required",
                    null
                )
                return
            }

            if (embeddingList == null || embeddingList.isEmpty()) {
                Log.e(TAG, "embedding is null or empty")
                result.error(
                    "INVALID_EMBEDDING",
                    "embedding is required",
                    null
                )
                return
            }

            val embedding = embeddingList.toFloatArraySafe()

            val registrationResult = faceEngine.registerFace(
                embedding = embedding,
                userId = userId
            )

            Log.d(TAG, "Registration result: ${registrationResult.success}")
            Log.d(TAG, "Registration message: ${registrationResult.message}")

            result.success(
                mapOf(
                    "success" to registrationResult.success,
                    "message" to registrationResult.message,
                    "userId" to registrationResult.userId,
                    "isComplete" to (registrationResult.success && 
                                    faceEngine.isUserRegistered(userId))
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "REGISTER FACE ERROR", e)
            result.error(
                "REGISTRATION_ERROR",
                e.message,
                null
            )
        }
    }

    // ================================================================
    // VERIFICATION METHODS
    // ================================================================

    private fun verifyFace(
        call: MethodCall,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "VERIFY FACE START")
        Log.d(TAG, "========================================")

        try {
            val userId = call.argument<String>("userId")
            val embeddingList = call.argument<List<Any>>("embedding")

            Log.d(TAG, "userId: $userId")
            Log.d(TAG, "embedding size: ${embeddingList?.size}")

            if (userId.isNullOrEmpty()) {
                Log.e(TAG, "userId is null or empty")
                result.error(
                    "INVALID_USER_ID",
                    "userId is required",
                    null
                )
                return
            }

            if (embeddingList == null || embeddingList.isEmpty()) {
                Log.e(TAG, "embedding is null or empty")
                result.error(
                    "INVALID_EMBEDDING",
                    "embedding is required",
                    null
                )
                return
            }

            val embedding = embeddingList.toFloatArraySafe()

            val verificationResult = faceEngine.verifyFace(
                queryEmbedding = embedding,
                userId = userId
            )

            Log.d(TAG, "Verification result:")
            Log.d(TAG, "  isMatch: ${verificationResult.isMatch}")
            Log.d(TAG, "  similarity: ${verificationResult.similarity}")
            Log.d(TAG, "  message: ${verificationResult.message}")

            result.success(
                mapOf(
                    "isMatch" to verificationResult.isMatch,
                    "similarity" to verificationResult.similarity.toDouble(),
                    "message" to verificationResult.message,
                    "threshold" to 0.6 // Default threshold
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "VERIFY FACE ERROR", e)
            result.error(
                "VERIFICATION_ERROR",
                e.message,
                null
            )
        }
    }

    private fun verifyFaceAgainstAll(
        call: MethodCall,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "VERIFY FACE AGAINST ALL START")
        Log.d(TAG, "========================================")

        try {
            val embeddingList = call.argument<List<Any>>("embedding")

            Log.d(TAG, "embedding size: ${embeddingList?.size}")

            if (embeddingList == null || embeddingList.isEmpty()) {
                Log.e(TAG, "embedding is null or empty")
                result.error(
                    "INVALID_EMBEDDING",
                    "embedding is required",
                    null
                )
                return
            }

            val embedding = embeddingList.toFloatArraySafe()

            val (userId, similarity) = faceEngine.verifyFaceAgainstAll(embedding)

            Log.d(TAG, "Best match: userId=$userId, similarity=$similarity")

            result.success(
                mapOf(
                    "userId" to userId,
                    "similarity" to similarity.toDouble(),
                    "isMatch" to (userId != null && similarity >= 0.6)
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "VERIFY FACE AGAINST ALL ERROR", e)
            result.error(
                "VERIFICATION_ERROR",
                e.message,
                null
            )
        }
    }

    // ================================================================
    // USER MANAGEMENT METHODS
    // ================================================================

    private fun isUserRegistered(
        call: MethodCall,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "IS USER REGISTERED START")
        Log.d(TAG, "========================================")

        try {
            val userId = call.argument<String>("userId")

            Log.d(TAG, "userId: $userId")

            if (userId.isNullOrEmpty()) {
                Log.e(TAG, "userId is null or empty")
                result.error(
                    "INVALID_USER_ID",
                    "userId is required",
                    null
                )
                return
            }

            val isRegistered = faceEngine.isUserRegistered(userId)

            Log.d(TAG, "isRegistered: $isRegistered")

            result.success(
                mapOf(
                    "isRegistered" to isRegistered,
                    "userId" to userId
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "IS USER REGISTERED ERROR", e)
            result.error(
                "USER_CHECK_ERROR",
                e.message,
                null
            )
        }
    }

    private fun getRegistrationProgress(
        call: MethodCall,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "GET REGISTRATION PROGRESS START")
        Log.d(TAG, "========================================")

        try {
            val userId = call.argument<String>("userId")

            Log.d(TAG, "userId: $userId")

            if (userId.isNullOrEmpty()) {
                Log.e(TAG, "userId is null or empty")
                result.error(
                    "INVALID_USER_ID",
                    "userId is required",
                    null
                )
                return
            }

            val progress = faceEngine.getRegistrationProgress(userId)

            Log.d(TAG, "progress: $progress/3")

            result.success(
                mapOf(
                    "progress" to progress,
                    "required" to 3,
                    "isComplete" to (progress == 3),
                    "userId" to userId
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "GET REGISTRATION PROGRESS ERROR", e)
            result.error(
                "PROGRESS_ERROR",
                e.message,
                null
            )
        }
    }

    private fun getRegisteredUsers(
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "GET REGISTERED USERS START")
        Log.d(TAG, "========================================")

        try {
            val users = faceEngine.getRegisteredUsers()

            Log.d(TAG, "Registered users: $users")

            result.success(
                mapOf(
                    "users" to users,
                    "count" to users.size
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "GET REGISTERED USERS ERROR", e)
            result.error(
                "USERS_ERROR",
                e.message,
                null
            )
        }
    }

    private fun deleteRegistration(
        call: MethodCall,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "DELETE REGISTRATION START")
        Log.d(TAG, "========================================")

        try {
            val userId = call.argument<String>("userId")

            Log.d(TAG, "userId: $userId")

            if (userId.isNullOrEmpty()) {
                Log.e(TAG, "userId is null or empty")
                result.error(
                    "INVALID_USER_ID",
                    "userId is required",
                    null
                )
                return
            }

            val deleted = faceEngine.deleteRegistration(userId)

            Log.d(TAG, "deleted: $deleted")

            result.success(
                mapOf(
                    "success" to deleted,
                    "userId" to userId
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "DELETE REGISTRATION ERROR", e)
            result.error(
                "DELETE_ERROR",
                e.message,
                null
            )
        }
    }

    private fun clearAllRegistrations(
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "CLEAR ALL REGISTRATIONS START")
        Log.d(TAG, "========================================")

        try {
            val cleared = faceEngine.clearAllRegistrations()

            Log.d(TAG, "cleared: $cleared")

            result.success(
                mapOf(
                    "success" to cleared
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "CLEAR ALL REGISTRATIONS ERROR", e)
            result.error(
                "CLEAR_ERROR",
                e.message,
                null
            )
        }
    }

    // ================================================================
    // EXISTING METHODS (KEPT FOR BACKWARD COMPATIBILITY)
    // ================================================================

    private fun compareEmbeddings(
        call: MethodCall,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "COMPARE EMBEDDINGS START")
        Log.d(TAG, "========================================")

        try {

            val query = call.argument<List<Any>>("query")
            val front = call.argument<List<Any>>("front")
            val left = call.argument<List<Any>>("left")
            val right = call.argument<List<Any>>("right")

            Log.d(TAG, "query size = ${query?.size}")
            Log.d(TAG, "front size = ${front?.size}")
            Log.d(TAG, "left size = ${left?.size}")
            Log.d(TAG, "right size = ${right?.size}")

            if (
                query == null ||
                front == null ||
                left == null ||
                right == null
            ) {

                Log.e(TAG, "One or more embeddings are null")

                result.error(
                    "INVALID_EMBEDDINGS",
                    "All four embeddings are required.",
                    null
                )

                return
            }

            val queryArray = query.toFloatArraySafe()
            val frontArray = front.toFloatArraySafe()
            val leftArray = left.toFloatArraySafe()
            val rightArray = right.toFloatArraySafe()

            Log.d(TAG, "Converted all embeddings to FloatArray")
            Log.d(TAG, "queryArray size = ${queryArray.size}")
            Log.d(TAG, "frontArray size = ${frontArray.size}")
            Log.d(TAG, "leftArray size = ${leftArray.size}")
            Log.d(TAG, "rightArray size = ${rightArray.size}")

            val comparison = faceEngine.compare(
                query = queryArray,
                front = frontArray,
                left = leftArray,
                right = rightArray
            )

            Log.d(TAG, "Comparison completed")
            Log.d(TAG, "front = ${comparison.front}")
            Log.d(TAG, "left = ${comparison.left}")
            Log.d(TAG, "right = ${comparison.right}")
            Log.d(TAG, "average = ${comparison.average}")
            Log.d(TAG, "best = ${comparison.best}")

            result.success(
                mapOf(
                    "front" to comparison.front.toDouble(),
                    "left" to comparison.left.toDouble(),
                    "right" to comparison.right.toDouble(),
                    "average" to comparison.average.toDouble(),
                    "best" to comparison.best.toDouble()
                )
            )

        } catch (e: Exception) {

            Log.e(TAG, "COMPARE ERROR", e)

            result.error(
                "COMPARISON_ERROR",
                e.message,
                null
            )
        }
    }

    private fun dispose(
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "DISPOSE START")

        try {

            faceEngine.dispose()

            Log.d(TAG, "FaceEngine disposed")

            result.success(
                mapOf(
                    "success" to true
                )
            )

        } catch (e: Exception) {

            Log.e(TAG, "DISPOSE ERROR", e)

            result.error(
                "DISPOSE_ERROR",
                e.message,
                null
            )
        }
    }

    // ================================================================
    // UTILITY FUNCTIONS
    // ================================================================

    private fun List<Any>.toFloatArraySafe(): FloatArray {

        Log.d(
            TAG,
            "Converting List to FloatArray, size=$size"
        )

        return FloatArray(size) { index ->

            val value = this[index]

            if (value !is Number) {

                Log.e(
                    TAG,
                    "Invalid embedding value at index $index: $value"
                )

                throw IllegalArgumentException(
                    "Embedding contains a non-numeric value at index $index."
                )
            }

            value.toFloat()
        }
    }
}