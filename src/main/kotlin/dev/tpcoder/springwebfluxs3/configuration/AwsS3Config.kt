package dev.tpcoder.springwebfluxs3.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI
import java.time.Duration


@Configuration
class AwsS3Config(private val s3ConfigProperties: AwsProperties) {

    @Bean
    fun s3AsyncClient(awsCredentialsProvider: AwsCredentialsProvider?): S3AsyncClient? {
        return S3AsyncClient.builder()
            .httpClient(sdkAsyncHttpClient())
            .region(Region.of(s3ConfigProperties.region))
            .credentialsProvider(awsCredentialsProvider)
            .endpointOverride(URI.create(s3ConfigProperties.endpoint))
            .forcePathStyle(true)
            .serviceConfiguration(s3Configuration()).build()
    }

    private fun sdkAsyncHttpClient(): SdkAsyncHttpClient? {
        return NettyNioAsyncHttpClient.builder()
            .writeTimeout(Duration.ZERO)
            .maxConcurrency(64)
            .build()
    }

    private fun s3Configuration(): S3Configuration? {
        return S3Configuration.builder()
            .checksumValidationEnabled(false)
            .chunkedEncodingEnabled(true)
            .build()
    }

    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider? {
        return AwsCredentialsProvider {
            AwsBasicCredentials.create(
                s3ConfigProperties.accessKey,
                s3ConfigProperties.secretKey
            )
        }
    }

}