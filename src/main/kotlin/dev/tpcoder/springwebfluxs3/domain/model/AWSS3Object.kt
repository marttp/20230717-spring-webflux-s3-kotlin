package dev.tpcoder.springwebfluxs3.domain.model

import java.time.Instant

data class AWSS3Object(
    val key: String,
    val lastModified: Instant,
    val eTag: String,
    val size: Long
)
