package com.rtrk.audiotest;

public interface IPlayer {
    void start();

    void stop();

    void release();

    boolean isMMap();

    boolean isPlaying();
}
