package com.rtrk.audiotest;

public class AAudioPlayer implements IPlayer {
    static {
        System.loadLibrary("audiotest");
    }

    private native int createAAudioPlayer(boolean exclusive, boolean lowlatency, int usage);
    private native void destroyAAudioPlayer(int id);
    private native void startAAudioPlayer(int id);
    private native void stopAAudioPlayer(int id);
    private native boolean isAAudioPlayerMMap(int id);

    static public native void releaseAllPlayers();

    private int player_id = -1;
    private final boolean is_exclusive;
    private final boolean is_lowlatency;
    private boolean is_mmap = false;

    public AAudioPlayer(boolean exclusive, boolean lowlatency, int usage) {
        player_id = createAAudioPlayer(exclusive, lowlatency, usage);
        is_exclusive = exclusive;
        is_lowlatency = lowlatency;
        is_mmap = isAAudioPlayerMMap(player_id);
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

    @Override
    public boolean isExclusive() {
        return is_exclusive;
    }

    @Override
    public boolean isLowLatency() {
        return is_lowlatency;
    }

    @Override
    public boolean isMMap() {
        return is_mmap;
    }
}
