#pragma once

#include <thread>

#include <aaudio/AAudio.h>

class AAudioPlayer
{
public:
    AAudioPlayer(int player_id, bool exclusive, bool lowlatency, int usage);
    virtual ~AAudioPlayer();

    void start();
    void stop();

    inline bool isMMap() const { return is_mmap; }

private:
    void playbackThreadFunc();

private:
    int id;
    AAudioStream *stream = NULL;
    std::thread mPlaybackThread;
    bool running = false;
    int pitch = 2000;
    bool is_mmap = false;
};