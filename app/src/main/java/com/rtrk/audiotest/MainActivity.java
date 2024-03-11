package com.rtrk.audiotest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MainActivity extends Activity {
    enum PlayerType {
        AAudioPlayerType,
        AudioTrackPlayerType
    }

    ArrayList<Pair<String, Integer>> mUsageList = new ArrayList<Pair<String, Integer>>() {{
        add(new Pair("USAGE_MEDIA", 1));
        add(new Pair("USAGE_VOICE_COMMUNICATION", 2));
        add(new Pair("USAGE_VOICE_COMMUNICATION_SIGNALLING", 3));
        add(new Pair("USAGE_ALARM", 4));
        add(new Pair("USAGE_NOTIFICATION", 5));
        add(new Pair("USAGE_NOTIFICATION_RINGTONE", 6));
        add(new Pair("USAGE_NOTIFICATION_EVENT", 10));
        add(new Pair("USAGE_ASSISTANCE_ACCESSIBILITY", 11));
        add(new Pair("USAGE_ASSISTANCE_NAVIGATION_GUIDANCE", 12));
        add(new Pair("USAGE_ASSISTANCE_SONIFICATION", 13));
        add(new Pair("USAGE_GAME", 14));
        add(new Pair("USAGE_ASSISTANT", 16));
    }};

    ArrayList<Pair<String, Integer>> mContentList = new ArrayList<Pair<String, Integer>>() {{
        add(new Pair("CONTENT_TYPE_UNKNOWN", 0));
        add(new Pair("CONTENT_TYPE_SPEECH", 1));
        add(new Pair("CONTENT_TYPE_MUSIC", 2));
        add(new Pair("CONTENT_TYPE_MOVIE", 3));
        add(new Pair("CONTENT_TYPE_SONIFICATION", 4));
    }};

    class PlayerItemView extends LinearLayout {
        private final IPlayer player;
        private final PlayerType type;
        private boolean isPlaying = true;

        private Button button;

        private void updateText() {
            if (isPlaying) {
                button.setText("Stop");
            } else {
                button.setText("Play");
            }
        }

        public PlayerItemView(Context context, IPlayer player, PlayerType type, boolean exclusive, boolean low_latency, boolean isPlaying, String usage, String content) {
            super(context);
            this.player = player;
            this.type = type;
            this.isPlaying = isPlaying;

            button = new Button(context);
            addView(button);

            TextView infoText = new TextView(context);
            if (type == PlayerType.AAudioPlayerType) {
                infoText.setText("player=" + type + " exclusive=" + exclusive + " low_latency=" + low_latency + " usage=" + usage + " mmap=" + player.isMMap());
            } else {
                infoText.setText("player=" + type + " usage=" + usage + " content=" + content);
            }
            addView(infoText);

            setOrientation(HORIZONTAL);
            updateText();

            button.setOnClickListener(v -> {
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
    ScrollView mScrollView;

    ArrayList<IPlayer> mPlayerList = new ArrayList<>();

    private IPlayer addPlayer(PlayerType type, boolean exclusive, boolean low_latency, boolean autoplay, String usage, String content) {
        int usageId = 0;
        for (Pair<String, Integer> p : mUsageList) {
            if (p.first.equals(usage)) {
                usageId = p.second;
                break;
            }
        }

        int contentId = 0;
        for (Pair<String, Integer> p : mContentList) {
            if (p.first.equals(usage)) {
                contentId = p.second;
                break;
            }
        }

        IPlayer player = null;
        if (type == PlayerType.AAudioPlayerType) {
            player = new AAudioPlayer(exclusive, low_latency, usageId);
        } else {
            player = new AudioTrackPlayer(usageId, contentId);
        }
        if (autoplay) {
            player.start();
        }
        mPlayerList.add(player);
        {
            PlayerItemView item = new PlayerItemView(this, player, type, exclusive, low_latency, autoplay, usage, content);
            mListView.addView(item);
            mScrollView.post(() -> { mScrollView.scrollTo(0, mScrollView.getBottom()); });
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

        ArrayAdapter<String> usageList = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        for (Pair<String, Integer> p : mUsageList) {
            usageList.add(p.first);
        }

        ArrayAdapter<String> contentList = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        for (Pair<String, Integer> p : mContentList) {
            contentList.add(p.first);
        }

        // Main menu
        {
            // first menu
            LinearLayout menu1 = new LinearLayout(this);
            menu1.setOrientation(LinearLayout.HORIZONTAL);
            mMainView.addView(menu1);
            TextView text = new TextView(this);
            text.setText("Player config: ");
            menu1.addView(text);
            CheckBox checkExclusive = new CheckBox(this);
            checkExclusive.setText("Exclusive (AAudio)");
            checkExclusive.setChecked(true);
            menu1.addView(checkExclusive);
            CheckBox checkLowLatency = new CheckBox(this);
            checkLowLatency.setText("Low Latency (AAudio)");
            checkLowLatency.setChecked(true);
            menu1.addView(checkLowLatency);
            CheckBox checkAutoPlay = new CheckBox(this);
            checkAutoPlay.setText("Auto Play");
            checkAutoPlay.setChecked(true);
            menu1.addView(checkAutoPlay);
            // second menu
            LinearLayout menu2 = new LinearLayout(this);
            menu2.setOrientation(LinearLayout.HORIZONTAL);
            mMainView.addView(menu2);
            Spinner spinnerUsage = new Spinner(this);
            spinnerUsage.setAdapter(usageList);
            spinnerUsage.setSelection(10); // set USAGE_GAME as default
            menu2.addView(spinnerUsage);
            Spinner spinnerContent = new Spinner(this);
            spinnerContent.setAdapter(contentList);
            menu2.addView(spinnerContent);
            TextView textContent = new TextView(this);
            textContent.setText("(AudioTrack)");
            menu2.addView(textContent);
            // third menu
            LinearLayout menu3 = new LinearLayout(this);
            menu3.setOrientation(LinearLayout.HORIZONTAL);
            mMainView.addView(menu3);
            // AAudio button
            Button buttonAddAAudio = new Button(this);
            buttonAddAAudio.setText("Add AAudio player");
            buttonAddAAudio.setOnClickListener(v -> {
                boolean exclusive = checkExclusive.isChecked();
                boolean low_latency = checkLowLatency.isChecked();
                boolean autoplay = checkAutoPlay.isChecked();
                String usage = spinnerUsage.getSelectedItem().toString();
                String content = spinnerContent.getSelectedItem().toString();
                addPlayer(PlayerType.AAudioPlayerType, exclusive, low_latency, autoplay, usage, content);
            });
            menu3.addView(buttonAddAAudio);
            // AudioTrack button
            Button buttonAddAudioTrack = new Button(this);
            buttonAddAudioTrack.setText("Add AudioTrack player");
            buttonAddAudioTrack.setOnClickListener(v -> {
                boolean exclusive = checkExclusive.isChecked();
                boolean low_latency = checkLowLatency.isChecked();
                boolean autoplay = checkAutoPlay.isChecked();
                String usage = spinnerUsage.getSelectedItem().toString();
                String content = spinnerContent.getSelectedItem().toString();
                addPlayer(PlayerType.AudioTrackPlayerType, exclusive, low_latency, autoplay, usage, content);
            });
            menu3.addView(buttonAddAudioTrack);
            // Remove all
            Button buttonRemoveAll = new Button(this);
            buttonRemoveAll.setText("Remove all players");
            buttonRemoveAll.setOnClickListener(v -> { removeAllPlayers(); });
            menu3.addView(buttonRemoveAll);
        }

        // Player list
        mScrollView = new ScrollView(this);
        mMainView.addView(mScrollView);
        mListView = new LinearLayout(this);
        mListView.setOrientation(LinearLayout.VERTICAL);
        mScrollView.addView(mListView);
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeAllPlayers();
    }
}
