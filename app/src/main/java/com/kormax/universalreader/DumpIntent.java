package com.kormax.universalreader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Set;

public class DumpIntent {

    private static final String TAG = "IntentDump";

    public static void dumpIntent(Intent i){
        Bundle bundle = i.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IntentDump \n\r");
            stringBuilder.append("-------------------------------------------------------------\n\r");

            for (String key : keys) {
                stringBuilder.append(key).append("=").append(bundle.get(key)).append("\n\r");
            }

            stringBuilder.append("-------------------------------------------------------------\n\r");
            Log.i(TAG, stringBuilder.toString());
        }
    }
}