package com.adc.td.assistant.voice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.adc.td.assistant.voice.google.SpeechService;
import com.adc.td.assistant.voice.google.VoiceRecorder;
import com.google.firebase.FirebaseApp;

import ai.api.android.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

import java.util.ArrayList;
import java.util.List;

public class SpeechWrapper implements SpeechCallback {

    private static final String TAG = SpeechWrapper.class.getSimpleName();

    private static final String CLIENT_API_TOKEN = "4bc68760f7b34a458d4acaf4a4552e10";

    private Context context;
    private AIAsyncHandler aiAsyncHandler;
    private SpeechService speechService;
    private VoiceRecorder voiceRecorder;
    private String recognizedText = "";

    private List<SpeechCallback> callbackList;

    public SpeechWrapper(@NonNull final Context context) {
        this.context = context;
        final AIConfiguration config = new AIConfiguration(CLIENT_API_TOKEN,
            AIConfiguration.SupportedLanguages.English,
            AIConfiguration.RecognitionEngine.System);
        aiAsyncHandler = new AIAsyncHandler(context, config, this);

        callbackList = new ArrayList<>();
    }

    public void addCallback(@NonNull SpeechCallback callback) {
        callbackList.add(callback);
    }

    public void bindService() {
        context.bindService(new Intent(context, SpeechService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void startListening() {
        Log.d(TAG, "voice recorder started");
        if (voiceRecorder != null) {
            voiceRecorder.stop();
        }
        recognizedText = "";
        onListeningPartial(recognizedText);
        voiceRecorder = new VoiceRecorder(voiceCallback);
        voiceRecorder.start();
    }

    public void stopListening() {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
            voiceRecorder = null;
        }
        onListeningFinished(recognizedText);
    }

    public void sendAiRequest(@NonNull String query) {
        AIRequest request;
        try {
            request = new AIRequest(query);
        } catch (Exception e) {
            Log.e(TAG, "failed to create AiRequest", e);
            onException(e);
            return;
        }
        aiAsyncHandler.submitRequest(request);
    }

    private final VoiceRecorder.Callback voiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            Log.d(TAG, "onVoiceStart");
            onListeningStarted();
            if (speechService != null) {
                speechService.startRecognizing(voiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (speechService != null) {
                speechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            Log.d(TAG, "onVoiceEnd");
            if (speechService != null) {
                speechService.finishRecognizing();
            }
            //            stopListening();
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            Log.d(TAG, "onServiceConnected");
            speechService = SpeechService.from(binder);
            speechService.addListener(speechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            speechService = null;
        }

    };

    private final SpeechService.Listener speechServiceListener =
        new SpeechService.Listener() {
            @Override
            public void onSpeechRecognized(final String text, final boolean isFinal) {
                recognizedText = text;
                if (isFinal) {
                    voiceRecorder.dismiss();
                }
                if (!TextUtils.isEmpty(text)) {
                    if (isFinal) {
                        onListeningFinished(recognizedText);
                        sendAiRequest(text);
                    } else {
                        onListeningPartial(recognizedText);
                    }
                }
            }
        };

    @Override
    public void onListeningStarted() {
        for (SpeechCallback callback : callbackList) {
            callback.onListeningStarted();
        }
    }

    @Override
    public void onListeningPartial(@NonNull String partialQuery) {
        for (SpeechCallback callback : callbackList) {
            callback.onListeningPartial(partialQuery);
        }
    }

    @Override
    public void onListeningFinished(@NonNull String fullQuery) {
        for (SpeechCallback callback : callbackList) {
            callback.onListeningFinished(fullQuery);
        }
    }

    @Override
    public void onAIResponse(@NonNull AIResponse aiResponse) {
        for (SpeechCallback callback : callbackList) {
            callback.onAIResponse(aiResponse);
        }
    }

    @Override
    public void onError(@NonNull AIError error) {
        for (SpeechCallback callback : callbackList) {
            callback.onError(error);
        }
    }

    @Override
    public void onException(@NonNull Exception e) {
        for (SpeechCallback callback : callbackList) {
            callback.onException(e);
        }
    }

}
