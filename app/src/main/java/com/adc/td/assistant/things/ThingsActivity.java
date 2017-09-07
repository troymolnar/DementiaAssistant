package com.adc.td.assistant.things;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;

import com.adc.td.assistant.voice.SpeechCallback;
import com.adc.td.assistant.voice.SpeechWrapper;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

import ai.api.model.AIError;
import ai.api.model.AIResponse;

/**
 * @author Troy Molnar.
 */

public class ThingsActivity extends AppCompatActivity {
    private static final String TAG = ThingsActivity.class.getSimpleName();

    private Gpio ledGpio;
    private ButtonInputDriver buttonInputDriver;
    private SpeechWrapper speechWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting ThingsActivity");

        initIO();
        speechWrapper = new SpeechWrapper(this);
        speechWrapper.addCallback(callback);
        speechWrapper.bindService();
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        Log.i(TAG, "onKeyDown: " + keyCode);
//        if (keyCode == KeyEvent.KEYCODE_SPACE) {
//            // Turn on the LED
//            setLedValue(true);
//            return true;
//        }
//
//        return super.onKeyDown(keyCode, event);
//    }


//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        Log.i(TAG, "onKeyUp: " + keyCode);
//        if (keyCode == KeyEvent.KEYCODE_SPACE) {
//            // Turn off the LED
//            setLedValue(false);
//            return true;
//        }
//
//        return super.onKeyUp(keyCode, event);
//    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyUp: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            speechWrapper.startListening();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void initIO() {
        PeripheralManagerService pioService = new PeripheralManagerService();

        try {
            Log.i(TAG, "Configuring GPIO pins");
            ledGpio = pioService.openGpio(PicoPorts.GPIO_LED_LISTENING);
            ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            Log.i(TAG, "Registering button driver");
            // Initialize and register the InputDriver that will emit SPACE key events
            // on GPIO state changes.
            buttonInputDriver = new ButtonInputDriver(
                    PicoPorts.GPIO_BUTTON_MIC_ON,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_SPACE);
            buttonInputDriver.register();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
        }
    }

    /**
     * Update the value of the LED output.
     */
    private void setLedValue(boolean value) {
        try {
            ledGpio.setValue(value);
        } catch (IOException e) {
            Log.e(TAG, "Error updating GPIO value", e);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (buttonInputDriver != null) {
            buttonInputDriver.unregister();
            try {
                buttonInputDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Button driver", e);
            } finally{
                buttonInputDriver = null;
            }
        }

        if (ledGpio != null) {
            try {
                ledGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing LED GPIO", e);
            } finally{
                ledGpio = null;
            }
            ledGpio = null;
        }
    }

    private SpeechCallback callback = new SpeechCallback() {
        @Override
        public void onSpeechServiceConnected(boolean ready) {

        }

        @Override
        public void onListeningStarted() {
            setLedValue(true);
        }

        @Override
        public void onListeningPartial(@NonNull final String partialQuery) {

        }

        @Override
        public void onListeningFinished(@NonNull final String fullQuery) {
            setLedValue(false);
        }

        @Override
        public void onAIResponse(@NonNull final AIResponse aiResponse) {

        }

        @Override
        public void onError(@NonNull final AIError error) {

        }

        @Override
        public void onException(@NonNull final Exception e) {

        }
    };
}
