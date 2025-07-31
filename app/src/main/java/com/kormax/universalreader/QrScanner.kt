// You might place this in a separate file like QrScanner.kt or directly in MainActivity.kt if it's only used there.
// (Ensure all necessary imports for CameraX, MLKit, Compose are present)

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QrCodeScannerView(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit, // Callback with the scanned string
    onCloseScanner: () -> Unit // Callback to close the scanner view
) {
    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
        remember { ProcessCameraProvider.getInstance(localContext) }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(localContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                // Handle permission denial, perhaps show a message and call onCloseScanner
                Log.e("QrScannerView", "Camera permission denied.")
                onCloseScanner()
            }
        }
    )

    LaunchedEffect(key1 = true) { // Use Unit or true if you want it to run once
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        var hasScanned by remember { mutableStateOf(false) } // Prevent multiple callbacks for the same QR

        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    val previewView = PreviewView(context).apply {
                        this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE) // Only look for QR codes
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                                if (hasScanned) { // If already scanned, ignore further frames
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty() && !hasScanned) {
                                                barcodes.firstOrNull()?.rawValue?.let { scannedValue ->
                                                    hasScanned = true // Set flag to true
                                                    onQrCodeScanned(scannedValue) // Call the callback
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("QrScannerView", "Barcode scanning failed", e)
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close() // ALWAYS close the ImageProxy
                                        }
                                } else {
                                    imageProxy.close() // Close if mediaImage is null
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll() // Unbind use cases before rebinding
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("QrScannerView", "Use case binding failed", exc)
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            // Button to close the scanner manually
            Button(
                onClick = onCloseScanner,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
            ) {
                Text("Cancel Scan")
            }
        }
    } else {
        // Optional: Show a message if permission is still not granted or being requested.
        // Or this composable might just not render anything significant until permission is granted.
        // The LaunchedEffect handles requesting permission. If it's permanently denied, onCloseScanner
        // should be called by the permissionLauncher's onResult.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Requesting camera permission...")
        }
    }
}