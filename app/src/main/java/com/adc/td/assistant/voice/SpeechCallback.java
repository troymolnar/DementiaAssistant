package com.adc.td.assistant.voice;

import android.support.annotation.NonNull;

import ai.api.model.AIError;
import ai.api.model.AIResponse;


public interface SpeechCallback {
    void onSpeechServiceConnected(boolean ready);

    void onListeningStarted();

    void onListeningPartial(@NonNull String partialQuery);

    void onListeningFinished(@NonNull String fullQuery);

    void onAIResponse(@NonNull AIResponse aiResponse);

    void onError(@NonNull AIError error);

    void onException(@NonNull Exception e);
}
