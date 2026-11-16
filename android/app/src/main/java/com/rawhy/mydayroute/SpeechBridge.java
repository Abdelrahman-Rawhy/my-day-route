package com.rawhy.mydayroute;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;

/** Live speech-to-text with Android's built-in recognizer. Restarts itself so long voice notes keep transcribing. */
@CapacitorPlugin(name = "SpeechBridge")
public class SpeechBridge extends Plugin {
    private SpeechRecognizer sr;
    private boolean running = false;
    private String lang = "ar-EG";
    private final StringBuilder done = new StringBuilder();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @PluginMethod
    public void available(PluginCall call) {
        JSObject r = new JSObject();
        r.put("available", SpeechRecognizer.isRecognitionAvailable(getContext()));
        call.resolve(r);
    }

    @PluginMethod
    public void start(PluginCall call) {
        lang = call.getString("lang", "ar-EG");
        done.setLength(0);
        running = true;
        ui.post(this::listen);
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        running = false;
        ui.post(() -> {
            if (sr != null) { try { sr.stopListening(); sr.destroy(); } catch (Exception e) {} sr = null; }
            JSObject r = new JSObject();
            r.put("text", done.toString().trim());
            call.resolve(r);
        });
    }

    private void emit(String partial, String err) {
        JSObject r = new JSObject();
        r.put("text", (done.toString() + (partial == null ? "" : partial)).trim());
        if (err != null) r.put("error", err);
        notifyListeners("partial", r);
    }

    private void again() { if (running) ui.postDelayed(this::listen, 120); }

    private void listen() {
        if (!running) return;
        try {
            if (sr != null) sr.destroy();
            sr = SpeechRecognizer.createSpeechRecognizer(getContext());
            sr.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) {}
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float v) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() {}
                public void onEvent(int t, Bundle b) {}
                public void onPartialResults(Bundle b) {
                    ArrayList<String> L = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (L != null && !L.isEmpty()) emit(L.get(0), null);
                }
                public void onResults(Bundle b) {
                    ArrayList<String> L = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (L != null && !L.isEmpty() && L.get(0).length() > 0) done.append(L.get(0)).append(' ');
                    emit(null, null);
                    again();
                }
                public void onError(int e) {
                    if (e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) { again(); return; }
                    if (e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) { if (running) ui.postDelayed(SpeechBridge.this::listen, 600); return; }
                    running = false;
                    emit(null, "code" + e);
                }
            });
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
            i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
            sr.startListening(i);
        } catch (Exception ex) {
            running = false;
            emit(null, "start");
        }
    }
}
