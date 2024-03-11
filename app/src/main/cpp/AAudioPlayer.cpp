#include "AAudioPlayer.h"
#include "AAudioTesting.h"

#include <android/log.h>

#define printLog(format, ...) __android_log_print(ANDROID_LOG_INFO, "audiotest", format, ##__VA_ARGS__)

#define SAMPLE_RATE 48000
#define NUM_CHANNELS 2
#define AUDIO_FORMAT AAUDIO_FORMAT_PCM_I16

#define SINUS_SCALE 2000
#define AAUDIO_NANOS_PER_MILLISECOND 1000000

AAudioPlayer::AAudioPlayer(int player_id, bool exclusive, bool lowlatency, int usage)
{
    aaudio_result_t result;

    id = player_id;
    aaudio_sharing_mode_t sharing = exclusive ? AAUDIO_SHARING_MODE_EXCLUSIVE : AAUDIO_SHARING_MODE_SHARED;
    aaudio_performance_mode_t performance = lowlatency ? AAUDIO_PERFORMANCE_MODE_LOW_LATENCY : AAUDIO_PERFORMANCE_MODE_NONE;

    if (stream)
    {
        printLog("AAudio stream already opened!");
        return;
    }

    AAudioStreamBuilder *builder;
    result = AAudio_createStreamBuilder(&builder);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio create failed: result=%d", result);
        return;
    }

    // Should select an output device. HDMI is default
    AAudioStreamBuilder_setDeviceId(builder, 0);
    AAudioStreamBuilder_setSharingMode(builder, sharing);
    AAudioStreamBuilder_setPerformanceMode(builder, performance);
    AAudioStreamBuilder_setUsage(builder, usage);
    AAudioStreamBuilder_setSampleRate(builder, SAMPLE_RATE);
    AAudioStreamBuilder_setChannelCount(builder, NUM_CHANNELS);
    AAudioStreamBuilder_setFormat(builder, AUDIO_FORMAT);
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    result = AAudioStreamBuilder_openStream(builder, &(stream));
    AAudioStreamBuilder_delete(builder);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio open stream failed: result=%d", result);
        return;
    }

    // Stream created
    is_mmap = AAudioStream_isMMapUsed(stream);
    printLog("AAudio stream created: player_id=%d sharing=%d performance=%d usage=%d sample_rate=%d num_channels=%d format=%d mmap=%d",
             player_id,
             sharing,
             performance,
             usage,
             SAMPLE_RATE,
             NUM_CHANNELS,
             AUDIO_FORMAT,
             is_mmap);
}

AAudioPlayer::~AAudioPlayer()
{
    aaudio_result_t result;

    if (running)
    {
        stop();
    }

    result = AAudioStream_close(stream);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio close stream failed: result=%d", result);
        return;
    }
}

void AAudioPlayer::playbackThreadFunc()
{
    printLog("AAudio playback thread started: player_id=%d", id);
    const int size = 1024;
    unsigned char buffer[size];
    while (running)
    {
        for (int i = 0; i < size; i++)
        {
            double x = (double)i / (double)size * 2 * 3.1415 * pitch;
            double y = sin(x);
            buffer[i] = y * 255;
        }
        aaudio_result_t result = AAudioStream_write(stream, buffer, size / (NUM_CHANNELS * 2), 0);
//        usleep(100);
    }
    printLog("AAudio playback thread stopped: player_id=%d", id);
}

void AAudioPlayer::start()
{
    aaudio_result_t result;

    if (!stream)
    {
        printLog("AAudio stream not opened!");
        return;
    }

    aaudio_stream_state_t inputState = AAUDIO_STREAM_STATE_STARTING;
    aaudio_stream_state_t nextState = AAUDIO_STREAM_STATE_UNINITIALIZED;
    int64_t timeoutNanos = 100 * AAUDIO_NANOS_PER_MILLISECOND;
    result = AAudioStream_requestStart(stream);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio request start failed: result=%d", result);
        return;
    }
    result = AAudioStream_waitForStateChange(stream, inputState, &nextState, timeoutNanos);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio wait for start failed: result=%d", result);
        return;
    }

    // start playback thread
    running = true;
    pitch = SINUS_SCALE * (id + 1);
    mPlaybackThread = std::thread(&AAudioPlayer::playbackThreadFunc, this);
    printLog("start comp[leted");
}

void AAudioPlayer::stop()
{
    aaudio_result_t result;

    if (!stream)
    {
        printLog("AAudio stream not opened!");
        return;
    }

    running = false;
    mPlaybackThread.join();

    aaudio_stream_state_t inputState = AAUDIO_STREAM_STATE_STOPPING;
    aaudio_stream_state_t nextState = AAUDIO_STREAM_STATE_UNINITIALIZED;
    int64_t timeoutNanos = 100 * AAUDIO_NANOS_PER_MILLISECOND;

    result = AAudioStream_requestStop(stream);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio request stop failed: result=%d", result);
        return;
    }

    result = AAudioStream_waitForStateChange(stream, inputState, &nextState, timeoutNanos);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio wait for stop failed: result=%d", result);
        return;
    }
}
