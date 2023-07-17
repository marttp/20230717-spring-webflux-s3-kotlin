package dev.tpcoder.springwebfluxs3.common

import software.amazon.awssdk.core.SdkResponse

object AwsSdkUtil {

    fun isErrorSdkHttpResponse(sdkResponse: SdkResponse): Boolean {
        return sdkResponse.sdkHttpResponse() == null || !sdkResponse.sdkHttpResponse().isSuccessful
    }
}