package com.adc.td.assistant.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.adc.td.assistant.voice.SpeechCallback;
import com.adc.td.assistant.voice.SpeechWrapper;
import com.firebaseDementia.R;

import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    TextView instructionView;
    EditText queryText;
    TextView resultView;
    Button connectButton;
    Button micOnButton;
    Button micOffButton;

    SpeechWrapper speechWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instructionView = findViewById(R.id.instruction);
        queryText = findViewById(R.id.editText);
        connectButton = findViewById(R.id.connect);
        micOnButton = findViewById(R.id.mic_on);
        micOffButton = findViewById(R.id.mic_off);
        Button submitButton = findViewById(R.id.submit);
        resultView = findViewById(R.id.result);

        speechWrapper = new SpeechWrapper(this);
        speechWrapper.addCallback(callback);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speechWrapper.bindService();
            }
        });

        micOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openVoiceRecording();
            }
        });

        micOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speechWrapper.stopListening();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speechWrapper.sendAiRequest(queryText.getText().toString());
            }
        });
    }

    private void openVoiceRecording() {
        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            speechWrapper.startListening();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private SpeechCallback callback = new SpeechCallback() {
        @Override
        public void onSpeechServiceConnected(boolean ready) {
            if (ready) {
                Toast.makeText(MainActivity.this,
                        R.string.cloud_speech_ready, Toast.LENGTH_SHORT).show();
                connectButton.setVisibility(View.GONE);

            } else {
                Toast.makeText(MainActivity.this,
                        R.string.cloud_speech_disconnected, Toast.LENGTH_SHORT).show();
                connectButton.setVisibility(View.VISIBLE);
            }
            micOnButton.setEnabled(ready);
            micOffButton.setEnabled(ready);
        }

        @Override
        public void onListeningStarted() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    instructionView.setText(R.string.listening);
                    queryText.setText(null);
                }
            });
        }

        @Override
        public void onListeningPartial(@NonNull final String partialQuery) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    queryText.setText(partialQuery);
                }
            });
        }

        @Override
        public void onListeningFinished(@NonNull final String fullQuery) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    queryText.setText(fullQuery);
                    instructionView.setText(R.string.listening_finished);
                }
            });
        }

        @Override
        public void onAIResponse(@NonNull final AIResponse aiResponse) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder builder = new StringBuilder();
                    Result result = aiResponse.getResult();
                    String speech = result.getFulfillment().getSpeech();
                    String resolvedQuery = result.getResolvedQuery();
                    builder
                            .append("QUERY: ").append(resolvedQuery).append("\n\n")
                            .append("ACTION: ").append(result.getAction()).append("\n\n")
                            .append("STATUS: ").append(aiResponse.getStatus()).append("\n\n")
                            .append("RESPONSE: ").append(speech).append("\n\n").append("RAW: ")
                            .append(aiResponse);
                    queryText.setText(resolvedQuery);
                    resultView.setText(builder.toString());
                }
            });
        }

        @Override
        public void onError(@NonNull final AIError error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    instructionView.setText(R.string.default_instruction);
                    resultView.setText(String.format("ERROR: %s", error.getMessage()));
                }
            });
        }

        @Override
        public void onException(@NonNull final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    instructionView.setText(R.string.default_instruction);
                    resultView.setText(String.format("ERROR: %s", e.getMessage()));
                }
            });
        }
    };

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }
}
