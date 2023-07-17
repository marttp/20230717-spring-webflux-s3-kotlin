package dev.tpcoder.springwebfluxs3.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws", ignoreUnknownFields = false)
data class AwsProperties(
    val accessKey: String, // Aws access key ID
    val secretKey: String, // Aws secret access key
    val region: String, // Aws region
    val s3BucketName: String, // Aws S3 bucket name
    val multipartMinPartSize: Int, // AWS S3 requires that file parts must have at least 5MB, except for the last part.
    val endpoint: String // S3 endpoint url
)