package com.adc.td.assistant;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebaseDementia.R;
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

import java.util.List;

public class MainActivity extends AppCompatActivity implements AIListener, TextToSpeech.OnInitListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String CLIENT_API_TOKEN = "4bc68760f7b34a458d4acaf4a4552e10";

    private static final int SPEECH_REQUEST_CODE = 2525;

    TextView instructionView;
    EditText queryText;
    TextView resultView;
    TextToSpeech tts;
    AIService aiService;

//    SpeechClient speech = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instructionView = (TextView) findViewById(R.id.instruction);
        queryText = (EditText) findViewById(R.id.editText);
        Button micOnButton = (Button) findViewById(R.id.mic_on);
        Button micOffButton = (Button) findViewById(R.id.mic_off);
        Button okButton = (Button) findViewById(R.id.button);
        resultView = (TextView) findViewById(R.id.result);

        tts = new TextToSpeech(this, this);

//        try {
//            speech = SpeechClient.create();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        speech.




        final AIConfiguration config = new AIConfiguration(CLIENT_API_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);

        aiService.setListener(this);


        micOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpeechToTextActivity();
            }
        });

//        micOnButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                aiService.startListening();
//            }
//        });

//        micOffButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                aiService.stopListening();
//            }
//        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendAiRequest(queryText.getText().toString());
            }
        });
    }


    ////////////////////////////////////AILISTENER////////////////////////////////////
    @Override
    public void onResult(AIResponse aiResponse) {


        StringBuilder builder = new StringBuilder();
        Result result = aiResponse.getResult();
        String speech = result.getFulfillment().getSpeech();
        String resolvedQuery = result.getResolvedQuery();
        builder
                .append("QUERY: " + resolvedQuery + "\n\n")
                .append("ACTION: " + result.getAction() + "\n\n")
                .append("STATUS: " + aiResponse.getStatus() + "\n\n")
                .append("RESPONSE: " + speech + "\n\n")
                .append("RAW: " + aiResponse);
        queryText.setText(resolvedQuery);
        resultView.setText(builder.toString());
        tts.speak(speech, QUEUE_FLUSH, null, "UTTERANCEID"+System.currentTimeMillis());

        try {
            // Write a message to the database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("Dementia Logs");

            myRef.push().setValue(resolvedQuery + " : " + speech );
        }catch(Exception e) {
            Log.e("crash", "Error", e);
        }

        Log.d("firebase logs", "Save " + resolvedQuery + " " + speech + " " + result.getAction());
    }

    @Override
    public void onError(AIError error) {
        instructionView.setText(R.string.default_instruction);
        resultView.setText("ERROR: " + error.getMessage());
        Log.e(TAG, "AILISTENER onError: " + error.getMessage());
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {
        instructionView.setText(R.string.listening);
        queryText.setText(null);
    }

    @Override
    public void onListeningCanceled() {
        instructionView.setText(R.string.default_instruction);
    }

    @Override
    public void onListeningFinished() {
        instructionView.setText(R.string.listening_finished);
    }

    @Override
    public void onInit(int i) {
        Toast.makeText(this, "tts has initialized", Toast.LENGTH_SHORT).show();
    }


    private void sendAiRequest(String query) {
        AIRequest request = new AIRequest(query);
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
                    onResult(aiResponse);
                } else if (exception != null) {
                    resultView.setText("TEXT ERROR: " + exception.getMessage());
                }
            }
        }.execute(request);
    }

    private void startSpeechToTextActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Log.e(TAG, "Your device does not support Speech to Text");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                List<String> result =
                        data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String spokenText = result.get(0);
                sendAiRequest(spokenText);
            }
        }
    }
}
