package com.kormax.universalreader


import QrCodeScannerView
import RetrofitClient
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kormax.universalreader.Constants.Companion.NFC_LOG_MESSAGE
import com.kormax.universalreader.Constants.Companion.VAS_READ_MESSAGE
import com.kormax.universalreader.apple.vas.VasResult
import com.kormax.universalreader.google.smarttap.SmartTapObjectCustomer
import com.kormax.universalreader.google.smarttap.SmartTapObjectPass
import com.kormax.universalreader.google.smarttap.SmartTapResult
import com.kormax.universalreader.jwt.PK
import com.kormax.universalreader.jwt.PK.Companion.buildConfigJSON
import com.kormax.universalreader.model.ReaderConfigurationModel
import com.kormax.universalreader.ui.theme.UniversalReaderTheme
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.utils.Numeric
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.ExecutorService
import kotlin.text.split

// For the request body
data class LoginRequest(
    val username: String,
    val password: String
)

// For the JSON response
data class LoginResponse(
    val success: Boolean,
    val cardName: String?,
    val cardIcon: String?,
    val cardColor: String?,
    val collectorId: String?,
    val privateKey: String?, // Be VERY careful with handling and storing private keys
    val passTypeIdentifier: String?,
    val keyPem: String?,
    val commandToken: String
    // Add any other fields you might get
)

data class UserDetails(
    val cardName: String?,
    val cardIcon: String?,
    val cardColor: String?,
    val collectorId: String?,
    val passTypeIdentifier: String?,
    val userPrivateKeyInsecure: String?,
    val userPrivatePEM: String?,
    val securityToken: String?
)

data class BumpTokensRequest(
    val userPayload: String,
    val commandToken: String,
    val amount: Int,
    val platform: String
)

data class TapSetupRequest(
    val tapId: String
)

data class BumpTokensResponse(
    val success: Boolean,
    val message: String?
    // Add any other fields you expect in the response
)

interface ApiService {
    @POST("merchant-logins/get-secret-key") // The path of your endpoint
    suspend fun login(
        @Header("Authorization") authorizationHeader: String, // Or however you provide this token
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse> // Use Response<> to get headers and status code

    // New function for bumping tokens
    @POST("merchant-logins/bump-tokens")
    suspend fun bumpTokens(
        @Header("Authorization") authorizationHeader: String,
        @Body bumpTokensRequest: BumpTokensRequest
    ): Response<BumpTokensResponse>

    // Login through tap
    @POST("merchant-logins/tap-setup")
    suspend fun tapSetup(
        @Header("Authorization") authorizationHeader: String,
        @Body tapSetupRequest: TapSetupRequest
    ): Response<LoginResponse>
}

val bearerToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiZHVtbXkiLCJpYXQiOjE3NTAzMTU1MjN9.iSo3ZVZ41DdEq-rl0QviOCbQkvktuelKPi4pvnvsh6I"; // If your API requires a Bearer token in the header

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var vasDone = false
    private var hook = { _: String, _: Any -> }
    private var configuration = ValueAddedServicesReaderConfiguration(null, null)
    private var directory = "0x1Fc5a77C8c666274a98908FD32928f0DA9A4b8C3";
    private var capturedCardId by mutableStateOf<String?>(null)

    // Keep track of whether the QR scanner is supposed to be active
    private var isQrScannerActive by mutableStateOf(false)


