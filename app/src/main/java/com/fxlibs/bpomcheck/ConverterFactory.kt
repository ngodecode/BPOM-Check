package com.fxlibs.bpomcheck

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Converter.Factory
import retrofit2.Retrofit
import java.lang.reflect.Type

class ConverterFactory : Factory() {

    override fun responseBodyConverter(
        type: Type?,
        annotations: Array<out Annotation>?,
        retrofit: Retrofit?
    ): Converter<ResponseBody, *> {
        return Converter<ResponseBody, String> { value -> value?.string() ?: "" }
    }

    override fun requestBodyConverter(
        type: Type?,
        parameterAnnotations: Array<out Annotation>?,
        methodAnnotations: Array<out Annotation>?,
        retrofit: Retrofit?
    ): Converter<*, RequestBody> {
        return Converter<String, RequestBody> { value -> RequestBody.create("text/x-gwt-rpc; charset=utf-8".toMediaTypeOrNull(), value) }
    }


}