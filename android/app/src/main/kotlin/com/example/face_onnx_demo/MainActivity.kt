package com.example.face_onnx_demo

import android.os.Bundle
import com.example.face_onnx_demo.FaceMethodChannel
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {

    private var faceMethodChannel: FaceMethodChannel? = null

    override fun configureFlutterEngine(
        flutterEngine: FlutterEngine
    ) {
        super.configureFlutterEngine(flutterEngine)

        faceMethodChannel = FaceMethodChannel(
            context = this,
            flutterEngine = flutterEngine
        )
    }

    override fun onDestroy() {
        faceMethodChannel = null
        super.onDestroy()
    }
}