    /*private fun enableNfcReader() {
        if (nfcAdapter != null && nfcAdapter!!.isEnabled) {
            val readerFlags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            nfcAdapter?.enableReaderMode(this, this, readerFlags, null)
            Log.d("NfcReader", "Reader Mode Enabled")
        } else {
            Log.w("NfcReader", "NFC Adapter not available or not enabled.")
            // Optionally prompt user to enable NFC
        }
    }*/
    private fun enableNfcReader() {
        nfcAdapter?.let {
            if (it.isEnabled) {
                val readerFlags = NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_NFC_BARCODE or // If you also read NFC barcodes
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
                it.enableReaderMode(this, this, readerFlags, null)
                Log.d("NfcReader", "Reader Mode Enabled onResume")
            } else {
                Log.w("NfcReader", "NFC Adapter not enabled.")
                // Optionally prompt user to enable NFC settings
                // Toast.makeText(this, "NFC is not enabled.", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Log.w("NfcReader", "NFC Adapter is null.")
            // Toast.makeText(this, "NFC not supported on this device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidEthAddress2(address: String): Boolean {
        return address.startsWith("0x") && address.length == 42 && address.matches(Regex("0x[a-fA-F0-9]{40}"))
    }

    private fun isValidCardId(cardTag: String): Boolean {
        if (cardTag.isEmpty()) {
            return false;
        } else if (cardTag.contains("-")) {
            val cardId = cardTag.split("-")[0]
            val ethAddress = cardTag.split("-")[1]
            if (cardId.isEmpty() || !isValidEthAddress2(ethAddress)) {
                return false;
            } else {
                return true;
            }
        }
        try {
            // decode hex to long int
            val decimalVal = BigInteger(cardTag, 16).toLong();
            if (decimalVal > 0) {
                return true;
            }
        } catch (e: Exception) {
            // not a pure hex value
        }

        return false;
    }

    fun performBumpTokens(
        userPayload: String,
        context: Context,
        amount: Int = 1, // Default amount to 1 as per your example
        platform: String = "google" // Default platform as per your example
    ) {

        //fetch commandToken from prefs
        val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val commandToken: String? = sharedPreferences.getString("securityToken", null);

        //check if this can be decrypted
        try {
            //attempt to decrypt
            val plainText = Aes256Decrypter.decrypt(userPayload);
            Log.w("YOLESS", "Decrypted: ${plainText}");
        } catch (e: Exception) {
            Log.e("YOLESS", "Error decrypting security token: ${e.message}")
        }

        if (commandToken.isNullOrEmpty()) {
            Log.e("BumpTokens", "No security token found in shared preferences.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch { // Use Dispatchers.IO for network calls
            val bumpRequest = BumpTokensRequest(
                userPayload = userPayload,
                commandToken = commandToken,
                amount = amount,
                platform = platform
            )

            try {
                val formattedAuthHeader = "Bearer $bearerToken"
                val response = RetrofitClient.instance.bumpTokens(
                    authorizationHeader = formattedAuthHeader,
                    bumpTokensRequest = bumpRequest,
                )

                withContext(Dispatchers.Main) { // Switch to Main thread for UI updates
                    if (response.isSuccessful) {
                        val bumpResponse = response.body()
                        if (bumpResponse != null) {
                            Log.i("BumpTokens", "Success: ${bumpResponse.success}, Message: ${bumpResponse.message}")
                            // TODO: Handle successful response (e.g., update UI, show Toast)
                            // showToast("Tokens bumped successfully: ${bumpResponse.message}")
                        } else {
                            Log.w("BumpTokens", "Response body is null but request was successful.")
                            // TODO: Handle successful response with empty body if that's expected
                            // showToast("Tokens bumped, but no specific message.")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("BumpTokens", "Error: ${response.code()} - ${response.message()}. Body: $errorBody")
                        // TODO: Handle API error (e.g., show error message to user)
                        // showToast("Error bumping tokens: ${response.code()} - ${errorBody ?: response.message()}")
                    }
                }
            } catch (e: Exception) {
                // Other unexpected errors (e.g., serialization issues if response structure mismatches)
                withContext(Dispatchers.Main) {
                    Log.e("BumpTokens", "Unexpected error: ${e.message}", e)
                    // TODO: Handle other errors
                    // showToast("An unexpected error occurred: ${e.message}")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        //clearUserDetailsFromPreferences(this);

        try {
            // ... (your configuration loading)
        } catch (e: Exception) {
            Log.e("MainActivity", "${e}")
        }

        // enableEdgeToEdge() // Called once at the top is fine

        val thisContext: Context = this;

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            // var uiMessage by remember { mutableStateOf("Scan QR") } // You can keep or remove if not directly used in the merged UI

            // Unified state to control visibility of the QR Scanner
            var showQrScannerState by remember { mutableStateOf(false) }

            // Update our activity-level tracker when the Composable state changes
            LaunchedEffect(showQrScannerState) {
                isQrScannerActive = showQrScannerState
                if (showQrScannerState) {
                    disableNfcReaderInternal() // Disable NFC when QR scanner becomes active
                } else {
                    // Only enable if activity is resumed and QR scanner is not active
                    if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                        enableNfcReaderInternal()
                    }
                }
            }

            // Effect to update UI message when capturedEthAddress changes (if uiMessage is still needed)
            LaunchedEffect(capturedCardId) {
                // ... (your uiMessage logic)
            }

            // --- Start of Configuration for the Menu Items (from the second Scaffold's logic) ---
            val configurationFileLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    try {
                        val jsonString = loadJsonFile(context = this, uri) ?: return@rememberLauncherForActivityResult
                        val model = jsonString.let { Json.decodeFromString<ReaderConfigurationModel>(it) }
                        configuration = model.load()
                        PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putString("configuration", jsonString)
                            .apply()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Configuration loaded for vas=${configuration.vas?.merchants?.size ?: 0} gst=${configuration.smartTap != null}",
                            )
                        }
                    } catch (e: Exception) {
                        scope.launch { snackbarHostState.showSnackbar("Could not load file due to $e") }
                    }
                }

            var modeMenuExpanded by remember { mutableStateOf(false) }
            var logsEnabled by remember { mutableStateOf(true) }
            var commandsEnabled by remember { mutableStateOf(true) }
            var responsesEnabled by remember { mutableStateOf(true) }

            // hook = { type, value -> ... } // Your hook definition should be outside setContent or passed if needed
            // For simplicity, I'll assume `sendMessage` is a method in your Activity
            // and `hook` is setup as a member variable as you have it.

            // --- End of Configuration for the Menu Items ---


            // SINGLE, UNIFIED UI BLOCK
            UniversalReaderTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        // This is the TopAppBar from your SECOND Scaffold, which includes the menu
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            title = { Text("Universal Reader") },
                            actions = {
                                Box {
                                    IconButton(onClick = { modeMenuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = "Configure display parameters"
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = modeMenuExpanded,
                                        onDismissRequest = { modeMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            { Text("Load configuration") },
                                            onClick = {
                                                modeMenuExpanded = false // Close menu after click
                                                configurationFileLauncher.launch("application/json")
                                            }
                                            // trailingIcon = { Icons.Outlined.Create } // Icon was causing type mismatch with Text lambda
                                        )
                                        Divider()
                                        DropdownMenuItem(
                                            { Text("Logs") },
                                            onClick = { logsEnabled = !logsEnabled; modeMenuExpanded = false },
                                            trailingIcon = { Checkbox(checked = logsEnabled, onCheckedChange = null) } // onCheckedChange null if only driven by item click
                                        )
                                        Divider()
                                        DropdownMenuItem(
                                            { Text("Commands") },
                                            onClick = { commandsEnabled = !commandsEnabled; modeMenuExpanded = false },
                                            trailingIcon = { Checkbox(checked = commandsEnabled, onCheckedChange = null) }
                                        )
                                        Divider()
                                        DropdownMenuItem(
                                            { Text("Responses") },
                                            onClick = { responsesEnabled = !responsesEnabled; modeMenuExpanded = false },
                                            trailingIcon = { Checkbox(checked = responsesEnabled, onCheckedChange = null) }
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            // sendMessage(null) // Assuming sendMessage is a method in your Activity
                                            snackbarHostState.showSnackbar("History cleared")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Localized description"
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        // THIS IS YOUR FAB from the FIRST Scaffold
                        if (!showQrScannerState) {
                            FloatingActionButton(onClick = {
                                showQrScannerState = true // This will trigger the LaunchedEffect
                            }) {
                                Icon(Icons.Filled.QrCodeScanner, "Scan QR Code")
                            }
                        }
                    }
                ) { innerPadding -> // This innerPadding comes from the single, unified Scaffold
                    Box(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()) {
                        if (showQrScannerState) {
                            QrCodeScannerView(
                                onQrCodeScanned = { scannedData ->
                                    showQrScannerState = false // This will trigger the LaunchedEffect
                                    capturedCardId = scannedData
                                    Log.i("MainActivity_QR", "Scanned Data: $scannedData")
                                    showToast("QR Scanned: $scannedData")
                                    if (isValidCardId(scannedData)) {
                                        // Use lifecycleScope if this lambda is conceptually tied
                                        // to the Activity's lifecycle for the operation.
                                        lifecycleScope.launch { // Activity's lifecycle scope
                                            try {
                                                performBumpTokens(scannedData, thisContext);
                                                //updateCardBalance(scannedData)
                                                // UI updates from here should ideally be on Dispatchers.Main
                                                // or via StateFlow/LiveData observed by Composables.
                                            } catch (e: Exception) {
                                                Log.e("MainActivity_QR", "Error updating card balance", e)
                                            }

                                            // now restart the app
                                            // Launch a coroutine to introduce a delay
                                            CoroutineScope(Dispatchers.Main).launch {
                                                delay(500) // Delay for 500 milliseconds (adjust as needed, but keep it short)
                                                Log.d("CredentialsAction", "Delay complete. Restarting application.")
                                                restartApplication(thisContext);
                                            }
                                        }
                                    }
                                },
                                onCloseScanner = {
                                    showQrScannerState = false
                                }
                            )
                        } else {
                            // Your main content (NFC reading UI, etc.)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Main() // Your existing Main composable
                                val userDetails: UserDetails? = loadUserDetailsFromPreferences(thisContext);

                                Log.w("YOLESS", "Has userDatiles: ${userDetails}")

                                var loginConfig: String;
                                if (userDetails != null) {
                                    loginConfig = buildConfigJSON(userDetails.collectorId!!, userDetails.userPrivatePEM!!, userDetails.passTypeIdentifier!!);
                                } else {
                                    loginConfig = loadDefaultConfig()
                                }

                                Log.i("YOLESS", "Login Config: ${loginConfig}")
                                val modelConfig = loginConfig.let {
                                    Json.decodeFromString<ReaderConfigurationModel>(it)
                                }
                                configuration = modelConfig.load()
                                Toast.makeText(this@MainActivity, "Loaded config from login", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isLoyaltyCard(passTypeIdentifier: String?, payload: String?): Boolean {
        // Apple VAS Detection Logic
        // Example: Check for known loyalty pass type identifiers
        if (passTypeIdentifier != null) {
            if (passTypeIdentifier.startsWith("pass.com.openpasskey.stampcard") || // Replace with actual known prefixes/identifiers
                passTypeIdentifier.contains("loyalty", ignoreCase = true)
            ) {
                return true
            }
        }
        // Example: Check payload content (if you parse it)
        // This is highly dependent on the payload structure
        if (payload != null) {
            if (payload.contains("\"loyaltyProgramName\"", ignoreCase = true) ||
                payload.contains("\"memberId\"", ignoreCase = true)
            ) {
                // This is a very basic string check.
                // For robust parsing, you'd typically deserialize the JSON payload
                // and check specific fields.
                return true
            }
        }
        return false
    }

    private fun isLoyaltyPass(passType: String?, objectId: String?): Boolean {
        // Google Smart Tap Detection Logic
        // Example: Check for known loyalty pass types
        if (passType != null) {
            if (passType.equals("LOYALTY_CARD", ignoreCase = true) ||
                passType.equals("LOYALTY", ignoreCase = true) ||
                // Add other known loyalty types from Google's documentation or issuer specs
                passType.contains("loyalty", ignoreCase = true)
            ) {
                return true
            }
        }
        // Example: Check objectId patterns (less common for type, more for issuer)
        if (objectId != null) {
            // if (objectId.startsWith("loyalty:")) { return true }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        // Only enable NFC if the QR scanner is NOT active and NFC adapter is available and enabled
        if (!isQrScannerActive) {
            enableNfcReaderInternal()
        }
    }

    override fun onPause() {
        super.onPause()
        disableNfcReaderInternal() // Always disable when activity is paused
    }

    // Renamed to avoid confusion with any potential Composable function
    private fun enableNfcReaderInternal() {
        nfcAdapter?.let {
            if (it.isEnabled && !isQrScannerActive) { // Double check isQrScannerActive
                val readerFlags = NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_NFC_BARCODE or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
                try {
                    it.enableReaderMode(this, this, readerFlags, null)
                    Log.d("NfcReader", "Reader Mode Enabled (onResume or QR close)")
                } catch (e: Exception) {
                    Log.e("NfcReader", "Error enabling reader mode: ${e.message}", e)
                }
            } else if (!it.isEnabled) {
                Log.w("NfcReader", "NFC Adapter not enabled.")
                // Toast.makeText(this, "NFC is not enabled.", Toast.LENGTH_LONG).show()
            } else {
                Log.d("NfcReader", "QR Scanner is active, not enabling NFC reader mode now.")
            }
        } ?: run {
            Log.w("NfcReader", "NFC Adapter is null.")
            // Toast.makeText(this, "NFC not supported on this device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun disableNfcReaderInternal() {
        nfcAdapter?.let {
            if (it.isEnabled) { // Check if adapter is enabled before trying to disable reader mode
                try {
                    it.disableReaderMode(this)
                    Log.d("NfcReader", "Reader Mode Disabled (onPause or QR open)")
                } catch (e: Exception) {
                    Log.e("NfcReader", "Error disabling reader mode: ${e.message}", e)
                }
            } else {
                Log.w("NfcReader", "NFC Adapter not enabled, can't disable reader mode.")
            }

        } ?: run {
            Log.w("NfcReader", "NFC Adapter is null, can't disable reader mode.")
        }
    }

    private fun disableNfcReader() {
        nfcAdapter?.disableReaderMode(this)
        Log.d("NfcReader", "Reader Mode Disabled onPause")
    }

    override fun onTagDiscovered(tag: Tag) {
        sendMessage(null)

        CoroutineScope(Dispatchers.IO).launch { // Already on a background thread
            try {
                hook("log", "Got tag: ${tag.id.toHexString()} ${tag.techList.contentToString()}")

                val isoDep = IsoDep.get(tag) ?: return@launch
                isoDep.connect()
                Log.i("YOLESS", "IsoDep connected.")

                // --- THIS IS THE CRITICAL AREA FOR THE DELAY ---
                try {
                    // Now attempt the operation that includes applet selection
                    val result = configuration.read(isoDep, hook)
                    vasDone = true // Assuming this means success
                    val readResult: Pair<String, String> = sendReadData(result)
                    val passPayload = readResult.first
                    val passType = readResult.second

                    Log.w("YOLESS", "Pass Payload: ${passPayload} ${passType} ${result}")
                    Log.w("YOLESS", "Pass Type: ${passType}")
                    Log.w("YOLESS", "Pass Result: ${result}")

                    // where is pass payload??


                    if (passPayload.isEmpty()) {
                        //handle case where user doesn't have a pass
                        //Open a new Activity which creates the pass
                        Log.i("YOLESS", "No relevant pass found. Launching AddPassActivity.")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "No pass found, please claim card first", Toast.LENGTH_SHORT).show()
                        }

                        /*withContext(Dispatchers.Main) {
                            val intent = Intent(this@MainActivity, AddPassActivity::class.java)
                            startActivity(intent)
                        }*/
                    } else {
                        // we got a payload. Format should be:
                        // LOYALTYCARDID_STRING-ETHEREUMADDRESS_STRING
                        //attempt to decrypt:
                        try {
                            //attempt to decrypt
                            val plainText = Aes256Decrypter.decrypt(passPayload);
                            Log.w("YOLESS", "Decrypted: ${plainText}");
                            //split and get the client id
                            val stringList: List<String> = plainText!!.split('|');
                            // pull out the command token
                            if (stringList.size == 3)
                            {
                                val commandToken = stringList[2];
                                // now invoke API
                                performTapSetupAndSaveDetails(this@MainActivity, commandToken);

                                //now restart app
                                // Launch a coroutine to introduce a delay
                                /*CoroutineScope(Dispatchers.Main).launch {
                                    delay(500) // Delay for 500 milliseconds (adjust as needed, but keep it short)
                                    Log.d("CredentialsAction", "Delay complete. Restarting application.")
                                    restartApplication(this@MainActivity)
                                }*/
                            }
                            return@launch
                        } catch (e: Exception) {
                            Log.e("YOLESS", "Error decrypting security token: ${e.message}")
                        }

                        if (!isValidCardId(passPayload)) {
                            Log.w("YOLESS", "Pass payload format incorrect: $passPayload")
                            return@launch
                        }

                        //

                        performBumpTokens(passPayload, this@MainActivity);
                        //updateCardBalance(passPayload);
                    }

                } catch (selectionException: Exception) {
                    // Specific logging if applet selection is the point of failure
                    Log.e("YOLESS", "Exception during/after initial delay, possibly applet selection: ${selectionException.message}", selectionException)
                    sendMessage("Error selecting applet: ${selectionException.message}")
                    // Optionally, you could try a retry mechanism here with a longer delay
                } finally {
                    if (isoDep.isConnected) {
                        isoDep.close()
                        Log.i("YOLESS", "IsoDep closed.")
                    }
                }

                // ... rest of your existing logic (handling passPayload, etc.) ...

            } catch (e: Exception) {
                sendMessage("Got an exception in onTagDiscovered: ${e.message}")
                Log.e("YOLESS", "General exception in onTagDiscovered: ${e.stackTraceToString()}")
                // Ensure IsoDep is closed in case of broader exceptions too
                IsoDep.get(tag)?.takeIf { it.isConnected }?.close()
            }
        }
    }

    fun sendMessage(message: String?) {
        val intent = Intent(NFC_LOG_MESSAGE)
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }

    fun sendReadData(read: ValueAddedServicesResult): Pair<String, String> {
        Log.i("MainActivity", "sendReadData=${read}")
        var payload: String = "";
        var detectedCardType: String = "UNKNOWN" // Default
        var thisDetectedCardType: String = "";
        try {
            when (read) {
                is VasResult -> {
                    for (t in read.read) {
                        val passPayloadString = t.payload?.value.toString()
                        Log.i("YOLESS", "Pass Payload: ${passPayloadString} ${t.passTypeIdentifier}")

                        //detect if pass payload is a hex string. Attempt to convert to long
                        if (isValidCardId(passPayloadString)) {
                            detectedCardType = "LOYALTY (Apple VAS)"
                            thisDetectedCardType = detectedCardType;
                            Log.i(
                                "YOLESS",
                                "Detected Loyalty Card (Apple VAS): ID=${t.passTypeIdentifier}"
                            )
                            payload = passPayloadString;
                            Log.i("YOLESS", "Payload: ${payload}")
                        }

                        val intent = Intent(VAS_READ_MESSAGE)
                        intent.putExtra(
                            "read",
                            arrayOf(
                                t.passTypeIdentifier ?: "N/A",
                                t.status.toString(),
                                passPayloadString,
                                detectedCardType // Add the detected type to the broadcast
                            )
                        )
                        sendBroadcast(intent)
                        detectedCardType = "UNKNOWN" // Reset for next pass if multiple
                    }
                }

                is SmartTapResult -> {
                    for (o in read.objects) {
                        val intent = Intent(VAS_READ_MESSAGE)
                        var passTypeForBroadcast: String? = null
                        var idForBroadcast: String? = null
                        var dataForBroadcast: String? = null

                        when (o) {
                            is SmartTapObjectPass -> {
                                idForBroadcast = o.objectId.toHexString()
                                passTypeForBroadcast = o.type
                                dataForBroadcast = o.message
                                if (isLoyaltyPass(o.type, o.objectId.toHexString())) {
                                    detectedCardType = "LOYALTY (Google SmartTap)"
                                    thisDetectedCardType = detectedCardType;
                                    Log.i(
                                        "YOLESS",
                                        "Detected Loyalty Card (Google SmartTap): ID=${o.objectId.toHexString()}, Type=${o.type} Payload=${o.message}"
                                    )
                                    payload = o.message;
                                }
                            }

                            is SmartTapObjectCustomer -> {
                                idForBroadcast = o.customerId.toHexString()
                                passTypeForBroadcast = "CUSTOMER"
                                dataForBroadcast = o.language
                                // Customers are not typically loyalty cards themselves, but could be linked
                            }
                        }

                        intent.putExtra(
                            "read",
                            arrayOf(
                                idForBroadcast ?: "N/A",
                                passTypeForBroadcast ?: "N/A",
                                dataForBroadcast ?: "N/A",
                                detectedCardType // Add the detected type to the broadcast
                            )
                        )
                        sendBroadcast(intent)
                        detectedCardType = "UNKNOWN" // Reset for next object
                    }
                }

                else -> {
                    Log.w(
                        "MainActivity",
                        "Unknown ValueAddedServicesResult type: ${read::class.java.simpleName}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in sendReadData: ${e.stackTraceToString()}")
        }

        //need to return payload and detectedCardType
        return Pair(payload, thisDetectedCardType);
    }
}

@Composable
fun MessageBroadcastReceiver(systemAction: String, onSuccess: (Intent?) -> Unit) {
    val context = LocalContext.current
    val currentOnSystemEvent by rememberUpdatedState(onSuccess)

    DisposableEffect(context) {
        val intentFilter = IntentFilter(systemAction)
        val broadcast =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    currentOnSystemEvent(intent)
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcast, intentFilter, Context.RECEIVER_EXPORTED)
            Log.i("DisposableEffect", "registered receiver")
        } else {
            context.registerReceiver(broadcast, intentFilter)
        }

        onDispose { context.unregisterReceiver(broadcast) }
    }
}

fun convertNewlinesToLiteralEscapes(pemKeyWithActualNewlines: String): String {
    // Replace different types of newlines with the literal characters '\' and 'n'
    return pemKeyWithActualNewlines
        .replace("\r\n", "\\n") // Windows-style newline first
        .replace("\n", "\\n")   // Unix-style newline
        .replace("\r", "\\n")   // Older Mac-style newline (less common now)
}

// Function to perform the login and save to SharedPreferences
fun performTapSetupAndSaveDetails(
    context: Context,
    thisCommandToken: String
) {
    // It's good practice to run network operations off the main thread.
    // If this is called from a Composable, use rememberCoroutineScope().launch
    // If called from a ViewModel, use viewModelScope.launch
    // For a simple function, you might pass a CoroutineScope or use GlobalScope (less recommended for UI related ops)

    CoroutineScope(Dispatchers.IO).launch { // Use an appropriate scope
        try {
            Log.d("YOLESS", "Attempting Tap setup request ${thisCommandToken}")

            val setupRequest = TapSetupRequest(tapId = thisCommandToken);

            // Make the API call

            val formattedAuthHeader = "Bearer $bearerToken"
            val response = RetrofitClient.instance.tapSetup(
                authorizationHeader = formattedAuthHeader,
                tapSetupRequest = setupRequest,
            )

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!! // Not null due to the check
                Log.d("YOLESS", "Login successful: ${loginResponse.success}")

                var success: Boolean = false;

                if (loginResponse.success) {
                    // Save to SharedPreferences
                    val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    success = with(sharedPreferences.edit()) {
                        putString("cardName", loginResponse.cardName)
                        putString("cardIcon", loginResponse.cardIcon)
                        putString("cardColor", loginResponse.cardColor)
                        putString("collectorId", loginResponse.collectorId)
                        putString("passTypeIdentifier", loginResponse.passTypeIdentifier)
                        // !!! SECURITY WARNING: Storing raw private keys in SharedPreferences is highly insecure !!!
                        // Consider using Android Keystore for sensitive data like private keys.
                        // For a demo, this might be acceptable, but NEVER do this in a production app.
                        loginResponse.privateKey?.let {
                            putString("userPrivateKey_INSECURE", it)
                            Log.w("LoginAPI_Security", "Warning: Private key stored insecurely in SharedPreferences for demo purposes.")
                        }
                        val escapedPem = loginResponse.keyPem?.trimIndent();
                        val pemKeyWithLiteralEscapes = convertNewlinesToLiteralEscapes(escapedPem!!)
                        Log.w("YOLESS", "PEM: ${pemKeyWithLiteralEscapes}");
                        putString("pemPrivateKey_INSECURE", pemKeyWithLiteralEscapes)
                        putString("securityToken", loginResponse.commandToken);
                        commit() // Use commit() for synchronous save
                    }

                    val reportStr = if (success) "Successful" else "Failed";

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Login ${reportStr}!", Toast.LENGTH_LONG).show()
                        Log.i("LoginAPI", "User details saved to SharedPreferences.")
                    }

                    if (success) {
                        // now trigger an application reload
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500) // Delay for 500 milliseconds (adjust as needed, but keep it short)
                            Log.d("CredentialsAction", "Delay complete. Restarting application.")
                            restartApplication(context.applicationContext)
                        }
                    }
                } else {
                    // API reported success: false (e.g., wrong credentials)
                    val errorMessage = "Login failed: ${loginResponse.cardName ?: "Invalid credentials"}" // Example error message
                    Log.w("LoginAPI", errorMessage)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // API call was not successful (e.g., 401, 404, 500)
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorCode = response.code()
                Log.e("LoginAPI", "Login error - Code: $errorCode, Body: $errorBody")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Login Error: $errorCode", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            // General exceptions (e.g., no network, parsing errors if GsonConverter fails, etc.)
            Log.e("LoginAPI", "Exception during login: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// Function to perform the login and save to SharedPreferences
fun performLoginAndSaveDetails(
    context: Context,
    usernameInput: String,
    passwordInput: String
) {
    // It's good practice to run network operations off the main thread.
    // If this is called from a Composable, use rememberCoroutineScope().launch
    // If called from a ViewModel, use viewModelScope.launch
    // For a simple function, you might pass a CoroutineScope or use GlobalScope (less recommended for UI related ops)

    CoroutineScope(Dispatchers.IO).launch { // Use an appropriate scope
        try {
            Log.d("LoginAPI", "Attempting login for user: $usernameInput")

            val loginRequest = LoginRequest(username = usernameInput, password = passwordInput)

            // Make the API call

            val formattedAuthHeader = "Bearer $bearerToken"
            val response = RetrofitClient.instance.login(
                authorizationHeader = formattedAuthHeader,
                loginRequest = loginRequest,
            )

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!! // Not null due to the check
                Log.d("LoginAPI", "Login successful: ${loginResponse.success}")

                var success: Boolean = false;

                if (loginResponse.success) {
                    // Save to SharedPreferences
                    val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    success = with(sharedPreferences.edit()) {
                        putString("cardName", loginResponse.cardName)
                        putString("cardIcon", loginResponse.cardIcon)
                        putString("cardColor", loginResponse.cardColor)
                        putString("collectorId", loginResponse.collectorId)
                        putString("passTypeIdentifier", loginResponse.passTypeIdentifier)
                        // !!! SECURITY WARNING: Storing raw private keys in SharedPreferences is highly insecure !!!
                        // Consider using Android Keystore for sensitive data like private keys.
                        // For a demo, this might be acceptable, but NEVER do this in a production app.
                        loginResponse.privateKey?.let {
                            putString("userPrivateKey_INSECURE", it)
                            Log.w("LoginAPI_Security", "Warning: Private key stored insecurely in SharedPreferences for demo purposes.")
                        }
                        val escapedPem = loginResponse.keyPem?.trimIndent();
                        val pemKeyWithLiteralEscapes = convertNewlinesToLiteralEscapes(escapedPem!!)
                        Log.w("YOLESS", "PEM: ${pemKeyWithLiteralEscapes}");
                        putString("pemPrivateKey_INSECURE", pemKeyWithLiteralEscapes)
                        putString("securityToken", loginResponse.commandToken);
                        commit() // Use commit() for synchronous save
                    }

                    val reportStr = if (success) "Successful" else "Failed";

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Login ${reportStr}!", Toast.LENGTH_LONG).show()
                        Log.i("LoginAPI", "User details saved to SharedPreferences.")
                    }

                    if (success) {
                        // now trigger an application reload
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500) // Delay for 500 milliseconds (adjust as needed, but keep it short)
                            Log.d("CredentialsAction", "Delay complete. Restarting application.")
                            restartApplication(context.applicationContext)
                        }
                    }
                } else {
                    // API reported success: false (e.g., wrong credentials)
                    val errorMessage = "Login failed: ${loginResponse.cardName ?: "Invalid credentials"}" // Example error message
                    Log.w("LoginAPI", errorMessage)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // API call was not successful (e.g., 401, 404, 500)
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorCode = response.code()
                Log.e("LoginAPI", "Login error - Code: $errorCode, Body: $errorBody")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Login Error: $errorCode", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            // General exceptions (e.g., no network, parsing errors if GsonConverter fails, etc.)
            Log.e("LoginAPI", "Exception during login: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// Camera related
private lateinit var cameraExecutor: ExecutorService
private var showQrScannerView by mutableStateOf(false)

// Function to load details from SharedPreferences
// You can call this, for example, when your app starts or when a particular screen is loaded.
fun loadUserDetailsFromPreferences(context: Context): UserDetails? {
    val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    // Retrieve the values.
    // The second parameter to getString (and other get methods) is the default value
    // to return if the key is not found.
    val cardName: String? = sharedPreferences.getString("cardName", null)
    val cardIcon: String? = sharedPreferences.getString("cardIcon", null)
    val cardColor: String? = sharedPreferences.getString("cardColor", null)
    val collectorId: String? = sharedPreferences.getString("collectorId", null)
    val passTypeIdentifier: String? = sharedPreferences.getString("passTypeIdentifier", null)
    val userPrivateKeyInsecure: String? = sharedPreferences.getString("userPrivateKey_INSECURE", null)
    val userPEMPrivateKeyInsecure: String? = sharedPreferences.getString("pemPrivateKey_INSECURE", null)
    val securityToken: String? = sharedPreferences.getString("securityToken", null);

    // Now you can use these variables. For example, log them or update your UI state.
    if (!cardName.isNullOrEmpty()) {
        Log.d("PrefsLoad", "Loaded Card Name: $cardName")
        Log.d("PrefsLoad", "Loaded Card Icon: $cardIcon")
        Log.d("PrefsLoad", "Loaded Card Color: $cardColor")
        Log.d("PrefsLoad", "Loaded Collector ID: $collectorId")
        Log.d("PrefsLoad", "Loaded Pass Type Identifier: $passTypeIdentifier")
        if (userPrivateKeyInsecure != null) {
            Log.w(
                "PrefsLoad_Security",
                "Loaded Insecure Private Key (FOR DEMO ONLY): $userPrivateKeyInsecure"
            )
        } else {
            Log.d("PrefsLoad", "No private key found in preferences.")
        }
        Log.d("YOLESS", "SECURITY TOKEN: ${securityToken}")

        return UserDetails(
            cardName = cardName,
            cardIcon = cardIcon,
            cardColor = cardColor,
            collectorId = collectorId,
            passTypeIdentifier = passTypeIdentifier,
            userPrivateKeyInsecure = userPrivateKeyInsecure,
            userPrivatePEM = userPEMPrivateKeyInsecure,
            securityToken = securityToken
        )
    } else {
        return null;
    }

    // Example of assigning to variables if this function were part of a class or Composable's state
    // (Assuming these variables are declared elsewhere, e.g., as mutableStateOf for Compose)
    //
    // this.viewModelCardName = cardName
    // this.uiState.update { it.copy(cardIconUrl = cardIcon) }
    // ... and so on
}

fun parseRgbColor(rgbString: String?): Color {
    if (rgbString == null) return Color.Transparent // Default or error color

    // Regex to extract R, G, B values from "rgb(r,g,b)"
    val regex = """rgb\((\d{1,3}),\s*(\d{1,3}),\s*(\d{1,3})\)""".toRegex()
    val matchResult = regex.find(rgbString)

    return if (matchResult != null) {
        try {
            val (r, g, b) = matchResult.destructured
            Color(red = r.toInt(), green = g.toInt(), blue = b.toInt())
        } catch (e: NumberFormatException) {
            // Log error or handle parsing failure
            println("Error parsing RGB string: $rgbString - ${e.message}")
            Color.Transparent // Fallback color on parsing error
        }
    } else {
        println("RGB string does not match expected format: $rgbString")
        Color.Transparent // Fallback if format doesn't match
    }
}

fun clearUserDetailsFromPreferences(context: Context) {
    val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    val success = with(sharedPreferences.edit()) {
        remove("cardName")
        remove("cardIcon")
        remove("cardColor")
        remove("collectorId")
        remove("passTypeIdentifier")
        remove("userPrivateKey_INSECURE") // Also clear the insecurely stored private key
        commit() // Use commit() for synchronous save
    }
    if (success) {
        Log.d("PrefsClear", "User details synchronously cleared from SharedPreferences.")
    } else {
        Log.e("PrefsClear", "Failed to clear SharedPreferences synchronously.")
    }
}

fun restartApplication(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    if (intent != null) {
        val componentName = intent.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)

        // Ensure the current process is killed to complete the restart
        Runtime.getRuntime().exit(0) // More forceful exit
        // OR
        // Process.killProcess(Process.myPid())
        // exitProcess(0) // Can also be used
    } else {
        // Fallback or error handling if launch intent can't be found
        Log.e("AppRestart", "Could not find launch intent to restart app.")
        // You might want to just finish the activity here as a less ideal fallback
        // if (context is Activity) {
        //     context.finishAffinity()
        // }
        // Process.killProcess(Process.myPid())
        // exitProcess(0)
    }
}

fun loadDefaultConfig(): String  {
    return buildConfigJSON("42183724", "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIDpWHubE6AgVwmPyf2Yj4JrIKMUaHNwmh9o29fMVYzhcoAoGCCqGSM49\nAwEHoUQDQgAEyo3NNxhy902KTrjt2tSnX9vGSRA2No0QlDsPiLT33f9M3wzzuef4\nKiZ2PTzAotbRQrM0n/f/jWAPaOpf6467dA==\n-----END EC PRIVATE KEY-----", "pass.com.openpasskey.stamppass");
}

@Composable
fun Main() {
    var messages: List<Triple<Int, String, Array<String>>> by remember { mutableStateOf(listOf()) }
    val clipboardManager = LocalClipboardManager.current // Get ClipboardManager
    val context = LocalContext.current // For showing Toast (optional)
    val focusManager = LocalFocusManager.current

    // State for the Ethereum address TextField
    var ethereumAddress by remember {
        mutableStateOf(PreferencesManager.getEthereumAddress(context))
    }


    // State for new Username and Password fields
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    MessageBroadcastReceiver(NFC_LOG_MESSAGE) { intent ->
        val message = intent?.getStringExtra("message")
        if (message != null) {
            messages =
                (messages.toMutableList() + Triple(messages.size, "message", arrayOf(message)))
                    .toList()
        } else {
            messages = emptyList()
        }
    }

    MessageBroadcastReceiver(VAS_READ_MESSAGE) { intent ->
        val read = intent?.getStringArrayExtra("read") ?: return@MessageBroadcastReceiver
        Log.i("MessageBroadcastReceiver", "read=${read.contentToString()}")
        messages = (messages.toMutableList() + Triple(messages.size, "read", read)).toList()
    }

    // Column to hold the TextField and then the LazyColumn
    Column(modifier = Modifier.fillMaxSize()) {
        /*OutlinedTextField(
            value = ethereumAddress,
            onValueChange = { newValue ->
                // Basic validation for Ethereum address (alphanumeric, starts with 0x, length)
                // You might want more robust validation
                if (newValue.all { it.isLetterOrDigit() || it == 'x' } && newValue.length <= 42) {
                    ethereumAddress = newValue
                }
            },
            label = { Text("Ethereum Address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        // Save when TextField loses focus
                        PreferencesManager.saveEthereumAddress(context, ethereumAddress)
                        Log.d("Main", "Saved Ethereum Address: $ethereumAddress")
                        // Optionally, you can show a Snackbar or Toast here
                    }
                },
            singleLine = true
        )*/

        //pull stored values in from SharedPreferences
        val userDetails: UserDetails? = loadUserDetailsFromPreferences(context);

        // --- NEW: Username TextField ---
        if (userDetails == null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )

            // --- NEW: Password TextField ---
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image =
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )

            // --- NEW: Simple Login Button ---
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // TODO: Implement actual login logic here if needed for the demo
                    // For now, just show a Toast indicating a login attempt.
                    Log.d("Main", "Login attempt with Username: $username, Password: $password")
                    Toast.makeText(context, "Login attempt: User '$username'", Toast.LENGTH_LONG)
                        .show()

                    // login to API and pull JSON package
                    if (username.isNotBlank() && password.isNotBlank()) {
                        performLoginAndSaveDetails(context, username, password)
                    } else {
                        Toast.makeText(
                            context,
                            "Username and password cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // If you want to simulate hiding them after "login":
                    // You would need a state like `var isLoggedIn by remember { mutableStateOf(false) }`
                    // and then set `isLoggedIn = true` here.
                    // Then wrap the username, password, and login button in `if (!isLoggedIn) { ... }`
                    // For now, they will remain visible.
                    focusManager.clearFocus() // Clear focus to hide keyboard
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Login / Submit") // Changed button text slightly
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            val backgroundColor = parseRgbColor(userDetails.cardColor)

            // ... (your existing else block)
            Log.w("YOLESS", "CARDNAME: ${userDetails.cardName}") // Still need to handle cardName being null
            Column(
                modifier = Modifier.fillMaxWidth(), // Parent takes full width
                horizontalAlignment = Alignment.CenterHorizontally // Center children horizontally
            ) {
                // ... other composables if any ...

                Text(
                    text = userDetails.cardName ?: "No Name",
                    color = Color.White, // Or your dynamic text color
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center, // Not strictly needed if text is single line and background is snug
                    modifier = Modifier
                        // .fillMaxWidth() // << REMOVE this if you want snug background
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(12.dp) // << Specify shape directly in background
                        )
                        .padding(vertical = 32.dp, horizontal = 48.dp) // Padding defines the size of the colored block around the text
                )

                // ... other composables if any ...
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    clearUserDetailsFromPreferences(context);
                    Toast.makeText(context, "Credentials Cleared", Toast.LENGTH_SHORT)
                        .show()

                    focusManager.clearFocus();

                    //userDetails = null

                    username = ""
                    password = ""

                    // Launch a coroutine to introduce a delay
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(500) // Delay for 500 milliseconds (adjust as needed, but keep it short)
                        Log.d("CredentialsAction", "Delay complete. Restarting application.")
                        restartApplication(context.applicationContext)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Change Credentials") // Changed button text slightly
            }
        }

        // Spacer before your LazyColumn for messages
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            userScrollEnabled = true
        ) {
            items(messages.size, { index -> messages.get(index).first }) { result ->
                val messageTriple = messages.get(result) // Renamed for clarity
                when (messageTriple.second) {
                    "read" -> {
                        val textToCopy =
                            messageTriple.third.joinToString(separator = "\n") // Combine all parts for "read"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { // Make the whole "read" item clickable
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    // Optional: Show a Toast
                                    // Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = messageTriple.third[0],
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                                Text(
                                    text = messageTriple.third[1],
                                )
                            }

                            Divider(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = messageTriple.third[2],
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }

                    else -> { // For "message" type and any other
                        val textToCopy = messageTriple.third[0]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable { // Make the row clickable
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    // Optional: Show a Toast
                                    // Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = textToCopy, modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}


// QrCodeScannerComposable remains the same as provided previously.
// Make sure it's included in your file or accessible.
@OptIn(ExperimentalGetImage::class) @Composable
fun QrCodeScannerComposable(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
        remember { ProcessCameraProvider.getInstance(localContext) }
    var hasScanned by remember { mutableStateOf(false) }

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
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                            if (hasScanned) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty() && !hasScanned) {
                                            barcodes.firstOrNull()?.rawValue?.let { scannedValue ->
                                                hasScanned = true
                                                onQrCodeScanned(scannedValue)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("QrScanner", "Barcode scanning failed", e)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("QrScanner", "Use case binding failed", exc)
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Text("Cancel Scan")
        }
    }
}