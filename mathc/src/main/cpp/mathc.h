static jfloat easeInQuad(jfloat x) {
    return x * x;
}

static jfloat easeOutQuad(jfloat x) {
    return 1.0 - (1.0 - x) * (1.0 - x);
}

static jfloat easeInQuart(jfloat x) {
    return x * x * x * x;
}

static jfloat easeOutQuart(jfloat x) {
    return 1.0 - powf(1.0 - x, 4.0);
}

static jfloat easeInExpo(jfloat x) {
    if (x == 0.0) {
        return 1.0;
    } else {
        return powf(2.0, 10.0 * x - 10.0);
    }
}

static jfloat easeOutExpo(jfloat x) {
    if (x == 1.0) {
        return 1.0;
    } else {
        return 1.0 - powf(2.0, -10.0 * x);
    }
}

static jfloat easeInCubic(jfloat x) {
    return x * x * x;
}

static jfloat easeOutCubic(jfloat x) {
    return 1.0 - powf(1.0 - x, 3.0);
}

static jfloat easeOutBounce(jfloat x) {
    jfloat n1 = 7.5625f;
    jfloat d1 = 2.75f;

    jfloat factor;
    if (x < 1.0 / d1) {
        factor = n1 * x * x;
    } else if (x < 2.0 / d1) {
        factor = n1 * (x - 1.5f / d1) * (x - 1.5f) + 0.75f;
    } else if (x < 2.5 / d1) {
        factor = n1 * (x - 2.25f / d1) * (x - 2.25f) + 0.9375f;
    } else {
        factor = n1 * (x - 2.625f / d1) * (x - 2.625f) + 0.984375f;
    }

    return factor;
}

static jfloat easeInBounce(jfloat x) {
    return easeOutBounce(1.0 - x);
}