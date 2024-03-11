package com.rtrk.audiotest;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioTrackPlayer implements IPlayer {
    AudioTrack mTrack;
    boolean mRunning = false;
    PlaybackThread mThread;

    final int SINUS_SCALE = 200;

    private static void printLog(String str) {
        android.util.Log.d("audiotest", str);
    }

    class PlaybackThread extends Thread {
        @Override
        public void run() {
            printLog("AudioTrack playback thread started");
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
            printLog("AudioTrack playback thread stopped");
        }
    }

    public AudioTrackPlayer(int audioUsage) {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int contentType = AudioAttributes.CONTENT_TYPE_MUSIC;

        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(audioUsage)
                .setContentType(contentType)
                .build();

        AudioFormat af = new AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build();

        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        mTrack = new AudioTrack(aa, af, minBufferSize, AudioTrack.MODE_STREAM, 0);
        printLog("AudioTrack player created: sample_rate=" + sampleRate + " channel_config="
                + channelConfig + " format=" + audioFormat + " usage=" + audioUsage + " content=" + contentType);
    }

    public void start() {
        if (mRunning) {
            printLog("AudioTrack player already started!");
            return;
        }

        if (mTrack == null) {
            printLog("AudioTrack player not created!");
            return;
        }

        mTrack.play();

        mRunning = true;
        mThread = new PlaybackThread();
        mThread.start();
    }

    public void stop() {
        if (!mRunning) {
            printLog("AudioTrack player not started!");
            return;
        }

        mRunning = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        mTrack.stop();
    }

    @Override
    public void release() {
        if (mTrack == null) {
            printLog("AudioTrack player already released!");
            return;
        }

        if (mRunning) {
            stop();
        }

        mTrack.release();
        mTrack = null;
        printLog("AudioTrack player destroyed");
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public boolean isLowLatency() {
        return false;
    }

    @Override
    public boolean isMMap() {
        return false;
    }
}
