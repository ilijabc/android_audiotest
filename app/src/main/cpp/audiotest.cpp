#include <jni.h>

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("audiotest");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("audiotest")
//      }
//    }

#pragma once

#include <android/log.h>
#include <aaudio/AAudio.h>
#include "AAudioTesting.h"
#include <pthread.h>
#include <unistd.h>
#include <math.h>

//#define printLog(format, ...) __android_log_print(ANDROID_LOG_INFO, "audiotest", "[%s:%d]" format, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define printLog(format, ...) __android_log_print(ANDROID_LOG_INFO, "audiotest", format, ##__VA_ARGS__)

// Stream definition
#define SHARING_MODE AAUDIO_SHARING_MODE_EXCLUSIVE
#define PERFORMANCE_MODE AAUDIO_PERFORMANCE_MODE_LOW_LATENCY
#define AUDIO_USAGE AAUDIO_USAGE_GAME
#define SAMPLE_RATE 48000
#define NUM_CHANNELS 2
#define AUDIO_FORMAT AAUDIO_FORMAT_PCM_I16

// Player config
#define SINUS_SCALE 2000
#define AAUDIO_NANOS_PER_MILLISECOND 1000000
#define MAX_PLAYERS 20

struct Player {
    int id;
    AAudioStream *stream = NULL;
    pthread_t thread;
    int running = 0;
    int pitch = SINUS_SCALE;
};

static Player players[MAX_PLAYERS];

static void *playbackThread(void *arg)
{
    Player *player = (Player *)arg;

    printLog("AAudio playback thread started: player_id=%d", player->id);
    const int size = 1024;
    unsigned char buffer[size];
    while (player->running)
    {
        for (int i = 0; i < size; i++)
        {
            double x = (double)i / (double)size * 2 * 3.1415 * player->pitch;
            double y = sin(x);
            buffer[i] = y * 255;
        }
        aaudio_result_t result = AAudioStream_write(player->stream, buffer, size / (NUM_CHANNELS * 2), 0);
        usleep(100);
    }
    printLog("AAudio playback thread stopped: player_id=%d", player->id);
    return NULL;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rtrk_audiotest_MainActivity_startAAudioPlayback(JNIEnv *env, jobject thiz, int player_id) {
    aaudio_result_t result;

    Player *player = NULL;
    player = &(players[player_id]);
    if (!player)
    {
        printLog("Player not found!");
        return;
    }

    player->id = player_id;

    if (player->stream)
    {
        printLog("AAudio stream already opened!");
        return;
    }

    // init
    {
        AAudioStreamBuilder *builder;
        result = AAudio_createStreamBuilder(&builder);
        if (result != AAUDIO_OK)
        {
            printLog("AAudio create failed: result=%d", result);
            return;
        }
        // Should select an output device. HDMI is default
        AAudioStreamBuilder_setDeviceId(builder, 0);
        AAudioStreamBuilder_setSharingMode(builder, SHARING_MODE);
        AAudioStreamBuilder_setPerformanceMode(builder, PERFORMANCE_MODE);
        AAudioStreamBuilder_setUsage(builder, AUDIO_USAGE);
        AAudioStreamBuilder_setSampleRate(builder, SAMPLE_RATE);
        AAudioStreamBuilder_setChannelCount(builder, NUM_CHANNELS);
        AAudioStreamBuilder_setFormat(builder, AUDIO_FORMAT);
        AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
        result = AAudioStreamBuilder_openStream(builder, &(player->stream));
        AAudioStreamBuilder_delete(builder);
        if (result != AAUDIO_OK)
        {
            printLog("AAudio open stream failed: result=%d", result);
            return;
        }
        // Stream created
        bool mmap = AAudioStream_isMMapUsed(player->stream);
        printLog("AAudio stream created: player_id=%d sharing=%d performance=%d usage=%d sample_rate=%d num_channels=%d format=%d mmap=%d",
                 player_id,
                 SHARING_MODE,
                 PERFORMANCE_MODE,
                 AUDIO_USAGE,
                 SAMPLE_RATE,
                 NUM_CHANNELS,
                 AUDIO_FORMAT,
                 mmap);
    }

    // start
    if (player->stream)
    {
        aaudio_stream_state_t inputState = AAUDIO_STREAM_STATE_STARTING;
        aaudio_stream_state_t nextState = AAUDIO_STREAM_STATE_UNINITIALIZED;
        int64_t timeoutNanos = 100 * AAUDIO_NANOS_PER_MILLISECOND;
        result = AAudioStream_requestStart(player->stream);
        if (result != AAUDIO_OK)
        {
            printLog("AAudio request start failed: result=%d", result);
            return;
        }
        result = AAudioStream_waitForStateChange(player->stream, inputState, &nextState, timeoutNanos);
        if (result != AAUDIO_OK)
        {
            printLog("AAudio wait for start failed: result=%d", result);
            return;
        }
    }

    // start playback thread
    {
        player->running = 1;
        player->pitch = SINUS_SCALE * (player_id + 1);
        pthread_create(&(player->thread), NULL, playbackThread, player);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rtrk_audiotest_MainActivity_stopAAudioPlayback(JNIEnv *env, jobject thiz, int player_id) {
    aaudio_result_t result;

    Player *player = NULL;
    player = &(players[player_id]);

    if (!player)
    {
        printLog("Player not found!");
        return;
    }

    if (!player->stream)
    {
        printLog("AAudio stream not opened!");
        return;
    }

    player->running = 0;
    pthread_join( player->thread, NULL);

    aaudio_stream_state_t inputState = AAUDIO_STREAM_STATE_STOPPING;
    aaudio_stream_state_t nextState = AAUDIO_STREAM_STATE_UNINITIALIZED;
    int64_t timeoutNanos = 100 * AAUDIO_NANOS_PER_MILLISECOND;

    result = AAudioStream_requestStop(player->stream);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio request stop failed: result=%d", result);
        return;
    }

    result = AAudioStream_waitForStateChange(player->stream, inputState, &nextState, timeoutNanos);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio wait for stop failed: result=%d", result);
        return;
    }

    result = AAudioStream_close(player->stream);
    if (result != AAUDIO_OK)
    {
        printLog("AAudio close stream failed: result=%d", result);
        return;
    }

    player->stream = NULL;
    printLog("AAudio stream destroyed: player_id=%d", player_id);
}