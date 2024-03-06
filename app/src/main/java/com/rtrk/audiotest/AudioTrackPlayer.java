package com.rtrk.audiotest;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioTrackPlayer {
    AudioTrack mTrack;
    boolean mRunning = false;
    PlaybackThread mThread;

    final int SINUS_SCALE = 200;

    private static void printLog(String str) {
        android.util.Log.d("zzz", str);
    }

    class PlaybackThread extends Thread {
        @Override
        public void run() {
            printLog("AT playbackThread BEGIN");
            final int size = 1024;
            byte[] buffer = new byte[size];
            while (mRunning)
            {
                for (int i = 0; i < size; i++)
                {
                    double x = (double)i / (double)size * 2 * 3.1415 * SINUS_SCALE;
                    double y = Math.sin(x);
                    buffer[i] = (byte)(y * 255);
                }
                mTrack.write(buffer, 0, size, AudioTrack.WRITE_BLOCKING);
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
            }
            printLog("AT playbackThread END");
        }
    }

    public void start() {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;

        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        AudioFormat af = new AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build();

        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

        mTrack = new AudioTrack(aa, af, minBufferSize, AudioTrack.MODE_STREAM, 0);

        mTrack.play();

        mRunning = true;
        mThread = new PlaybackThread();
        mThread.start();
    }

    public void stop() {
        mRunning = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        mTrack.stop();
        mTrack.release();
        mTrack = null;
    }
}