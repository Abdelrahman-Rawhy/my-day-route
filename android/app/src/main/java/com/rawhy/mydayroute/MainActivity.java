package com.rawhy.mydayroute;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetBridge.class);
        registerPlugin(SpeechBridge.class);
        registerPlugin(ApkUpdater.class);
        super.onCreate(savedInstanceState);
    }
}
