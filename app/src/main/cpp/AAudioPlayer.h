#pragma once

#include <thread>

#include <aaudio/AAudio.h>

class AAudioPlayer
{
public:
    AAudioPlayer(int player_id,
                 bool is_output, int sample_rate, int channels,
                 bool exclusive, bool lowlatency, int usage, int deviceId);
    virtual ~AAudioPlayer();

    void start();
    void stop();

    inline bool isMMap() const { return is_mmap; }

private:
    void playbackThreadFunc();

private:
    const int id;
    const bool is_output;
    const int sample_rate;
    const int channels;
    AAudioStream *stream = NULL;
    std::thread mPlaybackThread;
    bool running = false;
    int output_pitch = 2000;
    bool is_mmap = false;
};