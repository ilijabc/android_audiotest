package com.rtrk.audiotest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("audiotest");
    }

    native private void startAAudioPlayback(int player_id);
    native private void stopAAudioPlayback(int player_id);

    LinearLayout mMainView;

    AudioTrackPlayer mPlayer = new AudioTrackPlayer();

    final int max_players = 20;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);

        mMainView = new LinearLayout(this);
        mMainView.setOrientation(LinearLayout.VERTICAL);

        scroll.addView(mMainView);
        setContentView(scroll);

        // AAudio
        for (int i = 0; i < max_players; i++)
        {
            TextView text = new TextView(this);
            text.setText("AAudio " + i);
            mMainView.addView(text);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            mMainView.addView(layout);
            final int player_id = i;
            Button buttonStart = new Button(this);
            buttonStart.setText("start AAudio " + i);
            buttonStart.setOnClickListener(v -> { startAAudioPlayback(player_id); });
            layout.addView(buttonStart);
            Button buttonStop = new Button(this);
            buttonStop.setText("stop AAudio " + i);
            buttonStop.setOnClickListener(v -> { stopAAudioPlayback(player_id); });
            layout.addView(buttonStop);
        }


        // AudioTrack
        {
            TextView text = new TextView(this);
            text.setText("AudioTrack");
            mMainView.addView(text);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            mMainView.addView(layout);
            Button buttonStart = new Button(this);
            buttonStart.setText("start AudioTrack");
            buttonStart.setOnClickListener(v -> { mPlayer.start(); });
            layout.addView(buttonStart);
            Button buttonStop = new Button(this);
            buttonStop.setText("stop AudioTrack");
            buttonStop.setOnClickListener(v -> { mPlayer.stop(); });
            layout.addView(buttonStop);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        for (int i = 0; i < max_players; i++) {
            stopAAudioPlayback(i);
        }
        mPlayer.stop();
    }
}
