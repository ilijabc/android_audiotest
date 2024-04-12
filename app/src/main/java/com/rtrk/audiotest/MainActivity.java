package com.rtrk.audiotest;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    final static private String ADD_PLAYER_ACTION = "com.rtrk.audiotest.ADD_PLAYER";
    final static private String REMOVE_ALL_PLAYERS_ACTION = "com.rtrk.audiotest.REMOVE_ALL_PLAYERS";

    enum PlayerType {
        AAudioPlayerType,
        AudioTrackPlayerType,
        AAudioRecorderType,
    }

    public static String usageToString(int usage) {
        for (Pair<String, Integer> pair : mUsageList) {
            if (pair.second == usage)
                return pair.first;
        }
        return "?";
    }

    static ArrayList<Pair<String, Integer>> mUsageList = new ArrayList<Pair<String, Integer>>() {{
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

    static ArrayList<Pair<String, Integer>> mContentList = new ArrayList<Pair<String, Integer>>() {{
        add(new Pair("CONTENT_TYPE_UNKNOWN", 0));
        add(new Pair("CONTENT_TYPE_SPEECH", 1));
        add(new Pair("CONTENT_TYPE_MUSIC", 2));
        add(new Pair("CONTENT_TYPE_MOVIE", 3));
        add(new Pair("CONTENT_TYPE_SONIFICATION", 4));
    }};

    class PlayerItemView extends LinearLayout {
        private final IPlayer player;

        private Button buttonPlayStop;
        private TextView infoText;

        private void updateText() {
            if (player.isPlaying()) {
                buttonPlayStop.setText("Stop");
            } else {
                buttonPlayStop.setText("Play");
            }
            infoText.setText(player.toString());
        }

        public PlayerItemView(Context context, IPlayer player) {
            super(context);
            this.player = player;

            buttonPlayStop = new Button(context);
            buttonPlayStop.setOnClickListener(v -> {
                if (player.isPlaying()) {
                    player.stop();
                } else {
                    player.start();
                }
                updateText();
            });
            addView(buttonPlayStop);

            infoText = new TextView(context);
            addView(infoText);

            setOrientation(HORIZONTAL);
            updateText();
        }
    }

    class ControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ADD_PLAYER_ACTION.equals(intent.getAction())) {
                PlayerType type = PlayerType.AAudioPlayerType;
                if (intent.hasExtra("type") && intent.getStringExtra("type").equals("audio_track"))
                    type = PlayerType.AudioTrackPlayerType;
                int sample_rate = intent.getIntExtra("sample_rate", 48000);
                int channels = intent.getIntExtra("channels", 2);
                boolean exclusive = intent.getBooleanExtra("exclusive", true);
                boolean low_latency = intent.getBooleanExtra("low_latency", true);
                boolean autoplay = intent.getBooleanExtra("autoplay", true);
                String usage = "USAGE_MEDIA";
                if (intent.hasExtra("usage"))
                    usage = intent.getStringExtra("usage");
                String content = "CONTENT_TYPE_UNKNOWN";
                if (intent.hasExtra("content"))
                    content = intent.getStringExtra("content");
                int device_id = intent.getIntExtra("device_id", 0);
                addPlayer(type, sample_rate, channels,
                        exclusive, low_latency, autoplay, usage, content, device_id);
            } else if (REMOVE_ALL_PLAYERS_ACTION.equals(intent.getAction())) {
                removeAllPlayers();
            }
        }
    }

    LinearLayout mMainView;
    LinearLayout mListView;
    ScrollView mScrollView;

    ArrayList<IPlayer> mPlayerList = new ArrayList<>();

    ControlReceiver mReceiver = new ControlReceiver();

    private IPlayer addPlayer(PlayerType type, int sample_rate, int channels,
                              boolean exclusive, boolean low_latency, boolean autoplay,
                              String usage, String content, int deviceId) {
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
        switch (type) {
            case AAudioPlayerType:
                player = new AAudioPlayer(true, sample_rate, channels, exclusive, low_latency, usageId, deviceId);
                break;
            case AudioTrackPlayerType:
                player = new AudioTrackPlayer(sample_rate, channels, usageId, contentId);
                break;
            case AAudioRecorderType:
                player = new AAudioPlayer(false, sample_rate, channels, exclusive, low_latency, usageId, deviceId);
                break;
        }
        if (autoplay) {
            player.start();
        }
        mPlayerList.add(player);
        {
            PlayerItemView item = new PlayerItemView(this, player);
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

    void updatePlayers() {
        for (int i = 0; i < mListView.getChildCount(); i++) {
            if (mListView.getChildAt(i) instanceof PlayerItemView) {
                PlayerItemView item = (PlayerItemView)mListView.getChildAt(i);
                item.updateText();
            }
        }
    }

    private CheckBox newCheckBox(ViewGroup parent, String text, boolean checked) {
        CheckBox check = new CheckBox(this);
        check.setText(text);
        check.setChecked(checked);
        parent.addView(check);
        return check;
    }

    private Spinner newSpinner(ViewGroup parent, SpinnerAdapter adapter, int selected, float weight) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
        if (weight > 0)
            spinner.setLayoutParams(new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        parent.addView(spinner);
        return spinner;
    }

    private Button newButton(ViewGroup parent, String text, View.OnClickListener onClick) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(onClick);
        parent.addView(button);
        return button;
    }

    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                Toast.makeText(getApplicationContext(), "Audio reording permission granted.", Toast.LENGTH_LONG);
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQUEST_AUDIO_PERMISSION_CODE);

        mMainView = new LinearLayout(this);
        mMainView.setOrientation(LinearLayout.VERTICAL);
        setContentView(mMainView);

        ArrayAdapter<String> sampleRateList = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        sampleRateList.add("8000");
        sampleRateList.add("11025");
        sampleRateList.add("12000");
        sampleRateList.add("16000");
        sampleRateList.add("22050");
        sampleRateList.add("24000");
        sampleRateList.add("32000");
        sampleRateList.add("44100");
        sampleRateList.add("48000");

        ArrayAdapter<String> channelsList = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        channelsList.add("Mono");
        channelsList.add("Stereo");

        ArrayAdapter<String> usageList = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        for (Pair<String, Integer> p : mUsageList) {
            usageList.add(p.first);
        }

        ArrayAdapter<String> contentList = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        for (Pair<String, Integer> p : mContentList) {
            contentList.add(p.first);
        }

        AudioDeviceInfo[] outputDevices = null;
        AudioDeviceInfo[] inputDevices = null;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        }
        ArrayAdapter<String> devicesList = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        devicesList.add("0: default audio device");
        if (outputDevices != null) {
            for (AudioDeviceInfo info : outputDevices) {
                String infoText = info.getId() + ": [OUTPUT] " + info.getAddress() + " (" + info.getProductName() + ")";
                Log.d("audiotest", "  " + infoText);
                devicesList.add(infoText);
            }
        }
        if (inputDevices != null) {
            for (AudioDeviceInfo info : inputDevices) {
                String infoText = info.getId() + ": [INPUT] " + info.getAddress() + " (" + info.getProductName() + ")";
                Log.d("audiotest", "  " + infoText);
                devicesList.add(infoText);
            }
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
            CheckBox checkExclusive = newCheckBox(menu1, "Exclusive (AAudio)", true);
            CheckBox checkLowLatency = newCheckBox(menu1, "Low Latency (AAudio)", true);
            CheckBox checkAutoPlay = newCheckBox(menu1, "Auto Play", false);
            Spinner spinnerSampleRate = newSpinner(menu1, sampleRateList, 8, -0.2f);
            Spinner spinnerChannels = newSpinner(menu1, channelsList, 1, -0.2f);
            // second menu
            LinearLayout menu2 = new LinearLayout(this);
            menu2.setOrientation(LinearLayout.HORIZONTAL);
            mMainView.addView(menu2);
            Spinner spinnerUsage = newSpinner(menu2, usageList, 10, 0.3f); // set USAGE_GAME as default
            Spinner spinnerContent = newSpinner(menu2, contentList, 0, 0.3f);
            Spinner spinnerDevice = newSpinner(menu2, devicesList, 0, 0.3f);
            // third menu
            LinearLayout menu3 = new LinearLayout(this);
            menu3.setOrientation(LinearLayout.HORIZONTAL);
            mMainView.addView(menu3);
            // AAudio button
            newButton(menu3, "Add AAudio player",
                v -> {
                    int sample_rate = Integer.parseInt(spinnerSampleRate.getSelectedItem().toString());
                    int channels = spinnerChannels.getSelectedItemPosition() + 1;
                    boolean exclusive = checkExclusive.isChecked();
                    boolean low_latency = checkLowLatency.isChecked();
                    boolean autoplay = checkAutoPlay.isChecked();
                    String usage = spinnerUsage.getSelectedItem().toString();
                    String content = spinnerContent.getSelectedItem().toString();
                    int deviceId = Integer.parseInt(spinnerDevice.getSelectedItem().toString().split(":")[0]);
                    Log.d("audiotest", "deviceId=" + deviceId);
                    addPlayer(PlayerType.AAudioPlayerType, sample_rate, channels,
                            exclusive, low_latency, autoplay, usage, content, deviceId);
                });
            // AudioTrack button
            newButton(menu3, "Add AudioTrack player",
                v -> {
                    int sample_rate = Integer.parseInt(spinnerSampleRate.getSelectedItem().toString());
                    int channels = spinnerChannels.getSelectedItemPosition() + 1;
                    boolean exclusive = checkExclusive.isChecked();
                    boolean low_latency = checkLowLatency.isChecked();
                    boolean autoplay = checkAutoPlay.isChecked();
                    String usage = spinnerUsage.getSelectedItem().toString();
                    String content = spinnerContent.getSelectedItem().toString();
                    int deviceId = Integer.parseInt(spinnerDevice.getSelectedItem().toString().split(":")[0]);
                    Log.d("audiotest", "deviceId=" + deviceId);
                    addPlayer(PlayerType.AudioTrackPlayerType, sample_rate, channels,
                            exclusive, low_latency, autoplay, usage, content, deviceId);
                });
            // AAudio recorder button
            newButton(menu3, "Add AAudio recorder",
                v -> {
                    int sample_rate = Integer.parseInt(spinnerSampleRate.getSelectedItem().toString());
                    int channels = spinnerChannels.getSelectedItemPosition() + 1;
                    boolean exclusive = checkExclusive.isChecked();
                    boolean low_latency = checkLowLatency.isChecked();
                    boolean autoplay = checkAutoPlay.isChecked();
                    String usage = spinnerUsage.getSelectedItem().toString();
                    String content = spinnerContent.getSelectedItem().toString();
                    int deviceId = Integer.parseInt(spinnerDevice.getSelectedItem().toString().split(":")[0]);
                    Log.d("audiotest", "deviceId=" + deviceId);
                    addPlayer(PlayerType.AAudioRecorderType, sample_rate, channels,
                            exclusive, low_latency, autoplay, usage, content, deviceId);
                });
            // Remove all
            newButton(menu3, "Remove all",
                v -> { removeAllPlayers(); });
            // Update
            newButton(menu3, "Update",
                v -> { updatePlayers(); });
        }

        // Player list
        mScrollView = new ScrollView(this);
        mMainView.addView(mScrollView);
        mListView = new LinearLayout(this);
        mListView.setOrientation(LinearLayout.VERTICAL);
        mScrollView.addView(mListView);

        // Broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ADD_PLAYER_ACTION);
        filter.addAction(REMOVE_ALL_PLAYERS_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("audiotest", "register broadcast receiver");
            registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeAllPlayers();
    }
}
