package com.attafitamim.sensor.core.math

fun matrixMultiplication3x3(A: FloatArray, B: FloatArray): FloatArray {
    val result = FloatArray(9)
    result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6]
    result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7]
    result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8]
    result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6]
    result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7]
    result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8]
    result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6]
    result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7]
    result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8]
    return result
}
