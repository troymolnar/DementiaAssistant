package com.adc.td.assistant;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

public class MainActivity extends AppCompatActivity implements AIListener, TextToSpeech.OnInitListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String CLIENT_API_TOKEN = "4bc68760f7b34a458d4acaf4a4552e10";

    TextView instructionView;
    EditText queryText;
    TextView resultView;
    TextToSpeech tts;

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


        final AIConfiguration config = new AIConfiguration(CLIENT_API_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        final AIService aiService = AIService.getService(this, config);

        aiService.setListener(this);


        micOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aiService.startListening();
            }
        });

        micOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                aiService.stopListening();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            private Exception exception = null;

            @Override
            public void onClick(View view) {
                AIRequest request = new AIRequest(queryText.getText().toString());
                new AsyncTask<AIRequest, Void, AIResponse>() {
                    @Override
                    protected AIResponse doInBackground(AIRequest... requests) {
                        final AIRequest request = requests[0];
                        try {
                            return aiService.textRequest(request);
                        } catch (AIServiceException e) {
                            exception = e;
                            Log.e(TAG, "error sending text request", e);
                        }
                        ;
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
}
