/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_openpeer_javaapi_OPBackgrounding */

#ifndef _Included_com_openpeer_javaapi_OPBackgrounding
#define _Included_com_openpeer_javaapi_OPBackgrounding
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_openpeer_javaapi_OPBackgrounding
 * Method:    toDebug
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_openpeer_javaapi_OPBackgrounding_toDebug
  (JNIEnv *, jclass);

/*
 * Class:     com_openpeer_javaapi_OPBackgrounding
 * Method:    notifyGoingToBackground
 * Signature: (Lcom/openpeer/javaapi/OPBackgroundingCompletionDelegate;)Lcom/openpeer/javaapi/OPBackgroundingQuery;
 */
JNIEXPORT jobject JNICALL Java_com_openpeer_javaapi_OPBackgrounding_notifyGoingToBackground
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_openpeer_javaapi_OPBackgrounding
 * Method:    notifyGoingToBackgroundNow
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_openpeer_javaapi_OPBackgrounding_notifyGoingToBackgroundNow
  (JNIEnv *, jclass);

/*
 * Class:     com_openpeer_javaapi_OPBackgrounding
 * Method:    notifyReturningFromBackground
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_openpeer_javaapi_OPBackgrounding_notifyReturningFromBackground
  (JNIEnv *, jclass);

/*
 * Class:     com_openpeer_javaapi_OPBackgrounding
 * Method:    subscribe
 * Signature: (Lcom/openpeer/javaapi/OPBackgroundingDelegate;J)Lcom/openpeer/javaapi/OPBackgroundingSubscription;
 */
JNIEXPORT jobject JNICALL Java_com_openpeer_javaapi_OPBackgrounding_subscribe
  (JNIEnv *, jclass, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
