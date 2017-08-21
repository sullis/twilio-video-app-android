/*
 * Copyright (C) 2017 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "com_twilio_video_RemoteParticipant.h"
#include "logging.h"

#include "webrtc/sdk/android/src/jni/jni_helpers.h"
#include "webrtc/sdk/android/src/jni/classreferenceholder.h"

#include "video/video.h"

namespace twilio_video_jni {

void bindRemoteParticipantListenerProxy(JNIEnv *env,
                                        jobject j_remote_participant,
                                        jclass j_remote_participant_class,
                                        RemoteParticipantContext *remote_participant_context) {
    jfieldID j_remote_participant_listener_proxy_field = webrtc_jni::GetFieldID(env,
                                                                                j_remote_participant_class,
                                                                                "participantListenerProxy",
                                                                                "Lcom/twilio/video/RemoteParticipant$Listener;");
    jobject j_remote_participant_listener_proxy = webrtc_jni::GetObjectField(env,
                                                                             j_remote_participant,
                                                                             j_remote_participant_listener_proxy_field);

    remote_participant_context->android_participant_observer =
            std::make_shared<AndroidParticipantObserver>(env,
                                                         j_remote_participant,
                                                         j_remote_participant_listener_proxy,
                                                         remote_participant_context->remote_audio_track_map,
                                                         remote_participant_context->remote_video_track_map);
    remote_participant_context->remote_participant->setObserver(
            remote_participant_context->android_participant_observer);
}

jobject createJavaRemoteParticipant(JNIEnv *env,
                                    std::shared_ptr<twilio::video::RemoteParticipant> remote_participant,
                                    jclass j_remote_participant_class,
                                    jmethodID j_remote_participant_ctor_id,
                                    jclass j_array_list_class,
                                    jmethodID j_array_list_ctor_id,
                                    jmethodID j_array_list_add,
                                    jclass j_remote_audio_track_class,
                                    jmethodID j_remote_audio_track_ctor_id,
                                    jclass j_remote_video_track_class,
                                    jmethodID j_remote_video_track_ctor_id,
                                    jobject j_handler) {
    RemoteParticipantContext *remote_participant_context = new RemoteParticipantContext();
    remote_participant_context->remote_participant = remote_participant;
    jstring j_sid = webrtc_jni::JavaStringFromStdString(env, remote_participant->getSid());
    jstring j_identity =
            webrtc_jni::JavaStringFromStdString(env, remote_participant->getIdentity());
    jobject j_remote_audio_tracks = createRemoteParticipantAudioTracks(env,
                                                                       remote_participant_context,
                                                                       j_array_list_class,
                                                                       j_array_list_ctor_id,
                                                                       j_array_list_add,
                                                                       j_remote_audio_track_class,
                                                                       j_remote_audio_track_ctor_id);
    jobject j_remote_video_tracks = createRemoteParticipantVideoTracks(env,
                                                                       remote_participant_context,
                                                                       j_array_list_class,
                                                                       j_array_list_ctor_id,
                                                                       j_array_list_add,
                                                                       j_remote_video_track_class,
                                                                       j_remote_video_track_ctor_id);

    // Create participant
    jlong j_remote_participant_context = webrtc_jni::jlongFromPointer(remote_participant_context);
    jobject j_remote_participant =  env->NewObject(j_remote_participant_class,
                                                   j_remote_participant_ctor_id,
                                                   j_identity,
                                                   j_sid,
                                                   j_remote_audio_tracks,
                                                   j_remote_video_tracks,
                                                   j_handler,
                                                   j_remote_participant_context);

    // Bind the participant listener proxy with the native participant observer
    bindRemoteParticipantListenerProxy(env,
                                       j_remote_participant,
                                       j_remote_participant_class,
                                       remote_participant_context);

    return j_remote_participant;
}

jobject createRemoteParticipantAudioTracks(JNIEnv *env,
                                           RemoteParticipantContext *remote_participant_context,
                                           jclass j_array_list_class,
                                           jmethodID j_array_list_ctor_id,
                                           jmethodID j_array_list_add,
                                           jclass j_remote_audio_track_class,
                                           jmethodID j_remote_audio_track_ctor_id) {
    jobject j_remote_audio_tracks = env->NewObject(j_array_list_class, j_array_list_ctor_id);

    const std::vector<std::shared_ptr<twilio::media::RemoteAudioTrack>> remote_audio_tracks =
            remote_participant_context->remote_participant->getRemoteAudioTracks();

    // Add audio tracks to array list
    for (unsigned int i = 0; i < remote_audio_tracks.size(); i++) {
        std::shared_ptr<twilio::media::RemoteAudioTrack> remote_audio_track = remote_audio_tracks[i];
        jobject j_remote_audio_track =
                createJavaRemoteAudioTrack(env,
                                           remote_audio_track,
                                           j_remote_audio_track_class,
                                           j_remote_audio_track_ctor_id);

        /*
         * We create a global reference to the java audio track so we can map audio track events
         * to the original java instance.
         */
        remote_participant_context->remote_audio_track_map
                .insert(std::make_pair(remote_audio_track,
                                       webrtc_jni::NewGlobalRef(env, j_remote_audio_track)));
        env->CallBooleanMethod(j_remote_audio_tracks, j_array_list_add, j_remote_audio_track);
    }

    return j_remote_audio_tracks;
}

