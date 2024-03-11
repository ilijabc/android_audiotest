#include <jni.h>

#include <map>

#include "AAudioPlayer.h"

//#define printLog(format, ...) __android_log_print(ANDROID_LOG_INFO, "audiotest", "[%s:%d]" format, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define printLog(format, ...) __android_log_print(ANDROID_LOG_INFO, "audiotest", format, ##__VA_ARGS__)

static int id_counter = 0;

std::map<int, AAudioPlayer*> player_list;

extern "C"
JNIEXPORT int JNICALL
Java_com_rtrk_audiotest_AAudioPlayer_createAAudioPlayer(JNIEnv *env, jobject thiz, jboolean exclusive, jboolean lowlatency, jint usage) {
    id_counter++;
    AAudioPlayer *player = new AAudioPlayer(id_counter, exclusive, lowlatency, usage);
    player_list[id_counter] = player;
    return id_counter;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rtrk_audiotest_AAudioPlayer_destroyAAudioPlayer(JNIEnv *env, jobject thiz, int player_id) {
    AAudioPlayer *player = player_list[player_id];
    delete player;
    player_list.erase(player_id);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rtrk_audiotest_AAudioPlayer_startAAudioPlayer(JNIEnv *env, jobject thiz, int player_id) {
    AAudioPlayer *player = player_list[player_id];
    player->start();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rtrk_audiotest_AAudioPlayer_stopAAudioPlayer(JNIEnv *env, jobject thiz, int player_id) {
    AAudioPlayer *player = player_list[player_id];
    player->stop();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rtrk_audiotest_AAudioPlayer_releaseAllPlayers(JNIEnv *env, jclass thiz) {
    for (auto player : player_list) {
        delete player.second;
    }
    player_list.clear();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_rtrk_audiotest_AAudioPlayer_isAAudioPlayerMMap(JNIEnv *env, jobject thiz, int player_id) {
    AAudioPlayer *player = player_list[player_id];
    return player->isMMap();
}
