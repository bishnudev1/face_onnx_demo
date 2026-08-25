package com.example.face_onnx_demo.face

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.sqrt

class FaceAlignment {

    companion object {
        private const val OUTPUT_SIZE = 112

        /*
         * InsightFace / ArcFace 5-point reference landmarks.
         *
         * Order:
         * 0 = left eye
         * 1 = right eye
         * 2 = nose
         * 3 = left mouth
         * 4 = right mouth
         */
        private val REFERENCE_POINTS = arrayOf(
            Point(38.2946f, 51.6963f),
            Point(73.5318f, 51.5014f),
            Point(56.0252f, 71.7366f),
            Point(41.5493f, 92.3655f),
            Point(70.7299f, 92.2041f)
        )
    }

    /**
     * Aligns the detected face into the 112x112 ArcFace input format.
     */
    fun align(
        bitmap: Bitmap,
        landmarks: Array<Point>
    ): Bitmap {

        require(landmarks.size == 5) {
            "Exactly 5 facial landmarks are required."
        }

        val transform = estimateSimilarityTransform(
            source = landmarks,
            destination = REFERENCE_POINTS
        )

        val output = Bitmap.createBitmap(
            OUTPUT_SIZE,
            OUTPUT_SIZE,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(
            bitmap,
            transform,
            paint
        )

        return output
    }

    /**
     * Estimates a similarity transform using the five facial landmarks.
     *
     * This preserves:
     * - translation
     * - rotation
     * - uniform scale
     *
     * It does not introduce arbitrary perspective distortion.
     */
    private fun estimateSimilarityTransform(
        source: Array<Point>,
        destination: Array<Point>
    ): Matrix {

        val sourceCenter = calculateCenter(source)
        val destinationCenter = calculateCenter(destination)

        var sourceScale = 0.0
        var destinationScale = 0.0

        for (i in source.indices) {

            val sx =
                source[i].x - sourceCenter.x

            val sy =
                source[i].y - sourceCenter.y

            val dx =
                destination[i].x - destinationCenter.x

            val dy =
                destination[i].y - destinationCenter.y

            sourceScale +=
                sx * sx + sy * sy

            destinationScale +=
                dx * dx + dy * dy
        }

        sourceScale = sqrt(
            sourceScale / source.size
        )

        destinationScale = sqrt(
            destinationScale / destination.size
        )

        if (sourceScale == 0.0) {
            throw IllegalArgumentException(
                "Invalid source landmarks."
            )
        }

        val scale =
            destinationScale / sourceScale

        /*
         * Estimate rotation using the centered
         * landmark coordinates.
         */
        var dot = 0.0
        var cross = 0.0

        for (i in source.indices) {

            val sx =
                source[i].x - sourceCenter.x

            val sy =
                source[i].y - sourceCenter.y

            val dx =
                destination[i].x - destinationCenter.x

            val dy =
                destination[i].y - destinationCenter.y

            dot +=
                sx * dx + sy * dy

            cross +=
                sx * dy - sy * dx
        }

        val angle =
            Math.toDegrees(
                kotlin.math.atan2(
                    cross,
                    dot
                )
            ).toFloat()

        /*
         * Build the transform around the source center,
         * then move the transformed face to the destination
         * center.
         */
        val matrix = Matrix()

        matrix.postTranslate(
            -sourceCenter.x,
            -sourceCenter.y
        )

        matrix.postScale(
            scale.toFloat(),
            scale.toFloat()
        )

        matrix.postRotate(angle)

        matrix.postTranslate(
            destinationCenter.x,
            destinationCenter.y
        )

        return matrix
    }

    private fun calculateCenter(
        points: Array<Point>
    ): Point {

        var x = 0f
        var y = 0f

        for (point in points) {
            x += point.x
            y += point.y
        }

        return Point(
            x = x / points.size,
            y = y / points.size
        )
    }
}