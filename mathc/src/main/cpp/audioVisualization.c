#include <jni.h>
#include <math.h>
#include "mathc.h"

JNIEXPORT jfloat JNICALL
Java_de_dertyp7214_mathc_AudioVisualization_calculateBottomSpace(
        __attribute__((unused)) JNIEnv *env,
        __attribute__((unused)) jobject class,
        jfloat x, jfloat width,
        jint bottom_left_corner,
        jint bottom_right_corner,
        jfloat bar_width,
        jfloat bar_height) {

    jfloat cornerBottomSpace = 0.0;
    if (bar_height != 0.0) {
        jfloat leftRadius = bottom_left_corner * 1.1;
        jfloat rightRadius = bottom_right_corner * 1.1;
        if (x + bar_width < leftRadius) {
            jfloat point = leftRadius - x;
            cornerBottomSpace = point * easeInQuad(1.0 / leftRadius * point);
        } else if (x - bar_width > (width - rightRadius)) {
            jfloat point = rightRadius - (width - x);
            cornerBottomSpace = point * easeInQuad(1.0 / rightRadius * point);
        }
    }
    return cornerBottomSpace;
}