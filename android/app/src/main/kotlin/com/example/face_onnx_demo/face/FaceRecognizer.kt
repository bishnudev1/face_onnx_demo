package com.example.face_onnx_demo.face

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.math.sqrt

class FaceRecognizer(
    private val context: Context
) {

    companion object {
        private const val MODEL_NAME = "w600k_mbf.onnx"
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 512
        private const val INPUT_NAME = "input.1"
    }

    private val environment = OrtEnvironment.getEnvironment()

    private var session: OrtSession? = null

    fun initialize() {
        if (session != null) {
            return
        }

        val modelBytes = context.assets
            .open(MODEL_NAME)
            .use { it.readBytes() }

        val options = OrtSession.SessionOptions()

        session = environment.createSession(
            modelBytes,
            options
        )
    }

    /**
     * Generates a 512-dimensional InsightFace embedding
     * from an already aligned 112x112 face.
     */
    fun generateEmbedding(
        alignedFace: Bitmap
    ): FloatArray {

        val currentSession = session
            ?: throw IllegalStateException(
                "FaceRecognizer has not been initialized."
            )

        val bitmap = if (
            alignedFace.width == INPUT_SIZE &&
            alignedFace.height == INPUT_SIZE
        ) {
            alignedFace
        } else {
            Bitmap.createScaledBitmap(
                alignedFace,
                INPUT_SIZE,
                INPUT_SIZE,
                true
            )
        }

        val inputData = preprocess(bitmap)

        val inputTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(inputData),
            longArrayOf(
                1,
                3,
                INPUT_SIZE.toLong(),
                INPUT_SIZE.toLong()
            )
        )

        val outputs = currentSession.run(
            mapOf(
                INPUT_NAME to inputTensor
            )
        )

        inputTensor.close()

        val rawOutput = outputs[0].value

        val embedding = when (rawOutput) {

            is Array<*> -> {
                val first = rawOutput[0]

                when (first) {
                    is FloatArray -> first.copyOf()

                    is Array<*> -> {
                        first.map {
                            (it as Number).toFloat()
                        }.toFloatArray()
                    }

                    else -> {
                        throw IllegalStateException(
                            "Unexpected recognition output type: " +
                                "${first?.javaClass}"
                        )
                    }
                }
            }

            is FloatArray -> {
                rawOutput.copyOf()
            }

            else -> {
                throw IllegalStateException(
                    "Unexpected recognition output type: " +
                        "${rawOutput?.javaClass}"
                )
            }
        }

        outputs.close()

        if (embedding.size != EMBEDDING_SIZE) {
            throw IllegalStateException(
                "Expected $EMBEDDING_SIZE-dimensional embedding, " +
                    "but received ${embedding.size}."
            )
        }

        return l2Normalize(embedding)
    }

    /**
     * InsightFace recognition preprocessing.
     *
     * Input:
     *   RGB Bitmap
     *
     * Model expects:
     *   NCHW
     *   BGR
     *   (pixel - 127.5) / 127.5
     */
    private fun preprocess(
        bitmap: Bitmap
    ): FloatArray {

        val planeSize =
            INPUT_SIZE * INPUT_SIZE

        val output = FloatArray(
            3 * planeSize
        )

        var bOffset = 0
        var gOffset = planeSize
        var rOffset = planeSize * 2

        for (y in 0 until INPUT_SIZE) {

            for (x in 0 until INPUT_SIZE) {

                val pixel = bitmap.getPixel(
                    x,
                    y
                )

                val r =
                    ((pixel shr 16) and 0xFF).toFloat()

                val g =
                    ((pixel shr 8) and 0xFF).toFloat()

                val b =
                    (pixel and 0xFF).toFloat()

                output[bOffset++] =
                    (b - 127.5f) / 127.5f

                output[gOffset++] =
                    (g - 127.5f) / 127.5f

                output[rOffset++] =
                    (r - 127.5f) / 127.5f
            }
        }

        return output
    }

    private fun l2Normalize(
        embedding: FloatArray
    ): FloatArray {

        var sum = 0.0

        for (value in embedding) {
            sum += value * value
        }

        val norm = sqrt(sum)

        if (norm == 0.0) {
            throw IllegalStateException(
                "Recognition model returned a zero embedding."
            )
        }

        return FloatArray(
            embedding.size
        ) { index ->
            (embedding[index] / norm).toFloat()
        }
    }

    fun dispose() {
        session?.close()
        session = null
    }
}