package com.kormax.universalreader.jwt

import android.content.Context
import android.util.Log
import com.kormax.universalreader.UserDetails
import org.bouncycastle.asn1.esf.CommitmentTypeIdentifier
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader

class PK {
    // For demo only!
    companion object {

        fun buildConfigJSON(collectorId: String, userPrivatePEM: String, passTypeIdentifier: String): String {
            return "{\n" +
                    "    \"active\": [\"applevas\", \"googlesmarttap-google\"],\n" +
                    "    \"protocols\": [\n" +
                    "        {\n" +
                    "            \"id\": \"applevas\",\n" +
                    "            \"label\": \"Apple VAS\",\n" +
                    "\n" +
                    "            \"type\": \"apple_vas\",\n" +
                    "\n" +
                    "            \"vas_mode\": \"vas_only\",\n" +
                    "            \"terminal_type\": \"payment\",\n" +
                    "            \"protocol_version\": 1,\n" +
                    "            \"protocol_mode\": \"full\",\n" +
                    "            \"vas_supported\": true,\n" +
                    "            \"auth_required\": false,\n" +
                    "\n" +
                    "            \"active\": [\"passkit\"],\n" +
                    "            \"nonce\": \"12345678\",\n" +
                    "\n" +
                    "            \"merchants\": [\n" +
                    "                {\n" +
                    "                    \"id\": \"passkit\",\n" +
                    "                    \"label\": \"PassKit\",\n" +
                    "                    \"pass_type_identifier\": \"${passTypeIdentifier}\",\n" + //.test1
                    "                    \"signup_url\": \"https://apple.com\",\n" +
                    "                    \"crypto_providers\": [\n" +
                    "                        {\n" +
                    "                            \"type\": \"regular\",\n" +
                    "                            \"keys\": [\n" +
                    "                                \"${userPrivatePEM}\"" +
                    "                            ]\n" +
                    "                        }\n" +
                    "                    ]\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        },\n" +
                    "\n" +
                    "        {\n" +
                    "            \"id\": \"googlesmarttap-google\",\n" +
                    "            \"label\": \"Google Smart Tap (Google)\",\n" +
                    "            \"type\": \"google_smart_tap\",\n" +
                    "\n" +
                    "            \"system_flags\": [\"zlib_supported\"],\n" +
                    "\n" +
                    "            \"mode\": \"vas_or_pay\",\n" +
                    "            \n" +
                    "            \"session_id\": null,\n" +
                    "            \"nonce\": null,\n" +
                    "            \"ephemeral_key\": null,\n" +
                    "            \"collector\": {\n" +
                    "                \"collector_id\": ${collectorId},\n" +
                    "                \"crypto_providers\": [\n" +
                    "                    {\n" +
                    "                        \"type\": \"regular\",\n" +
                    "                        \"keys\": {\n" +
                    "                            \"1\": \"${userPrivatePEM}\"" +
                    "                        }\n" +
                    "                    }\n" +
                    "                ]\n" +
                    "            }\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
        }



        /**
         * Loads a raw resource file into a String.
         *
         * @param context The context to access resources.
         * @param resourceId The resource ID of the raw file (e.g., R.raw.my_config).
         * @return The content of the file as a String, or null if an error occurs.
         */
        fun loadRawResourceToString(context: Context, resourceId: Int): String? {
            val inputStream: InputStream = context.resources.openRawResource(resourceId)
            return try {
                // More concise way to read the entire stream to a String
                inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: Exception) {
                Log.e("RawResourceLoader", "Error reading raw resource: $resourceId", e)
                null // Or throw the exception, or return a default string, depending on your error handling needs
            } finally {
                // The 'use' block on bufferedReader should handle closing the inputStream,
                // but an explicit close here is a fallback if 'use' isn't used or for the raw stream itself.
                // However, with '.use', this explicit close is somewhat redundant for the wrapped streams.
                // For the raw inputStream, if not wrapped by 'use', it would be essential.
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    Log.e("RawResourceLoader", "Error closing input stream for resource: $resourceId", e)
                }
            }
        }
    }
}