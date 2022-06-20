#include <jni.h>
#include <math.h>
#include "mathc.h"

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeInQuad(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeInQuad(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeOutQuad(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeOutQuad(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeInQuart(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeInQuart(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeOutQuart(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeOutQuart(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeInExpo(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeInExpo(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeOutExpo(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeOutExpo(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeInCubic(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeInCubic(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeOutCubic(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeOutCubic(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeInBounce(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x) {
    return easeInBounce(x);
}

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_MathC_easeOutBounce(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x
) {
    return easeOutBounce(x);
}