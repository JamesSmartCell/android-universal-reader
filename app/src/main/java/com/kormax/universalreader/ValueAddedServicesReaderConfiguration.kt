package com.kormax.universalreader

import android.nfc.tech.IsoDep
import android.util.Log
import com.kormax.universalreader.apple.vas.VasReaderConfiguration
import com.kormax.universalreader.google.smarttap.SmartTapReaderConfiguration
import com.kormax.universalreader.iso7816.Iso7816Aid
import com.kormax.universalreader.iso7816.Iso7816Command
import com.kormax.universalreader.tlv.ber.BerTlvMessage

class ValueAddedServicesReaderConfiguration(
    val vas: VasReaderConfiguration?,
    val smartTap: SmartTapReaderConfiguration?
) {
    suspend fun read(
        isoDep: IsoDep,
        hook: (String, Any) -> Unit = { _, _ -> }
    ): ValueAddedServicesResult {
        val selectOseCommand = Iso7816Command.selectAid(Iso7816Aid.VAS)
        hook("command", selectOseCommand) // Make sure selectOseCommand's bytes are logged
        Log.d("YOLESS VASReader", "SELECT OSE.VAS APDU: ${selectOseCommand.toByteArray().toHexString()}") // Assuming Iso7816Command has a 'bytes' property

        val selectOseResponse = isoDep.transceive(selectOseCommand.toByteArray()) // Assuming .bytes gives the ByteArray
        hook("response", selectOseResponse) // Make sure the full byte array of selectOseResponse is logged
        Log.d("YOLESS VASReader", "SELECT OSE.VAS Full Response: ${selectOseResponse.toHexString()}")

        val selectOseResponse2 = isoDep.transceive(selectOseCommand)

        // Assuming selectOseResponse is a ByteArray or has a property 'sw' that is a ByteArray of 2 bytes
        // Or if 'sw' is an object that has a toHexString() method as per your original code:
        val statusWordHex = selectOseResponse2.sw.toHexString() // Get the status word hex string
        Log.d("YOLESS VASReader", "SELECT OSE.VAS Status Word: $statusWordHex")


        if (statusWordHex != "9000") {
            // Include the actual failing status word in the exception message
            throw Exception("Could not select OSE.VAS applet. Status Word: $statusWordHex")
        }

        val walletType =
            String(
                BerTlvMessage(selectOseResponse2.data)
                    .findByTagElseThrow("6f")
                    .findByTagElseThrow("50")
                    .value
                    .toByteArray(),
                Charsets.UTF_8
            )

        hook("log", "Wallet type ${walletType}")

        return when (walletType) {
            "ApplePay" -> vas?.read(isoDep, selectOseResponse2, hook) ?: ValueAddedServicesResult()
            "AndroidPay" ->
                smartTap?.read(isoDep, selectOseResponse2, hook) ?: ValueAddedServicesResult()
            else -> ValueAddedServicesResult()
        }
    }
}
