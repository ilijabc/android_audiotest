package com.rtrk.audiotest;

public class AAudioPlayer implements IPlayer {
    static {
        System.loadLibrary("audiotest");
    }

    private native int createAAudioPlayer();
    private native void destroyAAudioPlayer(int id);
    private native void startAAudioPlayer(int id);
    private native void stopAAudioPlayer(int id);

    static public native void releaseAllPlayers();

    private int player_id = -1;

    public AAudioPlayer() {
        player_id = createAAudioPlayer();
    }
    @Override
    public void start() {
        startAAudioPlayer(player_id);
    }

    @Override
    public void stop() {
        stopAAudioPlayer(player_id);
    }

    @Override
    public void release() {
        destroyAAudioPlayer(player_id);
        player_id = -1;
    }
}
