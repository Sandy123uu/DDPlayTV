package com.xyoye.common_component.log.http.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

object HttpLogJson {
    private val moshi: Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    private val adapterCache = ConcurrentHashMap<Type, JsonAdapter<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> adapter(type: Type): JsonAdapter<T> =
        adapterCache.getOrPut(type) { moshi.adapter<Any>(type) } as JsonAdapter<T>

    fun <T> adapter(clazz: Class<T>): JsonAdapter<T> = adapter(clazz as Type)

    fun <T> toJson(value: T, clazz: Class<T>): String = adapter(clazz).toJson(value)

    fun <T> fromJson(json: String, clazz: Class<T>): T? = adapter(clazz).fromJson(json)
}
