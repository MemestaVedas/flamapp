package com.example.cvgl

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.cvgl.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var renderer: CVGLRenderer

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize OpenGL Renderer
        renderer = CVGLRenderer()
        binding.glSurfaceView.setEGLContextClientVersion(2)
        binding.glSurfaceView.setRenderer(renderer)
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { image ->
                processImage(image)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(image: ImageProxy) {
        // Get the Y plane (grayscale)
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = image.width
        val height = image.height

        // Create OpenCV Mats
        // Note: Recreating Mats every frame is expensive. In production, reuse them.
        // For this assessment, we'll keep it simple but safe.
        // YUV_420_888 Y-plane is effectively CV_8UC1 (Grayscale)
        val yMat = org.opencv.core.Mat(height, width, org.opencv.core.CvType.CV_8UC1)
        yMat.put(0, 0, data)
        
        val processedMat = org.opencv.core.Mat()

        // Process in Native
        nativeProcessFrame(yMat.nativeObjAddr, processedMat.nativeObjAddr)

        // Convert back to byte array for OpenGL
        // OpenGL needs raw pixel data. Canny returns single channel.
        // We might want to convert to RGBA for simpler OpenGL rendering or use a LUMINANCE texture.
        // Let's stick to single channel for efficiency and handle it in shader.
        
        val processedData = ByteArray(processedMat.total().toInt() * processedMat.channels())
        processedMat.get(0, 0, processedData)

        // Update Renderer
        renderer.updateTexture(processedData, width, height)
        binding.glSurfaceView.requestRender()

        // Release Mats
        yMat.release()
        processedMat.release()
        
        image.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "MainActivity"
        
        // Load native library
        init {
            System.loadLibrary("cvgl")
            System.loadLibrary("opencv_java4") // Load OpenCV Java wrapper if needed, or just native
        }
    }
    
    // Native methods
    external fun stringFromJNI(): String
    external fun nativeProcessFrame(matAddrInput: Long, matAddrOutput: Long)
}
