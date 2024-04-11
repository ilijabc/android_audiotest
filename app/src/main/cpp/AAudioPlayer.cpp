#include "AAudioPlayer.h"
#include "AAudioTesting.h"

#include <android/log.h>

#define printLog(format, ...) __android_log_print(ANDROID_LOG_INFO, "audiotest", format, ##__VA_ARGS__)

#define AUDIO_FORMAT AAUDIO_FORMAT_PCM_I16

#define SINUS_SCALE 2000
#define AAUDIO_NANOS_PER_MILLISECOND 1000000

AAudioPlayer::AAudioPlayer(int player_id,
                           bool is_output, int sample_rate, int channels,
                           bool exclusive, bool low_latency, int usage, int deviceId)
                           : id(player_id)
                           , is_output(is_output)
                           , sample_rate(sample_rate)
                           , channels(channels)
{
    aaudio_result_t result;

    aaudio_sharing_mode_t sharing = exclusive ? AAUDIO_SHARING_MODE_EXCLUSIVE : AAUDIO_SHARING_MODE_SHARED;
    aaudio_performance_mode_t performance = low_latency ? AAUDIO_PERFORMANCE_MODE_LOW_LATENCY : AAUDIO_PERFORMANCE_MODE_NONE;
    aaudio_direction_t direction = is_output ? AAUDIO_DIRECTION_OUTPUT : AAUDIO_DIRECTION_INPUT;

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
    AAudioStreamBuilder_setDeviceId(builder, deviceId);
    AAudioStreamBuilder_setSharingMode(builder, sharing);
    AAudioStreamBuilder_setPerformanceMode(builder, performance);
    AAudioStreamBuilder_setUsage(builder, usage);
    AAudioStreamBuilder_setSampleRate(builder, sample_rate);
    AAudioStreamBuilder_setChannelCount(builder, channels);
    AAudioStreamBuilder_setFormat(builder, AUDIO_FORMAT);
    AAudioStreamBuilder_setDirection(builder, direction);
    result = AAudioStreamBuilder_openStream(builder, &(stream));
    AAudioStreamBuilder_delete(builder);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio open stream failed: result=%d", result);
        return;
    }

    // Stream created
    is_mmap = AAudioStream_isMMapUsed(stream);
    printLog("AAudio stream created: player_id=%d device_id=%d sharing=%d performance=%d direction=%d usage=%d sample_rate=%d num_channels=%d format=%d mmap=%d",
             player_id,
             deviceId,
             sharing,
             performance,
             direction,
             usage,
             sample_rate,
             channels,
             AUDIO_FORMAT,
             is_mmap);
}

AAudioPlayer::~AAudioPlayer()
{
    aaudio_result_t result;

    stop();

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
            double x = (double)i / (double)size * 2 * 3.1415 * output_pitch;
            double y = sin(x);
            buffer[i] = y * 255;
        }
        aaudio_result_t result = AAudioStream_write(stream, buffer, size / (channels * 2), 0);
        if (result != AAUDIO_OK) {
            printLog("AAudioStream_write ERROR: %d", result);
        }
        // usleep(100);
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
    if (is_output) {
        running = true;
        output_pitch = SINUS_SCALE * (id + 1);
        mPlaybackThread = std::thread(&AAudioPlayer::playbackThreadFunc, this);
    }
    printLog("start completed");
}

void AAudioPlayer::stop()
{
    aaudio_result_t result;

    if (!stream)
    {
        printLog("AAudio stream not opened!");
        return;
    }

    if (running) {
        running = false;
        mPlaybackThread.join();
    }

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
