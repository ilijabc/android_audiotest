package com.rtrk.audiotest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("audiotest");
    }

    native private void sayHello();
    native private void startAAudioPlayback();
    native private void stopAAudioPlayback();

    LinearLayout mMainView;

    AudioTrackPlayer mPlayer = new AudioTrackPlayer();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainView = new LinearLayout(this);
        mMainView.setOrientation(LinearLayout.VERTICAL);
        setContentView(mMainView);
        // AAudio
        {
            TextView text = new TextView(this);
            text.setText("AAudio");
            mMainView.addView(text);
        }
        {
            Button button = new Button(this);
            button.setText("start AAudio");
            button.setOnClickListener(v -> { startAAudioPlayback(); });
            mMainView.addView(button);
        }
        {
            Button button = new Button(this);
            button.setText("stop AAudio");
            button.setOnClickListener(v -> { stopAAudioPlayback(); });
            mMainView.addView(button);
        }
        // AudioTrack
        {
            TextView text = new TextView(this);
            text.setText("AudioTrack");
            mMainView.addView(text);
        }
        {
            Button button = new Button(this);
            button.setText("start AudioTrack");
            button.setOnClickListener(v -> { mPlayer.start(); });
            mMainView.addView(button);
        }
        {
            Button button = new Button(this);
            button.setText("stop AudioTrack");
            button.setOnClickListener(v -> { mPlayer.stop(); });
            mMainView.addView(button);
        }
    }
}
