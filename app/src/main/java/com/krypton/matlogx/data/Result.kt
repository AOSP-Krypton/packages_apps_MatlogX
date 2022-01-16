package com.krypton.matlogx.data

/**
 * A proxy for kotlin.Result class. Since AOSP source
 * has kotlin version 1.4.3 which has a weird bug when it's Result class
 * is used, we are forced to use this until the source gets upstreamed.
 */
class Result<out T> private constructor(
    private val data: T? = null,
    private val exception: Throwable? = null
) {
    var isSuccess = false
    var isFailure = false

    fun getOrThrow(): T = data!!

    fun exceptionOrNull(): Throwable? = exception

    companion object {
        fun <T> success(t: T): Result<T> =
            Result(t).apply {
                isSuccess = true
            }

        fun <T> failure(e: Throwable): Result<T> =
            Result<T>(exception = e).apply {
                isFailure = true
            }
    }
}