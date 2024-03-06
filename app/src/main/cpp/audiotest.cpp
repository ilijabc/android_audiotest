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
#include <pthread.h>
#include <unistd.h>
#include <math.h>

#define printLog(format, ...) __android_log_print(ANDROID_LOG_INFO, "zzz", "[%s:%d]" format, __FUNCTION__, __LINE__, ##__VA_ARGS__)

#define AAUDIO_NANOS_PER_MILLISECOND 1000000
#define NUM_CHANNELS 2
#define SAMPLE_RATE 44100
#define SINUS_SCALE 2000

static AAudioStream *mStream = NULL;
static pthread_t mThread;
static int running = 0;

static void *playbackThread(void *args)
{
    printLog("playbackThread BEGIN");
    const int size = 1024;
    unsigned char buffer[size];
    while (running)
    {
        for (int i = 0; i < size; i++)
        {
            double x = (double)i / (double)size * 2 * 3.1415 * SINUS_SCALE;
            double y = sin(x);
            buffer[i] = y * 255;
        }
        aaudio_result_t result = AAudioStream_write(mStream, buffer, size / (NUM_CHANNELS * 2), 0);
//        usleep(100);
    }
    printLog("playbackThread END");
    return NULL;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rtrk_audiotest_MainActivity_startAAudioPlayback(JNIEnv *env, jobject thiz) {
    aaudio_result_t result;

    if (mStream)
    {
        printLog("AAudio stream already opened!");
        return;
    }

    // init
    {
        AAudioStreamBuilder *builder;
        result = AAudio_createStreamBuilder(&builder);
        printLog("AAudio create builder: result=%d", result);
        // Should select an output device. HDMI is default
        AAudioStreamBuilder_setDeviceId(builder, 0);
        AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
        AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);
        AAudioStreamBuilder_setSampleRate(builder, SAMPLE_RATE);
        AAudioStreamBuilder_setChannelCount(builder, NUM_CHANNELS);
        AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
        // AAudioStreamBuilder_setSessionId(builder, AAUDIO_SESSION_ID_ALLOCATE);
        result = AAudioStreamBuilder_openStream(builder, &mStream);
        AAudioStreamBuilder_delete(builder);
        printLog("AAudio open stream: result=%d", result);
    }

    // start
    if (mStream)
    {
        aaudio_stream_state_t inputState = AAUDIO_STREAM_STATE_STARTING;
        aaudio_stream_state_t nextState = AAUDIO_STREAM_STATE_UNINITIALIZED;
        int64_t timeoutNanos = 100 * AAUDIO_NANOS_PER_MILLISECOND;
        result = AAudioStream_requestStart(mStream);
        printLog("AAudio request start: result=%d", result);
        if (result != AAUDIO_OK)
        {
            return;
        }
        result = AAudioStream_waitForStateChange(mStream, inputState, &nextState, timeoutNanos);
        printLog("AAudio wait for start: result=%d", result);
        if (result != AAUDIO_OK)
        {
            return;
        }
    }

    // start playback thread
    {
        running = 1;
        int r = pthread_create(&mThread, NULL, playbackThread, NULL);
        printLog("Create playbackThread: r=%d", r);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rtrk_audiotest_MainActivity_stopAAudioPlayback(JNIEnv *env, jobject thiz) {
    aaudio_result_t result;

    if (!mStream)
    {
        printLog("AAudio stream not opened!");
        return;
    }

    running = 0;
    pthread_join( mThread, NULL);

    aaudio_stream_state_t inputState = AAUDIO_STREAM_STATE_STOPPING;
    aaudio_stream_state_t nextState = AAUDIO_STREAM_STATE_UNINITIALIZED;
    int64_t timeoutNanos = 100 * AAUDIO_NANOS_PER_MILLISECOND;
    result = AAudioStream_requestStop(mStream);
    printLog("AAudio request stop: result=%d", result);
    if (result != AAUDIO_OK)
    {
        return;
    }
    result = AAudioStream_waitForStateChange(mStream, inputState, &nextState, timeoutNanos);
    printLog("AAudio wait for stop: result=%d", result);
    if (result != AAUDIO_OK)
    {
        return;
    }

    result = AAudioStream_close(mStream);
    printLog("AAudio close stream: result=%d", result);

    mStream = NULL;
}