/**
 * Copyright (c) Microsoft. All rights reserved. Licensed under the MIT license.
 * See LICENSE.md file in the project root for full license information.
 */

package com.speechsdk.quickstart;

import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.PullAudioOutputStream;
import com.microsoft.cognitiveservices.speech.dialog.DialogConfig;
import com.microsoft.cognitiveservices.speech.dialog.DialogConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        // Please replace below with your speech channel secret, speech
        // subscription key, and service region.
        //
        // Note: In preview, the Direct Line Speech channel currently supports only the `westus2` region.
        final String channelSecret = "YourChannelSecret";
        final String subscriptionKey = "YourSubscriptionKey";
        final String region = "YourServiceRegion";

        assert !channelSecret.equals("YourChannelSecret") : "Replace the string \"YourChannelSecret\" with your speech channel secret.";
        assert !subscriptionKey.equals("YourSubscriptionKey") : "Replace the string \"YourSubscriptionKey\" with your speech subscription key.";
        assert !region.equals("YourServiceRegion") : "Replace the string \"YourServiceRegion\" with your service region.";

        // Create a DialogConfig instance from channel secret, subscription key and region
        final DialogConfig dialogConfig = DialogConfig.fromBotSecret(channelSecret, subscriptionKey, region);
        if (dialogConfig == null) {
            log.error("DialogConfig should not be null");
        }

        // Set audio input from microphone.
        final AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();

        // Create a DialogConnector instance
        final DialogConnector dialogConnector = new DialogConnector(dialogConfig, audioConfig);

        // Configure all event listeners.
        registerEventListeners(dialogConnector);

        try {
            // Connect to the backing dialog.
            dialogConnector.connectAsync();
            log.info("DialogConnector is successfully connected");

            // Start listening.
            System.out.println("Say something ...");
            dialogConnector.listenOnceAsync();
        } catch (Exception e) {
            log.error("Exception thrown when connecting to DialogConnector. ErrorMessage:", e.getMessage(), e);

            // Disconnect from the dialog.
            dialogConnector.disconnectAsync();
        }
    }

    private static void registerEventListeners(final DialogConnector dialogConnector) {
        // Recognizing will provide the intermediate recognized text while an audio stream is being processed
        dialogConnector.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
            log.info("Recognizing speech event text: {}", speechRecognitionResultEventArgs.getResult().getText());
        });

        // Recognized will provide the final recognized text once audio capture is completed
        dialogConnector.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
            if (speechRecognitionResultEventArgs.getResult().getText().trim().equals("")) {
                log.warn("No speech was recognized. Try running the program again.");
            } else {
                log.info("Recognized speech event text: {}", speechRecognitionResultEventArgs.getResult().getText());
            }
        });

        // SessionStarted will notify when audio begins flowing to the service for a turn
        dialogConnector.sessionStarted.addEventListener((o, sessionEventArgs) -> {
            log.info("Session started event. Session id: {} ", sessionEventArgs.getSessionId());
        });

        // SessionStopped will notify when a turn is complete
        dialogConnector.sessionStopped.addEventListener((o, sessionEventArgs) -> {
            log.info("Session stopped event. Session id: {}", sessionEventArgs.getSessionId());
        });

        // Canceled will be signaled when a turn is aborted or experiences an error condition
        dialogConnector.canceled.addEventListener((o, canceledEventArgs) -> {
            log.info("Canceled event details: {}", canceledEventArgs.getErrorDetails());
            dialogConnector.disconnectAsync();
        });

        // ActivityReceived is the main way your bot will communicate with the client and uses bot framework activities
        dialogConnector.activityReceived.addEventListener((o, activityEventArgs) -> {
            final String act = activityEventArgs.getActivity().serialize();
            log.info("Received activity {} audio: {}", activityEventArgs.hasAudio() ? "with" : "without", act);
            if (activityEventArgs.hasAudio()) {
                System.out.println("Starting playback.");
                try {
                    PullAudioOutputStream response = activityEventArgs.getAudio();
                    ActivityAudioStream activityAudioStream = new ActivityAudioStream(response);

                    final ActivityAudioStream.ActivityAudioFormat audioFormat = activityAudioStream.getActivityAudioFormat();
                    final AudioFormat defaultAudioFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            audioFormat.getSamplesPerSecond(),
                            audioFormat.getBitsPerSample(),
                            audioFormat.getChannels(),
                            audioFormat.getFrameSize(),
                            audioFormat.getSamplesPerSecond(),
                            false);
                    playStream(activityAudioStream, defaultAudioFormat);
                } catch (Exception e) {
                    log.error("Exception thrown during playback. ErrorMessage: ", e.getMessage());
                }
            }
        });
    }

    public static void playStream(final InputStream stream, final AudioFormat format) throws Exception {
        int bufferSize = format.getFrameSize();
        final byte[] data = new byte[bufferSize];

        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);

        if (line != null) {
            line.start();
            int nBytesRead = 0;
            while (nBytesRead != -1) {
                nBytesRead = stream.read(data);
                if (nBytesRead != -1) {
                    line.write(data, 0, nBytesRead);
                }
            }
            line.drain();
            line.stop();
            line.close();
        }
        stream.close();
    }
}