jobject createRemoteParticipantVideoTracks(JNIEnv *env,
                                           RemoteParticipantContext *remote_participant_context,
                                           jclass j_array_list_class,
                                           jmethodID j_array_list_ctor_id,
                                           jmethodID j_array_list_add,
                                           jclass j_remote_video_track_class,
                                           jmethodID j_remote_video_track_ctor_id) {
    jobject j_remote_video_tracks = env->NewObject(j_array_list_class, j_array_list_ctor_id);

    const std::vector<std::shared_ptr<twilio::media::RemoteVideoTrack>> remote_video_tracks =
            remote_participant_context->remote_participant->getRemoteVideoTracks();

    // Add video tracks to array list
    for (unsigned int i = 0; i < remote_video_tracks.size(); i++) {
        std::shared_ptr<twilio::media::RemoteVideoTrack> remote_video_track = remote_video_tracks[i];
        jobject j_remote_video_track =
                createJavaRemoteVideoTrack(env,
                                           remote_video_track,
                                           j_remote_video_track_class,
                                           j_remote_video_track_ctor_id);
        /*
         * We create a global reference to the java video track so we can map video track events
         * to the original java instance.
         */
        remote_participant_context->remote_video_track_map
                .insert(std::make_pair(remote_video_track,
                                       webrtc_jni::NewGlobalRef(env, j_remote_video_track)));
        env->CallBooleanMethod(j_remote_video_tracks, j_array_list_add, j_remote_video_track);
    }

    return j_remote_video_tracks;
}

jobject createJavaRemoteAudioTrack(JNIEnv *env,
                                   std::shared_ptr<twilio::media::RemoteAudioTrack> remote_audio_track,
                                   jclass j_remote_audio_track_class,
                                   jmethodID j_remote_audio_track_ctor_id) {
    jstring j_track_sid = webrtc_jni::JavaStringFromStdString(env, remote_audio_track->getSid());
    jboolean j_is_enabled = (jboolean) remote_audio_track->isEnabled();
    jboolean j_is_subscribed = (jboolean) remote_audio_track->isSubscribed();

    return env->NewObject(j_remote_audio_track_class,
                          j_remote_audio_track_ctor_id,
                          j_track_sid,
                          j_is_enabled,
                          j_is_subscribed);
}

jobject createJavaRemoteVideoTrack(JNIEnv *env,
                                   std::shared_ptr<twilio::media::RemoteVideoTrack> remote_video_track,
                                   jclass j_remote_video_track_class,
                                   jmethodID j_remote_video_track_ctor_id) {
    jstring j_track_sid = webrtc_jni::JavaStringFromStdString(env, remote_video_track->getSid());
    jboolean j_is_enabled = (jboolean) remote_video_track->isEnabled();
    jboolean j_is_subscribed = (jboolean) remote_video_track->isSubscribed();
    jobject j_remote_video_track = env->NewObject(j_remote_video_track_class,
                                                  j_remote_video_track_ctor_id,
                                                  j_track_sid,
                                                  j_is_enabled,
                                                  j_is_subscribed);
    CHECK_EXCEPTION(env) << "Failed to create RemoteVideoTrack";
    return j_remote_video_track;
}



JNIEXPORT jboolean JNICALL
Java_com_twilio_video_RemoteParticipant_nativeIsConnected(JNIEnv *env, jobject instance,
                                                          jlong j_participant_context) {
    std::string func_name = std::string(__FUNCTION__);
    VIDEO_ANDROID_LOG(twilio::video::LogModule::kPlatform,
                      twilio::video::LogLevel::kDebug,
                      "%s", func_name.c_str());
    RemoteParticipantContext *participant_context =
            reinterpret_cast<RemoteParticipantContext *>(j_participant_context);
    if (participant_context == nullptr || !participant_context->remote_participant) {
        VIDEO_ANDROID_LOG(twilio::video::LogModule::kPlatform,
                          twilio::video::LogLevel::kWarning,
                          "RemoteParticipant object no longer exist");
        return false;
    }

    return participant_context->remote_participant->isConnected();
}

JNIEXPORT void JNICALL
Java_com_twilio_video_RemoteParticipant_nativeRelease(JNIEnv *env,
                                                      jobject instance,
                                                      jlong j_participant_context) {
    RemoteParticipantContext *participant_context =
            reinterpret_cast<RemoteParticipantContext *>(j_participant_context);

    // Delete the participant observer
    participant_context->android_participant_observer->setObserverDeleted();
    participant_context->android_participant_observer = nullptr;

    // Delete all remaining global references to AudioTracks
    for (auto it = participant_context->remote_audio_track_map.begin() ;
         it != participant_context->remote_audio_track_map.end() ; it++) {
        webrtc_jni::DeleteGlobalRef(env, it->second);
    }
    participant_context->remote_audio_track_map.clear();

    // Delete all remaining global references to VideoTracks
    for (auto it = participant_context->remote_video_track_map.begin() ;
         it != participant_context->remote_video_track_map.end() ; it++) {
        webrtc_jni::DeleteGlobalRef(env, it->second);
    }
    participant_context->remote_video_track_map.clear();

    // Now that all participant resources are deleted we delete participant context
    delete participant_context;
}

}