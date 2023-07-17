package dev.tpcoder.springwebfluxs3.domain.model

data class FileResponse(
    val name: String,
    val uploadId: String,
    val path: String,
    val type: String,
    val eTag: String
)
