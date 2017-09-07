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

    private Gpio readyLedGpio;
    private Gpio listeningLedGpio;
    private ButtonInputDriver micOnInputDriver;
    private ButtonInputDriver micOffInputDriver;
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyUp: " + keyCode);
        switch(keyCode) {
            case KeyEvent.KEYCODE_SPACE:
                speechWrapper.startListening();
                return true;
            case KeyEvent.KEYCODE_STAR:
                speechWrapper.stopListening();
                return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void initIO() {
        PeripheralManagerService pioService = new PeripheralManagerService();

        try {
            Log.i(TAG, "Configuring GPIO pins");
            readyLedGpio = pioService.openGpio(PicoPorts.GPIO_LED_READY);
            readyLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            listeningLedGpio = pioService.openGpio(PicoPorts.GPIO_LED_LISTENING);
            listeningLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            Log.i(TAG, "Registering button driver");
            // Initialize and register the InputDrivers that will emit key events on GPIO state changes.
            micOnInputDriver = new ButtonInputDriver(
                    PicoPorts.GPIO_BUTTON_MIC_ON,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_SPACE);
            micOnInputDriver.register();
            micOffInputDriver = new ButtonInputDriver(
                    PicoPorts.GPIO_BUTTON_MIC_OFF,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_STAR);
            micOffInputDriver.register();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
        }
    }

    /**
     * Update the value of the LED output.
     */
    private void setLedValue(@NonNull Gpio led, boolean value) {
        try {
            led.setValue(value);
        } catch (IOException e) {
            Log.e(TAG, "Error updating GPIO value", e);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (readyLedGpio != null) {
            try {
                readyLedGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing readyLedGpio", e);
            } finally{
                readyLedGpio = null;
            }
            readyLedGpio = null;
        }
        if (listeningLedGpio != null) {
            try {
                listeningLedGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing listeningLedGpio", e);
            } finally{
                listeningLedGpio = null;
            }
            listeningLedGpio = null;
        }

        if (micOnInputDriver != null) {
            micOnInputDriver.unregister();
            try {
                micOnInputDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing micOnInputDriver", e);
            } finally{
                micOnInputDriver = null;
            }
        }
        if (micOffInputDriver != null) {
            micOffInputDriver.unregister();
            try {
                micOffInputDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing micOffInputDriver", e);
            } finally{
                micOffInputDriver = null;
            }
        }
    }

    private SpeechCallback callback = new SpeechCallback() {
        @Override
        public void onSpeechServiceConnected(boolean ready) {
            setLedValue(readyLedGpio, ready);
        }

        @Override
        public void onListeningStarted() {
            setLedValue(listeningLedGpio, true);
        }

        @Override
        public void onListeningPartial(@NonNull final String partialQuery) {

        }

        @Override
        public void onListeningFinished(@NonNull final String fullQuery) {
            setLedValue(listeningLedGpio, false);
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
