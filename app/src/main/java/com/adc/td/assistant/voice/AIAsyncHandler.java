package com.adc.td.assistant.voice;

import android.content.Context;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

@SuppressWarnings("WeakerAccess")
public class AIAsyncHandler implements AIListener {
    private static final String TAG = AIAsyncHandler.class.getSimpleName();

    private AIService aiService;
    private TextToSpeech tts;
    private SpeechCallback callback;
    private Exception exception = null;
    private AIAsyncTask aiAsyncTask = null;


    public AIAsyncHandler(@NonNull final Context context, @NonNull AIConfiguration config,
        @NonNull SpeechCallback callback) {
        FirebaseApp.initializeApp(context);
        this.aiService = AIService.getService(context, config);
        this.aiService.setListener(this);
        this.callback = callback;
        this.tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                Toast.makeText(context, "tts has initialized", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void submitRequest(@NonNull AIRequest aiRequest) {
        if (aiAsyncTask != null) {
            aiAsyncTask.cancel(true);
        }
        aiAsyncTask = new AIAsyncTask();
        aiAsyncTask.execute(aiRequest);
    }

    @Override
    public void onResult(AIResponse aiResponse) {
        Result result = aiResponse.getResult();
        String speech = result.getFulfillment().getSpeech();
        String resolvedQuery = result.getResolvedQuery();

        Log.d(TAG, "resolved recognizedText: " + resolvedQuery);
        Log.d(TAG, "speech: " + speech);
        Log.d(TAG, "action: " + result.getAction());

        tts.speak(speech, QUEUE_FLUSH, null, "UTTERANCEID" + System.currentTimeMillis());

        try {
            // Write a message to the database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("Dementia Logs");
            myRef.push().setValue(resolvedQuery + " : " + speech);
        } catch (Exception e) {
            //todo: this doesn't work on Android Things, so much log
//            Log.e(TAG, "Problem calling Firebase", e);
        }

        callback.onAIResponse(aiResponse);
    }

    @Override
    public void onError(AIError error) {
        callback.onError(error);
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {
        // only using AI for text, not for build in recognizer
    }

    @Override
    public void onListeningCanceled() {
        // only using AI for text, not for build in recognizer
    }

    @Override
    public void onListeningFinished() {
        // only using AI for text, not for build in recognizer
    }

    private class AIAsyncTask extends AsyncTask<AIRequest, Void, AIResponse> {
        @Override
        protected AIResponse doInBackground(AIRequest... requests) {
            final AIRequest request = requests[0];
            try {
                return aiService.textRequest(request);
            } catch (AIServiceException e) {
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(AIResponse aiResponse) {
            if (aiResponse != null) {
                // process aiResponse here
                onResult(aiResponse);
            } else if (exception != null) {
                callback.onException(exception);
            }
            aiAsyncTask = null;
        }
    }
}
