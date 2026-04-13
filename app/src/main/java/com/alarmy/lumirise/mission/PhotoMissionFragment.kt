package com.alarmy.lumirise.mission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.alarmy.lumirise.R
import com.alarmy.lumirise.databinding.FragmentPhotoMissionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class PhotoMissionFragment : BaseMissionFragment() {

    private var _photoBinding: FragmentPhotoMissionBinding? = null
    private val photoBinding get() = _photoBinding!!

    private lateinit var photoMatcher: PhotoMatcher
    private var photoMode: PhotoMissionMode = PhotoMissionMode.VERIFY

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private val cameraOpenCloseLock = Semaphore(1)

    private var autoCaptureEnabled = true
    private var isProcessingPhoto = false

    companion object {
        private const val ARG_PHOTO_MODE = "photo_mode"

        fun newInstance(mode: PhotoMissionMode = PhotoMissionMode.VERIFY): PhotoMissionFragment {
            return PhotoMissionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_MODE, mode.name)
                }
            }
        }
    }

    override val missionType = MissionType.PHOTO

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            showPermissionDeniedUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val modeStr = it.getString(ARG_PHOTO_MODE, PhotoMissionMode.VERIFY.name)
            photoMode = PhotoMissionMode.valueOf(modeStr)
        }
        photoMatcher = PhotoMatcher(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _photoBinding = FragmentPhotoMissionBinding.inflate(inflater, container, false)

        binding.missionContentContainer.addView(photoBinding.root)

        return binding.root
    }

    override fun setupMissionUI() {
        setupUIForMode()
        setupCaptureButton()
        setupAutoCaptureToggle()
        checkCameraPermission()
    }

    private fun setupUIForMode() {
        when (photoMode) {
            PhotoMissionMode.SETUP -> {
                photoBinding.photoInstructionText.setText(R.string.photo_instruction_setup)
                photoBinding.captureButton.setText(R.string.capture_photo)
                photoBinding.setupHint.visibility = View.VISIBLE
                photoBinding.autoCaptureToggle.visibility = View.GONE
            }
            PhotoMissionMode.VERIFY -> {
                photoBinding.photoInstructionText.setText(R.string.photo_instruction)
                photoBinding.captureButton.setText(R.string.capture_photo)
                photoBinding.setupHint.visibility = View.GONE
                photoBinding.autoCaptureToggle.visibility = View.VISIBLE
                updateAutoCaptureButton()
            }
        }

        MissionManager.getInstance().updateProgress(0, "")
    }

    private fun setupCaptureButton() {
        photoBinding.captureButton.setOnClickListener {
            if (!isProcessingPhoto) {
                capturePhoto()
            }
        }
    }

    private fun setupAutoCaptureToggle() {
        photoBinding.autoCaptureToggle.setOnClickListener {
            autoCaptureEnabled = !autoCaptureEnabled
            updateAutoCaptureButton()
        }
    }

    private fun updateAutoCaptureButton() {
        if (autoCaptureEnabled) {
            photoBinding.autoCaptureToggle.setText(R.string.auto_capture_on)
        } else {
            photoBinding.autoCaptureToggle.setText(R.string.auto_capture_off)
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    override fun getHintText(): String {
        return when (photoMode) {
            PhotoMissionMode.SETUP -> getString(R.string.photo_setup_hint)
            PhotoMissionMode.VERIFY -> "Take a photo matching your target"
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            if (autoCaptureEnabled && photoMode == PhotoMissionMode.VERIFY && !isProcessingPhoto) {
                checkForAutoCapture()
            }
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (!photoBinding.cameraPreview.isAvailable) {
            photoBinding.cameraPreview.surfaceTextureListener = surfaceTextureListener
            return
        }

        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = getBackCameraId(cameraManager)

        if (cameraId == null) {
            Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    activity?.finish()
                }
            }, backgroundHandler)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun getBackCameraId(cameraManager: CameraManager): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return cameraManager.cameraIdList.firstOrNull()
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = photoBinding.cameraPreview.surfaceTexture ?: return
            val previewWidth = photoBinding.cameraPreview.width
            val previewHeight = photoBinding.cameraPreview.height
            texture.setDefaultBufferSize(previewWidth, previewHeight)

            val previewSurface = Surface(texture)
            val previewSize = getOptimalPreviewSize(previewWidth, previewHeight)

            imageReader = ImageReader.newInstance(
                previewSize.width,
                previewSize.height,
                ImageFormat.JPEG,
                2
            )

            val surfaces = listOf(previewSurface, imageReader!!.surface)

            cameraDevice?.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        startPreview(previewSurface)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show()
                    }
                },
                backgroundHandler
            )

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getOptimalPreviewSize(width: Int, height: Int): Size {
        val targetRatio = width.toFloat() / height.toFloat()
        val sizes = try {
            val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = getBackCameraId(cameraManager) ?: return Size(1920, 1080)
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(ImageFormat.JPEG) ?: arrayOf()
        } catch (e: Exception) {
            arrayOf<Size>()
        }

        var optimalSize = Size(1920, 1080)
        var minDiff = Float.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toFloat() / size.height.toFloat()
            if (kotlin.math.abs(ratio - targetRatio) < minDiff) {
                optimalSize = size
                minDiff = kotlin.math.abs(ratio - targetRatio)
            }
        }

        return optimalSize
    }

    private fun startPreview(surface: Surface) {
        try {
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )?.apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            } ?: return

            captureSession?.setRepeatingRequest(
                previewRequestBuilder.build(),
                null,
                backgroundHandler
            )

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private var lastAutoCaptureCheck = 0L
    private var lastAutoCaptureBitmap: Bitmap? = null

    private fun checkForAutoCapture() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAutoCaptureCheck < 2000) return
        lastAutoCaptureCheck = currentTime

        val bitmap = photoBinding.cameraPreview.bitmap ?: return
        lastAutoCaptureBitmap = bitmap

        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                photoMatcher.comparePhotos(bitmap)
            }

            when (result) {
                is PhotoMatcher.MatchResult.Match -> {
                    if (result.similarity >= 85) {
                        isProcessingPhoto = true
                        showMatchResult(result.similarity)
                        completePhotoMission()
                    }
                }
                else -> {}
            }
        }
    }

    private fun capturePhoto() {
        if (isProcessingPhoto) return
        isProcessingPhoto = true

        showProcessingUI()

        try {
            val captureBuilder = cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
            )?.apply {
                imageReader?.surface?.let { addTarget(it) }
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation())
            } ?: run {
                hideProcessingUI()
                isProcessingPhoto = false
                return
            }

            captureSession?.capture(
                captureBuilder.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        processCapturedImage()
                    }
                },
                backgroundHandler
            )

        } catch (e: CameraAccessException) {
            e.printStackTrace()
            hideProcessingUI()
            isProcessingPhoto = false
            Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getJpegOrientation(): Int {
        val rotation = activity?.windowManager?.defaultDisplay?.rotation ?: 0
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager

        return try {
            val cameraId = getBackCameraId(cameraManager) ?: return 0
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

            when (rotation) {
                Surface.ROTATION_0 -> sensorOrientation
                Surface.ROTATION_90 -> (sensorOrientation + 270) % 360
                Surface.ROTATION_180 -> (sensorOrientation + 180) % 360
                Surface.ROTATION_270 -> (sensorOrientation + 90) % 360
                else -> sensorOrientation
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun processCapturedImage() {
        val reader = imageReader ?: return

        val image = reader.acquireLatestImage()
        if (image == null) {
            hideProcessingUI()
            isProcessingPhoto = false
            return
        }

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        image.close()

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap == null) {
            hideProcessingUI()
            isProcessingPhoto = false
            return
        }

        lifecycleScope.launch {
            handlePhotoCapture(bitmap)
        }
    }

    private suspend fun handlePhotoCapture(bitmap: Bitmap) = withContext(Dispatchers.Main) {
        when (photoMode) {
            PhotoMissionMode.SETUP -> {
                handleSetupCapture(bitmap)
            }
            PhotoMissionMode.VERIFY -> {
                handleVerifyCapture(bitmap)
            }
        }
    }

    private suspend fun handleSetupCapture(bitmap: Bitmap) = withContext(Dispatchers.Main) {
        val rotatedBitmap = rotateBitmap(bitmap, 90f)

        val success = withContext(Dispatchers.IO) {
            photoMatcher.saveTargetPhoto(rotatedBitmap)
        }

        if (success) {
            Toast.makeText(context, R.string.photo_setup_complete, Toast.LENGTH_SHORT).show()
            photoBinding.statusMessage.visibility = View.VISIBLE
            photoBinding.statusMessage.setText(R.string.photo_setup_complete)
            MissionManager.getInstance().completeMission()
        } else {
            Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show()
        }

        hideProcessingUI()
        isProcessingPhoto = false
    }

    private suspend fun handleVerifyCapture(bitmap: Bitmap) = withContext(Dispatchers.Main) {
        val rotatedBitmap = rotateBitmap(bitmap, 90f)

        val result = withContext(Dispatchers.Default) {
            photoMatcher.comparePhotos(rotatedBitmap)
        }

        when (result) {
            is PhotoMatcher.MatchResult.Match -> {
                showMatchResult(result.similarity)
                completePhotoMission()
            }
            is PhotoMatcher.MatchResult.NoMatch -> {
                showNoMatchResult(result.similarity)
                hideProcessingUI()
                isProcessingPhoto = false
            }
            is PhotoMatcher.MatchResult.NoTarget -> {
                Toast.makeText(context, R.string.target_photo_missing, Toast.LENGTH_LONG).show()
                hideProcessingUI()
                isProcessingPhoto = false
            }
            is PhotoMatcher.MatchResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                hideProcessingUI()
                isProcessingPhoto = false
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun showProcessingUI() {
        photoBinding.captureButton.isEnabled = false
        photoBinding.statusMessage.visibility = View.VISIBLE
        photoBinding.statusMessage.setText(R.string.matching_photo)
        photoBinding.similarityIndicator.visibility = View.VISIBLE
        photoBinding.similarityText.visibility = View.VISIBLE
    }

    private fun hideProcessingUI() {
        photoBinding.captureButton.isEnabled = true
        photoBinding.similarityIndicator.visibility = View.GONE
        photoBinding.similarityText.visibility = View.GONE
    }

    private fun showMatchResult(similarity: Int) {
        photoBinding.similarityIndicator.visibility = View.VISIBLE
        photoBinding.similarityText.visibility = View.VISIBLE
        photoBinding.similarityText.text = getString(R.string.similarity, similarity)
        photoBinding.similarityIndicator.progress = similarity
        photoBinding.statusMessage.visibility = View.VISIBLE
        photoBinding.statusMessage.setText(R.string.photo_matched)
    }

    private fun showNoMatchResult(similarity: Int) {
        photoBinding.similarityIndicator.visibility = View.VISIBLE
        photoBinding.similarityText.visibility = View.VISIBLE
        photoBinding.similarityText.text = getString(R.string.similarity, similarity)
        photoBinding.similarityIndicator.progress = similarity
        photoBinding.statusMessage.visibility = View.VISIBLE
        photoBinding.statusMessage.setText(R.string.photo_not_matched)
    }

    private fun completePhotoMission() {
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            MissionManager.getInstance().completeMission()
        }, 1000)
    }

    private fun showPermissionDeniedUI() {
        photoBinding.permissionOverlay.visibility = View.VISIBLE
        photoBinding.permissionText.visibility = View.VISIBLE
        photoBinding.captureButton.isEnabled = false
        photoBinding.autoCaptureToggle.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (photoBinding.cameraPreview.isAvailable) {
            openCamera()
        } else {
            photoBinding.cameraPreview.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    override fun onDestroyView() {
        closeCamera()
        photoBinding.root.parent?.let {
            if (it is ViewGroup) {
                it.removeView(photoBinding.root)
            }
        }
        _photoBinding = null
        super.onDestroyView()
    }
}
