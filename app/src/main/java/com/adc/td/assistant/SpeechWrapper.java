package com.adc.td.assistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

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
public class SpeechWrapper {

    private static final String TAG = SpeechWrapper.class.getSimpleName();

    private static final String CLIENT_API_TOKEN = "4bc68760f7b34a458d4acaf4a4552e10";

    private Context context;
    private TextToSpeech tts;
    private AIService aiService;
    private SpeechService speechService;
    private VoiceRecorder voiceRecorder;

    private List<Callback> callbackList;

    public SpeechWrapper(@NonNull final Context context) {
        this.context = context;
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                Toast.makeText(context, "tts has initialized", Toast.LENGTH_SHORT).show();
            }
        });

        final AIConfiguration config = new AIConfiguration(CLIENT_API_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(context, config);
        aiService.setListener(aiListener);

        FirebaseApp.initializeApp(context);

        callbackList = new ArrayList<>();
    }

    public void addCallback(@NonNull Callback callback) {
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
        voiceRecorder = new VoiceRecorder(voiceCallback);
        voiceRecorder.start();
    }

    public void stopListening() {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
            voiceRecorder = null;
        }
    }

    public void sendAiRequest(@NonNull String query) {
        AIRequest request;
        try {
            request = new AIRequest(query);
        } catch (Exception e) {
            Log.e(TAG, "failed to create AiRequest", e);
            for (Callback callback : callbackList) {
                callback.onException(e);
            }
            return;
        }
        new AsyncTask<AIRequest, Void, AIResponse>() {
            private Exception exception = null;

            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    return aiService.textRequest(request);
                } catch (AIServiceException e) {
                    exception = e;
                    Log.e(TAG, "error sending text request", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                    // process aiResponse here
                    aiListener.onResult(aiResponse);
                } else if (exception != null) {
                    for (Callback callback : callbackList) {
                        callback.onException(exception);
                    }
                }
            }
        }.execute(request);
    }

    private AIListener aiListener = new AIListener() {
        @Override
        public void onResult(AIResponse aiResponse) {
            Result result = aiResponse.getResult();
            String speech = result.getFulfillment().getSpeech();
            String resolvedQuery = result.getResolvedQuery();

            Log.d(TAG, "resolved query: " + resolvedQuery);
            Log.d(TAG, "speech: " + speech);
            Log.d(TAG, "action: " + result.getAction());

            tts.speak(speech, QUEUE_FLUSH, null, "UTTERANCEID" + System.currentTimeMillis());

            try {
                // Write a message to the database
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("Dementia Logs");
                myRef.push().setValue(resolvedQuery + " : " + speech);
            } catch (Exception e) {
                Log.e(TAG, "Problem calling Firebase", e);
            }

            for (Callback callback : callbackList) {
                callback.onComplete(aiResponse);
            }
        }

        @Override
        public void onError(AIError error) {
            Log.e(TAG, "AILISTENER onError: " + error.getMessage());
            for (Callback callback : callbackList) {
                callback.onError(error);
            }
        }

        @Override
        public void onAudioLevel(float level) {
            // only using AI for text, not for build in recognizer
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
    };

    private final VoiceRecorder.Callback voiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            Log.d(TAG, "onVoiceStart");
            for (Callback callback : callbackList) {
                callback.onListeningStarted();
            }
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
//            for (Callback callback : callbackList) {
//                callback.onListeningFinished();
//            }
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
                    if (isFinal) {
                        voiceRecorder.dismiss();
                    }
                    if (!TextUtils.isEmpty(text)) {
                        if (isFinal) {
                            for (Callback callback : callbackList) {
                                callback.onListeningFinished(text);
                            }
                            sendAiRequest(text);
                        } else {
                            for (Callback callback : callbackList) {
                                callback.onListeningPartial(text);
                            }
                        }
                    }
                }
            };


    public interface Callback {
        void onListeningStarted();

        void onListeningPartial(@NonNull String partialQuery);

        void onListeningFinished(@NonNull String fullQuery);

        void onComplete(@NonNull AIResponse aiResponse);

        void onError(@NonNull AIError error);

        void onException(@NonNull Exception e);
    }

}
