/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
#include "TSCoreSDKTypes.h"
#include "TSCoreConstants.h"
#include "TSCoreSDK.h"
#include "TSCEndpoint.h"
#include "TSCEndpointObserver.h"
#include "TSCConfiguration.h"
#include "TSCLogger.h"
#include "AccessManager/AccessManager.h"
#include "webrtc/voice_engine/include/voe_base.h"
#include "webrtc/modules/video_capture/video_capture_internal.h"
#include "webrtc/modules/video_render/video_render_internal.h"
#include "talk/app/webrtc/java/jni/androidvideocapturer_jni.h"
#include "webrtc/modules/audio_device/android/audio_manager.h"
#include "webrtc/modules/audio_device/android/opensles_player.h"
#include "webrtc/modules/video_capture/android/device_info_android.h"

#include <string.h>
#include "com_twilio_signal_impl_TwilioRTCImpl.h"

#include "talk/app/webrtc/java/jni/jni_helpers.h"
#include "talk/app/webrtc/java/jni/classreferenceholder.h"

#define TAG  "TwilioSDK(native)"

using namespace webrtc_jni;
using namespace twiliosdk;


static TwilioCommon::AccessManager* getNativeAccessMgrFromJava(JNIEnv* jni, jobject j_accessMgr) {
  jclass j_accessManagerClass = GetObjectClass(jni, j_accessMgr);
  jmethodID getNativeHandleId = GetMethodID(jni, j_accessManagerClass, "getNativeHandle", "()J");

  jlong j_am = jni->CallLongMethod(j_accessMgr, getNativeHandleId);
  return reinterpret_cast<TwilioCommon::AccessManager*>(j_am);
}

/*
 * Class:     com_twilio_signal_impl_TwilioRTCImpl
 * Method:    initCore
 * Signature: (Landroid/content/Context;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_twilio_signal_impl_TwilioRTCImpl_initCore(JNIEnv *env, jobject obj, jobject context) {

	bool success = false;
	JavaVM * cachedJVM = NULL;

	env->GetJavaVM(&cachedJVM);

	// Perform webrtc_jni initialization to enable peerconnection_jni.cc JNI bindings.
	jint ret = webrtc_jni::InitGlobalJniVariables(cachedJVM);
	if(ret < 0) {
		TS_CORE_LOG_MODULE(kTSCoreLogModuleSignalSDK, kTSCoreLogLevelError, "TwilioSDK.InitGlobalJniVariables() failed");
		return success;
	} else {
		webrtc_jni::LoadGlobalClassReferenceHolder();
	}

	TSCSDK* tscSdk = TSCSDK::instance();

	webrtc::videocapturemodule::DeviceInfoAndroid::Initialize(env);
	webrtc::OpenSLESPlayer::SetAndroidAudioDeviceObjects(cachedJVM, context);

	success |= webrtc::SetCaptureAndroidVM(cachedJVM, context);
	success |= webrtc::SetRenderAndroidVM(cachedJVM);

	// Required to setup an external capturer
	success |= webrtc_jni::AndroidVideoCapturerJni::SetAndroidObjects(env, context);

	TS_CORE_LOG_MODULE(kTSCoreLogModuleSignalSDK, kTSCoreLogLevelDebug, "Calling DA Magic formula");
	success |= webrtc::VoiceEngine::SetAndroidObjects(cachedJVM, context);

	// TODO: check success and return appropriately 
	if (tscSdk != NULL && tscSdk->isInitialized()) {
		return JNI_TRUE;
	}

	return JNI_FALSE;
}


JNIEXPORT jlong JNICALL Java_com_twilio_signal_impl_TwilioRTCImpl_createEndpoint
  (JNIEnv *env, jobject obj, jobject j_accessMgr, jlong nativeEndpointObserver) {
	TS_CORE_LOG_MODULE(kTSCoreLogModuleSignalSDK, kTSCoreLogLevelDebug, "createEndpoint");

	TSCOptions options;

	if (!nativeEndpointObserver) {
		TS_CORE_LOG_MODULE(kTSCoreLogModuleSignalSDK, kTSCoreLogLevelError, "nativeEndpointObserver is null");
		return 0;
	}

	TSCEndpointObserverPtr *endpointObserver = reinterpret_cast<TSCEndpointObserverPtr *>(nativeEndpointObserver);
	TwilioCommon::AccessManager* accessManager = getNativeAccessMgrFromJava(env, j_accessMgr);

	if (accessManager == NULL) {
		TS_CORE_LOG_MODULE(kTSCoreLogModuleSignalSDK, kTSCoreLogLevelError, "AccessManager is null");
		return 0;
	}

	if (accessManager->getToken().empty()) {
		TS_CORE_LOG_MODULE(kTSCoreLogModuleSignalSDK, kTSCoreLogLevelError, "token is null");
		return 0;
	}

	TS_CORE_LOG_DEBUG("access token is:%s", accessManager->getToken().c_str());

	TSCEndpointPtr *endpoint = new TSCEndpointPtr();
	*endpoint = TSCSDK::instance()->createEndpoint(options, accessManager, *endpointObserver);

	return jlongFromPointer(endpoint);
}


JNIEXPORT void JNICALL Java_com_twilio_signal_impl_TwilioRTCImpl_setCoreLogLevel
  (JNIEnv *env, jobject obj, jint level) {
	TS_CORE_LOG_MODULE(kTSCoreLogModuleSignalSDK, kTSCoreLogLevelDebug, "setCoreLogLevel");
	TSCoreLogLevel coreLogLevel = static_cast<TSCoreLogLevel>(level);
	TSCLogger::instance()->setLogLevel(coreLogLevel);
}


JNIEXPORT jint JNICALL Java_com_twilio_signal_impl_TwilioRTCImpl_getCoreLogLevel
  (JNIEnv *env, jobject obj) {
	TS_CORE_LOG_MODULE(kTSCoreLogModuleSignalSDK, kTSCoreLogLevelDebug, "getCoreLogLevel");
        return TSCLogger::instance()->getLogLevel();
}
