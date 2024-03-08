#pragma once

#include <thread>

#include <aaudio/AAudio.h>

class AAudioPlayer
{
public:
    AAudioPlayer(int player_id);
    virtual ~AAudioPlayer();

    void start();
    void stop();

private:
    void playbackThreadFunc();

private:
    int id;
    AAudioStream *stream = NULL;
    std::thread mPlaybackThread;
    bool running = false;
    int pitch = 2000;
};