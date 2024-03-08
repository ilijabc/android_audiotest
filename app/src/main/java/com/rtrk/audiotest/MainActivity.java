package com.rtrk.audiotest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MainActivity extends Activity {
    enum PlayerType {
        AAudioPlayerType,
        AudioTrackPlayerType
    }

    class PlayerItemView extends Button {
        private final IPlayer player;
        private final PlayerType type;
        private boolean isPlaying = true;

        private void updateText() {
            if (isPlaying) {
                setText("Stop " + type);
            } else {
                setText("Play " + type);
            }
        }

        public PlayerItemView(Context context, IPlayer player, PlayerType type, boolean isPlaying) {
            super(context);
            this.player = player;
            this.type = type;
            this.isPlaying = isPlaying;
            updateText();

            setOnClickListener(v -> {
                this.isPlaying = !this.isPlaying;
                if (this.isPlaying) {
                    player.start();
                } else {
                    player.stop();
                }
                updateText();
            });
        }
    }

    LinearLayout mMainView;
    LinearLayout mListView;

    ArrayList<IPlayer> mPlayerList = new ArrayList<>();

    private IPlayer addPlayer(PlayerType type) {
        IPlayer player = null;
        if (type == PlayerType.AAudioPlayerType) {
            player = new AAudioPlayer();
        } else {
            player = new AudioTrackPlayer();
        }
        player.start();
        mPlayerList.add(player);
        {
            PlayerItemView item = new PlayerItemView(this, player, type, true);
            mListView.addView(item);
        }
        return player;
    }

    void removeAllPlayers() {
        for (IPlayer player : mPlayerList) {
            player.release();
        }
        mPlayerList.clear();
        mListView.removeAllViews();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainView = new LinearLayout(this);
        mMainView.setOrientation(LinearLayout.VERTICAL);
        setContentView(mMainView);

        // Main menu
        {
            LinearLayout menu = new LinearLayout(this);
            menu.setOrientation(LinearLayout.HORIZONTAL);
            mMainView.addView(menu);
            TextView text = new TextView(this);
            text.setText("Add track of type: ");
            menu.addView(text);
            // AAudio button
            Button buttonAddAAudio = new Button(this);
            buttonAddAAudio.setText("Add AAudio player");
            buttonAddAAudio.setOnClickListener(v -> { addPlayer(PlayerType.AAudioPlayerType); });
            menu.addView(buttonAddAAudio);
            // AudioTrack button
            Button buttonAddAudioTrack = new Button(this);
            buttonAddAudioTrack.setText("Add AudioTrack player");
            buttonAddAudioTrack.setOnClickListener(v -> { addPlayer(PlayerType.AudioTrackPlayerType); });
            menu.addView(buttonAddAudioTrack);
            // Remove all
            Button buttonRemoveAll = new Button(this);
            buttonRemoveAll.setText("Remove all players");
            buttonRemoveAll.setOnClickListener(v -> { removeAllPlayers(); });
            menu.addView(buttonRemoveAll);
        }

        // Player list
        ScrollView scroll = new ScrollView(this);
        mMainView.addView(scroll);
        mListView = new LinearLayout(this);
        mListView.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(mListView);

    }

    @Override
    protected void onStop() {
        super.onStop();
        removeAllPlayers();
    }
}
