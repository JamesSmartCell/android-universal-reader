// AddPassActivity.kt
package com.kormax.universalreader

// Removed jwtGen import for now, as it's not used in this step
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kormax.universalreader.ui.theme.UniversalReaderTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AddPassActivity : ComponentActivity() {

    // NfcAdapter can be initialized but its logic will be separated from eth address capture for now
    private var nfcAdapter: NfcAdapter? = null

    companion object {
        const val TAG = "AddPassActivity"
        private const val ACTIVITY_TIMEOUT_MS = 30_000L // Timeout for the activity itself
    }

    // State for the Ethereum address (captured here, but not yet used for pass gen)
    private var capturedEthAddress by mutableStateOf<String?>(null)

    // Camera related
    private lateinit var cameraExecutor: ExecutorService
    private var showQrScannerView by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize NFC Adapter for completeness, but its active usage for HCE is deferred
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Log.w(TAG, "NFC is not available on this device. Address capture will still work.")
            // Not necessarily an error for *this step*, but good to log
        } else if (!nfcAdapter!!.isEnabled) {
            Log.w(TAG, "NFC is disabled. Address capture will still work.")
        }

        setContent {
            UniversalReaderTheme {
                // This state is local to the composable for UI display and input
                var displayedEthAddress by remember { mutableStateOf(capturedEthAddress ?: "") }
                var uiMessage by remember { mutableStateOf("Scan or enter Ethereum Address.") }

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        showQrScannerView = true
                    } else {
                        Log.e(TAG, "Camera permission denied.")
                        uiMessage = "Camera permission is required to scan QR codes."
                    }
                }

                // Effect to update UI message when capturedEthAddress changes
                LaunchedEffect(capturedEthAddress) {
                    if (capturedEthAddress != null) {
                        uiMessage = "Address captured: ${capturedEthAddress}"
                        Log.i(TAG, "Ethereum Address State Updated: $capturedEthAddress")
                    } else {
                        uiMessage = "Scan or enter Ethereum Address."
                    }
                }


                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            title = { Text("Capture Ethereum Address") }
                        )
                    }
                ) { innerPadding ->
                    if (showQrScannerView) {
                        QrCodeScannerComposable(
                            modifier = Modifier.padding(innerPadding),
                            onQrCodeScanned = { scannedAddress ->
                                // Update the Activity's state with the scanned address
                                capturedEthAddress = scannedAddress
                                // Update the Composable's display state
                                displayedEthAddress = scannedAddress
                                showQrScannerView = false
                                Log.i(TAG, "QR Scanned, Address: $scannedAddress")
                            },
                            onClose = { showQrScannerView = false }
                        )
                    } else {
                        CaptureAddressScreenContent( // Renamed for clarity for this step
                            modifier = Modifier.padding(innerPadding),
                            uiMessage = uiMessage,
                            currentAddress = displayedEthAddress,
                            onAddressChanged = { newAddress ->
                                displayedEthAddress = newAddress
                                // User can manually type. Update the capturedEthAddress
                                // after some basic validation if desired, or directly.
                                if (isValidEthAddress(newAddress)) {
                                    capturedEthAddress = newAddress
                                } else {
                                    // If invalid, you might want to clear capturedEthAddress
                                    // or keep it as is and show an error in uiMessage
                                    capturedEthAddress = null // Example: clear if invalid
                                }
                            },
                            onScanQrClick = {
                                when (ContextCompat.checkSelfPermission(
                                    this, android.Manifest.permission.CAMERA
                                )) {
                                    android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                                        showQrScannerView = true
                                    }
                                    else -> {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            onDoneClick = {
                                // In this decoupled step, "Done" might just finish the activity
                                // or log the captured address.
                                if (capturedEthAddress != null) {
                                    Log.i(TAG, "Final captured Ethereum Address: $capturedEthAddress")
                                    // Proceed to next step or finish
                                } else {
                                    Log.w(TAG, "Done clicked, but no address was captured.")
                                }
                                finish()
                            }
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            Log.d(TAG, "Activity timeout timer started for ${ACTIVITY_TIMEOUT_MS / 1000} seconds.")
            delay(ACTIVITY_TIMEOUT_MS)
            Log.d(TAG, "Activity timeout reached. Finishing AddPassActivity.")
            if (!isFinishing && !isDestroyed) {
                finish()
            }
        }
    }

    private fun isValidEthAddress(address: String): Boolean {
        // Basic check, can be made more robust
        return address.startsWith("0x") && address.length == 42 && address.matches(Regex("0x[a-fA-F0-9]{40}"))
    }

    // onResume, onPause, onDestroy remain largely the same for cameraExecutor
    // and basic activity lifecycle logging.

    override fun onResume() {
        super.onResume()
        // No NFC reader mode logic needed specifically for address capture
        Log.d(TAG, "AddPassActivity resumed.")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "AddPassActivity paused.")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        Log.d(TAG, "Camera executor shutdown.")
    }
    // Removed saveUriForHceService and getGooglePaySaveUri as they are not used in this step
}

// Renamed Composable for clarity in this decoupled step
@Composable
fun CaptureAddressScreenContent(
    modifier: Modifier = Modifier,
    uiMessage: String,
    currentAddress: String,
    onAddressChanged: (String) -> Unit,
    onScanQrClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ethereum Address Input",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = currentAddress,
            onValueChange = onAddressChanged,
            label = { Text("Ethereum Address") },
            placeholder = { Text("0x...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = currentAddress.isNotEmpty() && !isValidEthAddress(currentAddress) // Basic visual feedback
        )
        if (currentAddress.isNotEmpty() && !isValidEthAddress(currentAddress)) {
            Text(
                "Invalid Ethereum address format.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onScanQrClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan QR Code", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Scan Address from QR")
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = uiMessage,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiMessage.contains("permission") || uiMessage.contains("Error")) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface // Default/info color
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onDoneClick, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Done")
        }
    }
}

// Add this helper function within the file or accessible scope if not already present
// This is duplicated from the previous isValidEthAddress for the Composable's direct use
@Composable
private fun isValidEthAddress(address: String): Boolean {
    return address.startsWith("0x") && address.length == 42 && address.matches(Regex("0x[a-fA-F0-9]{40}"))
}

