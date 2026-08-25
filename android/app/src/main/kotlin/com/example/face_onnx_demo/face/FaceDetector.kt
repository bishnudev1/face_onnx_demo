package com.example.face_onnx_demo.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import java.io.ByteArrayInputStream
import java.nio.FloatBuffer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlin.math.max
import kotlin.math.min

class FaceDetector(
    private val context: Context
) {

    companion object {
        private const val TAG = "FACE_NATIVE"

        private const val MODEL_NAME = "det_500m.onnx"

        private const val INPUT_SIZE = 640

        private const val SCORE_THRESHOLD = 0.5f
        private const val NMS_THRESHOLD = 0.4f

        private val STRIDES = intArrayOf(8, 16, 32)

        // SCRFD-500M / buffalo_sc:
        // 9 outputs = 3 scales x (score + bbox + landmarks)
        //
        // Each spatial location has TWO anchors.
        private const val NUM_ANCHORS_PER_LOCATION = 2
    }

    private val environment = OrtEnvironment.getEnvironment()

    private var session: OrtSession? = null

    fun initialize() {

        android.util.Log.d(TAG, "========================================")
        android.util.Log.d(TAG, "FaceDetector.initialize() START")
        android.util.Log.d(TAG, "========================================")

        if (session != null) {
            android.util.Log.d(TAG, "Detector already initialized")
            return
        }

        android.util.Log.d(TAG, "Loading model: $MODEL_NAME")

        val modelBytes = context.assets
            .open(MODEL_NAME)
            .use { it.readBytes() }

        android.util.Log.d(
            TAG,
            "Model loaded from assets: ${modelBytes.size} bytes"
        )

        val options = OrtSession.SessionOptions()

        session = environment.createSession(
            modelBytes,
            options
        )

        android.util.Log.d(
            TAG,
            "ONNX detector session created successfully"
        )

        session?.let { ortSession ->

            android.util.Log.d(
                TAG,
                "Input count: ${ortSession.inputNames.size}"
            )

            android.util.Log.d(
                TAG,
                "Output count: ${ortSession.outputNames.size}"
            )

            android.util.Log.d(
                TAG,
                "Input names: ${ortSession.inputNames}"
            )

            android.util.Log.d(
                TAG,
                "Output names: ${ortSession.outputNames}"
            )

            for (name in ortSession.outputNames) {

                val info = ortSession.outputInfo[name]

                android.util.Log.d(
                    TAG,
                    "Output $name -> $info"
                )
            }
        }

        android.util.Log.d(
            TAG,
            "FaceDetector initialized successfully"
        )
    }

    fun detect(imageBytes: ByteArray): DetectionResult {

        val bitmap = BitmapFactory.decodeStream(
            ByteArrayInputStream(imageBytes)
        ) ?: throw IllegalArgumentException(
            "Unable to decode image."
        )

        return try {
            detect(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun detect(bitmap: Bitmap): DetectionResult {

        android.util.Log.d(TAG, "----------------------------------------")
        android.util.Log.d(TAG, "FaceDetector.detect() START")
        android.util.Log.d(TAG, "----------------------------------------")

        val currentSession = session
            ?: throw IllegalStateException(
                "FaceDetector has not been initialized."
            )

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        android.util.Log.d(
            TAG,
            "Original bitmap: ${originalWidth}x${originalHeight}"
        )

        /*
         * ============================================================
         * SCRFD PREPROCESSING
         * ============================================================
         *
         * InsightFace keeps aspect ratio.
         *
         * Example:
         *
         * 3408 x 2556
         *
         * becomes approximately:
         *
         * 640 x 480
         *
         * and the remaining area is padded with zeros.
         */

        val scale: Float
        val resizedWidth: Int
        val resizedHeight: Int

        if (originalWidth >= originalHeight) {

            scale =
                INPUT_SIZE.toFloat() /
                        originalWidth.toFloat()

            resizedWidth = INPUT_SIZE

            resizedHeight =
                (originalHeight * scale)
                    .toInt()

        } else {

            scale =
                INPUT_SIZE.toFloat() /
                        originalHeight.toFloat()

            resizedHeight = INPUT_SIZE

            resizedWidth =
                (originalWidth * scale)
                    .toInt()
        }

        android.util.Log.d(
            TAG,
            "Detection scale: $scale"
        )

        android.util.Log.d(
            TAG,
            "Resized image: ${resizedWidth}x${resizedHeight}"
        )

        /*
         * Create 640x640 padded bitmap.
         */
        val paddedBitmap = Bitmap.createBitmap(
            INPUT_SIZE,
            INPUT_SIZE,
            Bitmap.Config.ARGB_8888
        )

        paddedBitmap.eraseColor(Color.BLACK)

        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            resizedWidth,
            resizedHeight,
            true
        )

        val canvas = Canvas(paddedBitmap)

        /*
         * InsightFace's preprocessing uses top-left padding.
         */
        canvas.drawBitmap(
            resizedBitmap,
            0f,
            0f,
            null
        )

        if (resizedBitmap !== bitmap) {
            resizedBitmap.recycle()
        }

        android.util.Log.d(
            TAG,
            "Padded bitmap: ${paddedBitmap.width}x${paddedBitmap.height}"
        )

        /*
         * ============================================================
         * CREATE INPUT TENSOR
         * ============================================================
         */

        val input = createInputTensor(
            paddedBitmap
        )

        android.util.Log.d(
            TAG,
            "Input tensor created: ${input.info}"
        )

        /*
         * ============================================================
         * ONNX INFERENCE
         * ============================================================
         */

        val outputs = currentSession.run(
            mapOf(
                "input.1" to input
            )
        )

        android.util.Log.d(
            TAG,
            "ONNX inference completed"
        )

        android.util.Log.d(
            TAG,
            "Returned outputs: ${outputs.size()}"
        )

        input.close()
        paddedBitmap.recycle()

        /*
         * ============================================================
         * SCRFD OUTPUTS
         * ============================================================
         *
         * Output order:
         *
         * 0 -> scores stride 8
         * 1 -> scores stride 16
         * 2 -> scores stride 32
         *
         * 3 -> boxes stride 8
         * 4 -> boxes stride 16
         * 5 -> boxes stride 32
         *
         * 6 -> landmarks stride 8
         * 7 -> landmarks stride 16
         * 8 -> landmarks stride 32
         */

        val detections =
            mutableListOf<FaceDetection>()

        processLevel(
            outputs = outputs,
            scoreIndex = 0,
            boxIndex = 3,
            keypointIndex = 6,
            stride = STRIDES[0],
            detections = detections
        )

        processLevel(
            outputs = outputs,
            scoreIndex = 1,
            boxIndex = 4,
            keypointIndex = 7,
            stride = STRIDES[1],
            detections = detections
        )

        processLevel(
            outputs = outputs,
            scoreIndex = 2,
            boxIndex = 5,
            keypointIndex = 8,
            stride = STRIDES[2],
            detections = detections
        )

        outputs.close()

        android.util.Log.d(
            TAG,
            "Total detections before NMS: ${detections.size}"
        )

        if (detections.isEmpty()) {

            throw IllegalStateException(
                "No face detected."
            )
        }

        /*
         * ============================================================
         * NMS
         * ============================================================
         */

        val selected = nonMaximumSuppression(
            detections,
            NMS_THRESHOLD
        )

        android.util.Log.d(
            TAG,
            "Detections after NMS: ${selected.size}"
        )

        val best = selected.maxByOrNull {
            it.score
        } ?: throw IllegalStateException(
            "No face remained after NMS."
        )

        android.util.Log.d(
            TAG,
            "Best face score: ${best.score}"
        )

        android.util.Log.d(
            TAG,
            "Best face box: " +
                    "(${best.x1}, ${best.y1}) -> " +
                    "(${best.x2}, ${best.y2})"
        )

        /*
         * ============================================================
         * MAP 640x640 DETECTOR COORDINATES BACK TO ORIGINAL IMAGE
         * ============================================================
         */

        return DetectionResult(
            detection = best.scaleToOriginalImage(
                scale = scale,
                originalWidth = originalWidth.toFloat(),
                originalHeight = originalHeight.toFloat()
            )
        )
    }

    /**
     * Process one SCRFD scale.
     *
     * IMPORTANT:
     *
     * For det_500m there are TWO anchors per spatial location.
     *
     * Therefore:
     *
     * stride 8:
     *   80 x 80 x 2 = 12800
     *
     * stride 16:
     *   40 x 40 x 2 = 3200
     *
     * stride 32:
     *   20 x 20 x 2 = 800
     */
   private fun processLevel(
    outputs: OrtSession.Result,
    scoreIndex: Int,
    boxIndex: Int,
    keypointIndex: Int,
    stride: Int,
    detections: MutableList<FaceDetection>
) {

    android.util.Log.d(
        TAG,
        "========================================"
    )

    android.util.Log.d(
        TAG,
        "Processing SCRFD level stride=$stride"
    )

    android.util.Log.d(
        TAG,
        "scoreIndex=$scoreIndex " +
                "boxIndex=$boxIndex " +
                "keypointIndex=$keypointIndex"
    )

    /*
     * ONNX Runtime returns these tensors as flattened
     * FloatArrays.
     *
     * SCRFD outputs:
     *
     * score     -> [N, 1]
     * box       -> [N, 4]
     * keypoints -> [N, 10]
     *
     * flattenFloatOutput() handles the different Java/Kotlin
     * array representations safely.
     */

    val scoreArray =
        flattenFloatOutput(
            outputs[scoreIndex].value
        )

    val boxArray =
        flattenFloatOutput(
            outputs[boxIndex].value
        )

    val keypointArray =
        flattenFloatOutput(
            outputs[keypointIndex].value
        )

    android.util.Log.d(
        TAG,
        "scoreArray size=${scoreArray.size}"
    )

    android.util.Log.d(
        TAG,
        "boxArray size=${boxArray.size}"
    )

    android.util.Log.d(
        TAG,
        "keypointArray size=${keypointArray.size}"
    )

    /*
     * SCRFD input is 640x640.
     *
     * Feature-map sizes:
     *
     * stride 8  -> 80 x 80
     * stride 16 -> 40 x 40
     * stride 32 -> 20 x 20
     */

    val featureMapWidth =
        INPUT_SIZE / stride

    val featureMapHeight =
        INPUT_SIZE / stride

    val locations =
        featureMapWidth * featureMapHeight

    val expectedAnchors =
        locations * NUM_ANCHORS_PER_LOCATION

    android.util.Log.d(
        TAG,
        "Feature map: ${featureMapWidth}x${featureMapHeight}"
    )

    android.util.Log.d(
        TAG,
        "Locations: $locations"
    )

    android.util.Log.d(
        TAG,
        "Anchors per location: " +
                "$NUM_ANCHORS_PER_LOCATION"
    )

    android.util.Log.d(
        TAG,
        "Expected anchors: $expectedAnchors"
    )

    /*
     * Validate model output sizes.
     */

    require(scoreArray.size >= expectedAnchors) {
        "Invalid score output size: " +
                "${scoreArray.size}, " +
                "expected $expectedAnchors"
    }

    require(boxArray.size >= expectedAnchors * 4) {
        "Invalid box output size: " +
                "${boxArray.size}, " +
                "expected ${expectedAnchors * 4}"
    }

    require(
        keypointArray.size >= expectedAnchors * 10
    ) {
        "Invalid landmark output size: " +
                "${keypointArray.size}, " +
                "expected ${expectedAnchors * 10}"
    }

    var passedThreshold = 0
    var maximumScore = 0f

    /*
     * Each spatial location contains
     * NUM_ANCHORS_PER_LOCATION anchors.
     */

    for (index in 0 until expectedAnchors) {

        val score =
            scoreArray[index]

        if (score > maximumScore) {
            maximumScore = score
        }

        /*
         * Ignore weak detections.
         */

        if (score < SCORE_THRESHOLD) {
            continue
        }

        passedThreshold++

        /*
         * Convert flattened anchor index into:
         *
         * location index
         * anchor number
         * row
         * column
         */

        val locationIndex =
            index / NUM_ANCHORS_PER_LOCATION

        val anchorNumber =
            index % NUM_ANCHORS_PER_LOCATION

        val row =
            locationIndex / featureMapWidth

        val col =
            locationIndex % featureMapWidth

        /*
         * SCRFD anchor center.
         *
         * Both anchors at a spatial location share
         * the same center.
         */

        val anchorX =
            col * stride.toFloat()

        val anchorY =
            row * stride.toFloat()

        /*
         * ------------------------------------------------
         * BOUNDING BOX
         * ------------------------------------------------
         *
         * SCRFD outputs:
         *
         * left
         * top
         * right
         * bottom
         *
         * These distances are relative to the anchor
         * center and must be multiplied by stride.
         */

        val boxOffset =
            index * 4

        val left =
            boxArray[boxOffset] * stride

        val top =
            boxArray[boxOffset + 1] * stride

        val right =
            boxArray[boxOffset + 2] * stride

        val bottom =
            boxArray[boxOffset + 3] * stride

        val x1 =
            anchorX - left

        val y1 =
            anchorY - top

        val x2 =
            anchorX + right

        val y2 =
            anchorY + bottom

        /*
         * ------------------------------------------------
         * FIVE LANDMARKS
         * ------------------------------------------------
         *
         * 10 values:
         *
         * x1, y1
         * x2, y2
         * x3, y3
         * x4, y4
         * x5, y5
         */

        val kpsOffset =
            index * 10

        val landmarks =
            Array(5) { pointIndex ->

                val x =
                    anchorX +
                            keypointArray[
                                kpsOffset +
                                        pointIndex * 2
                            ] * stride

                val y =
                    anchorY +
                            keypointArray[
                                kpsOffset +
                                        pointIndex * 2 +
                                        1
                            ] * stride

                Point(
                    x = x,
                    y = y
                )
            }

        /*
         * Add detection.
         */

        detections.add(
            FaceDetection(
                score = score,

                x1 = x1.coerceIn(
                    0f,
                    INPUT_SIZE.toFloat()
                ),

                y1 = y1.coerceIn(
                    0f,
                    INPUT_SIZE.toFloat()
                ),

                x2 = x2.coerceIn(
                    0f,
                    INPUT_SIZE.toFloat()
                ),

                y2 = y2.coerceIn(
                    0f,
                    INPUT_SIZE.toFloat()
                ),

                landmarks = landmarks
            )
        )
    }

    /*
     * Summary logging only.
     *
     * Do NOT log every detection because SCRFD
     * can generate many detections.
     */

    android.util.Log.d(
        TAG,
        "Maximum score at stride $stride: " +
                "$maximumScore, " +
                "threshold=$SCORE_THRESHOLD"
    )

    android.util.Log.d(
        TAG,
        "Faces above threshold at stride $stride: " +
                "$passedThreshold"
    )
}

    /**
     * SCRFD input:
     *
     * [1, 3, 640, 640]
     *
     * normalization:
     *
     * (pixel - 127.5) / 128.0
     */
    private fun createInputTensor(
        bitmap: Bitmap
    ): OnnxTensor {

        val input = FloatArray(
            1 *
                    3 *
                    INPUT_SIZE *
                    INPUT_SIZE
        )

        var offsetR = 0

        var offsetG =
            INPUT_SIZE *
                    INPUT_SIZE

        var offsetB =
            INPUT_SIZE *
                    INPUT_SIZE *
                    2

        for (y in 0 until INPUT_SIZE) {

            for (x in 0 until INPUT_SIZE) {

                val pixel =
                    bitmap.getPixel(x, y)

                val r =
                    ((pixel shr 16) and 0xFF)
                        .toFloat()

                val g =
                    ((pixel shr 8) and 0xFF)
                        .toFloat()

                val b =
                    (pixel and 0xFF)
                        .toFloat()

                /*
                 * IMPORTANT:
                 *
                 * InsightFace SCRFD:
                 *
                 * (pixel - 127.5) / 128.0
                 */
                input[offsetR++] =
                    (r - 127.5f) / 128.0f

                input[offsetG++] =
                    (g - 127.5f) / 128.0f

                input[offsetB++] =
                    (b - 127.5f) / 128.0f
            }
        }

        return OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(
                1L,
                3L,
                INPUT_SIZE.toLong(),
                INPUT_SIZE.toLong()
            )
        )
    }

    private fun nonMaximumSuppression(
        detections: List<FaceDetection>,
        threshold: Float
    ): List<FaceDetection> {

        val sorted =
            detections
                .sortedByDescending {
                    it.score
                }
                .toMutableList()

        val selected =
            mutableListOf<FaceDetection>()

        while (sorted.isNotEmpty()) {

            val current =
                sorted.removeAt(0)

            selected.add(current)

            val remaining =
                mutableListOf<FaceDetection>()

            for (candidate in sorted) {

                if (
                    calculateIoU(
                        current,
                        candidate
                    ) <= threshold
                ) {
                    remaining.add(candidate)
                }
            }

            sorted.clear()
            sorted.addAll(remaining)
        }

        return selected
    }

    private fun flattenFloatOutput(value: Any?): FloatArray {
    return when (value) {

        is FloatArray -> {
            value
        }

        is Array<*> -> {
            val result = ArrayList<Float>()

            for (item in value) {
                when (item) {

                    is FloatArray -> {
                        for (v in item) {
                            result.add(v)
                        }
                    }

                    is Array<*> -> {
                        result.addAll(
                            flattenFloatOutput(item).toList()
                        )
                    }

                    is Number -> {
                        result.add(item.toFloat())
                    }

                    else -> {
                        throw IllegalArgumentException(
                            "Unsupported nested ONNX output type: ${item?.javaClass}"
                        )
                    }
                }
            }

            result.toFloatArray()
        }

        else -> {
            throw IllegalArgumentException(
                "Unsupported ONNX output type: ${value?.javaClass}"
            )
        }
    }
}

    private fun calculateIoU(
        a: FaceDetection,
        b: FaceDetection
    ): Float {

        val intersectionLeft =
            max(a.x1, b.x1)

        val intersectionTop =
            max(a.y1, b.y1)

        val intersectionRight =
            min(a.x2, b.x2)

        val intersectionBottom =
            min(a.y2, b.y2)

        val intersectionWidth =
            max(
                0f,
                intersectionRight -
                        intersectionLeft
            )

        val intersectionHeight =
            max(
                0f,
                intersectionBottom -
                        intersectionTop
            )

        val intersectionArea =
            intersectionWidth *
                    intersectionHeight

        val areaA =
            max(
                0f,
                a.x2 - a.x1
            ) *
                    max(
                        0f,
                        a.y2 - a.y1
                    )

        val areaB =
            max(
                0f,
                b.x2 - b.x1
            ) *
                    max(
                        0f,
                        b.y2 - b.y1
                    )

        val union =
            areaA +
                    areaB -
                    intersectionArea

        if (union <= 0f) {
            return 0f
        }

        return intersectionArea / union
    }

    fun dispose() {

        android.util.Log.d(
            TAG,
            "Disposing FaceDetector"
        )

        session?.close()
        session = null
    }
}

data class Point(
    val x: Float,
    val y: Float
)

data class FaceDetection(
    val score: Float,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val landmarks: Array<Point>
) {

    /*
     * Convert coordinates from the padded 640x640
     * detector image back to the original image.
     */
    fun scaleToOriginalImage(
        scale: Float,
        originalWidth: Float,
        originalHeight: Float
    ): FaceDetection {

        val inverseScale =
            1f / scale

        return FaceDetection(

            score = score,

            x1 = (x1 * inverseScale)
                .coerceIn(
                    0f,
                    originalWidth
                ),

            y1 = (y1 * inverseScale)
                .coerceIn(
                    0f,
                    originalHeight
                ),

            x2 = (x2 * inverseScale)
                .coerceIn(
                    0f,
                    originalWidth
                ),

            y2 = (y2 * inverseScale)
                .coerceIn(
                    0f,
                    originalHeight
                ),

            landmarks =
                landmarks.map {

                    Point(
                        x = (
                            it.x *
                                    inverseScale
                            ).coerceIn(
                                0f,
                                originalWidth
                            ),

                        y = (
                            it.y *
                                    inverseScale
                            ).coerceIn(
                                0f,
                                originalHeight
                            )
                    )

                }.toTypedArray()
        )
    }
}

data class DetectionResult(
    val detection: FaceDetection
)