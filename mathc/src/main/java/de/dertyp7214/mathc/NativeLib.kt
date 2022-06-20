package de.dertyp7214.mathc

object MathC {
    fun init() {
        System.loadLibrary("mathc")
    }

    external fun easeInQuad(x: Float): Float
    external fun easeOutQuad(x: Float): Float
    external fun easeInQuart(x: Float): Float
    external fun easeOutQuart(x: Float): Float
    external fun easeInExpo(x: Float): Float
    external fun easeOutExpo(x: Float): Float
    external fun easeInCubic(x: Float): Float
    external fun easeOutCubic(x: Float): Float
    external fun easeInBounce(x: Float): Float
    external fun easeOutBounce(x: Float): Float
}

object AudioVisualization {
    external fun calculateBottomSpace(
        x: Float,
        width: Float,
        bottomLeftCorner: Int,
        bottomRightCorner: Int,
        barWidth: Float,
        barHeight: Float
    ): Float
}