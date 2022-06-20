#include <jni.h>
#include <stdio.h>
#include <setjmp.h>
#include <android/bitmap.h>
#include <math.h>
#include <malloc.h>

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCDFAInspection"
static inline uint64_t get_colors(const uint8_t *p) {
    return p[0] + (p[1] << 16) + ((uint64_t) p[2] << 32);
}
#pragma clang diagnostic pop

#define BLUR_SIZE_LIMIT (160 * 160)

static void
fastBlurMore(int imageWidth, int imageHeight, int imageStride, void *pixels, int radius) {
    uint8_t *pix = (uint8_t *) pixels;
    const int w = imageWidth;
    const int h = imageHeight;
    const int stride = imageStride;
    const int r1 = radius + 1;
    const int div = radius * 2 + 1;

    if (radius > 15 || div >= w || div >= h || w * h > BLUR_SIZE_LIMIT ||
        imageStride > imageWidth * 4) {
        return;
    }

    uint64_t *rgb = malloc(imageWidth * imageHeight * sizeof(uint64_t));
    if (rgb == NULL) {
        return;
    }

    int x, y, i;

    int yw = 0;
    const int we = w - r1;
    for (y = 0; y < h; y++) {
        uint64_t cur = get_colors(&pix[yw]);
        uint64_t rgballsum = -radius * cur;
        uint64_t rgbsum = cur * ((r1 * (r1 + 1)) >> 1);

        for (i = 1; i <= radius; i++) {
            uint64_t colors = get_colors(&pix[yw + i * 4]);
            rgbsum += colors * (r1 - i);
            rgballsum += colors;
        }

        x = 0;

#define update(start, middle, end) \
            rgb[y * w + x] = (rgbsum >> 6) & 0x00FF00FF00FF00FF; \
            rgballsum += get_colors (&pix[yw + (start) * 4]) - 2 * get_colors (&pix[yw + (middle) * 4]) + get_colors (&pix[yw + (end) * 4]); \
            rgbsum += rgballsum; \
            x++; \

        while (x < r1) {
            update (0, x, x + r1)
        }
        while (x < we) {
            update (x - r1, x, x + r1)
        }
        while (x < w) {
            update (x - r1, x, w - 1)
        }
#undef update

        yw += stride;
    }

    const int he = h - r1;
    for (x = 0; x < w; x++) {
        uint64_t rgballsum = -radius * rgb[x];
        uint64_t rgbsum = rgb[x] * ((r1 * (r1 + 1)) >> 1);
        for (i = 1; i <= radius; i++) {
            rgbsum += rgb[i * w + x] * (r1 - i);
            rgballsum += rgb[i * w + x];
        }

        y = 0;
        int yi = x * 4;

#define update(start, middle, end) \
            int64_t res = rgbsum >> 6; \
            pix[yi] = res; \
            pix[yi + 1] = res >> 16; \
            pix[yi + 2] = res >> 32; \
            rgballsum += rgb[x + (start) * w] - 2 * rgb[x + (middle) * w] + rgb[x + (end) * w]; \
            rgbsum += rgballsum; \
            y++; \
            yi += stride;

        while (y < r1) {
            update (0, y, y + r1)
        }
        while (y < he) {
            update (y - r1, y, y + r1)
        }
        while (y < h) {
            update (y - r1, y, h - 1)
        }
#undef update
    }
}

static void fastBlur(int imageWidth, int imageHeight, int imageStride, void *pixels, int radius) {
    uint8_t *pix = (uint8_t *) pixels;
    if (pix == NULL) {
        return;
    }
    const int w = imageWidth;
    const int h = imageHeight;
    const int stride = imageStride;
    const int r1 = radius + 1;
    const int div = radius * 2 + 1;
    int shift;
    if (radius == 1) {
        shift = 2;
    } else if (radius == 3) {
        shift = 4;
    } else if (radius == 7) {
        shift = 6;
    } else if (radius == 15) {
        shift = 8;
    } else {
        return;
    }

    if (radius > 15 || div >= w || div >= h || w * h > BLUR_SIZE_LIMIT ||
        imageStride > imageWidth * 4) {
        return;
    }

    uint64_t *rgb = malloc(imageWidth * imageHeight * sizeof(uint64_t));
    if (rgb == NULL) {
        return;
    }

    int x, y, i;

    int yw = 0;
    const int we = w - r1;
    for (y = 0; y < h; y++) {
        uint64_t cur = get_colors(&pix[yw]);
        uint64_t rgballsum = -radius * cur;
        uint64_t rgbsum = cur * ((r1 * (r1 + 1)) >> 1);

        for (i = 1; i <= radius; i++) {
            uint64_t colors = get_colors(&pix[yw + i * 4]);
            rgbsum += colors * (r1 - i);
            rgballsum += colors;
        }

        x = 0;

#define update(start, middle, end)  \
                rgb[y * w + x] = (rgbsum >> shift) & 0x00FF00FF00FF00FFLL; \
                rgballsum += get_colors (&pix[yw + (start) * 4]) - 2 * get_colors (&pix[yw + (middle) * 4]) + get_colors (&pix[yw + (end) * 4]); \
                rgbsum += rgballsum;        \
                x++;                        \

        while (x < r1) {
            update (0, x, x + r1)
        }
        while (x < we) {
            update (x - r1, x, x + r1)
        }
        while (x < w) {
            update (x - r1, x, w - 1)
        }

#undef update
        yw += stride;
    }

    const int he = h - r1;
    for (x = 0; x < w; x++) {
        uint64_t rgballsum = -radius * rgb[x];
        uint64_t rgbsum = rgb[x] * ((r1 * (r1 + 1)) >> 1);
        for (i = 1; i <= radius; i++) {
            rgbsum += rgb[i * w + x] * (r1 - i);
            rgballsum += rgb[i * w + x];
        }

        y = 0;
        int yi = x * 4;

#define update(start, middle, end)  \
                int64_t res = rgbsum >> shift;   \
                pix[yi] = res;              \
                pix[yi + 1] = res >> 16;    \
                pix[yi + 2] = res >> 32;    \
                rgballsum += rgb[x + (start) * w] - 2 * rgb[x + (middle) * w] + rgb[x + (end) * w]; \
                rgbsum += rgballsum;        \
                y++;                        \
                yi += stride;

        while (y < r1) {
            update (0, y, y + r1)
        }
        while (y < he) {
            update (y - r1, y, y + r1)
        }
        while (y < h) {
            update (y - r1, y, h - 1)
        }
#undef update
    }

    free(rgb);
}

JNIEXPORT jint
Java_de_dertyp7214_youtubemusicremote_native_ImageC_blurBitmap(
        JNIEnv *env,
        __attribute__((unused)) jclass class,
        jobject bitmap,
        int radius,
        int unpin,
        int forceLess
) {
    if (!bitmap) {
        return -1;
    }

    AndroidBitmapInfo info;

    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        return -2;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return -3;
    }

    if (!info.width || !info.height) {
        return -4;
    }

    if (!info.stride) {
        return -5;
    }

    void *pixels = 0;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return -6;
    }
    if (radius <= 3 || forceLess == 1) {
        fastBlur(info.width, info.height, info.stride, pixels, radius);
    } else {
        fastBlurMore(info.width, info.height, info.stride, pixels, radius);
    }
    if (unpin) {
        AndroidBitmap_unlockPixels(env, bitmap);
    }
    return 0;
}