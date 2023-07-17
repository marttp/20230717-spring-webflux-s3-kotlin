package dev.tpcoder.springwebfluxs3.domain.model

import software.amazon.awssdk.services.s3.model.CompletedPart

data class UploadStatus(
    val fileKey: String,
    val contentType: String,
    var uploadId: String? = null,
    var partCounter: Int = 0,
    var buffered: Int = 0,
    val completedParts: MutableMap<Int, CompletedPart> = mutableMapOf()
) {


    fun addBuffered(buffered: Int) {
        this.buffered += buffered
    }

    fun getAddedPartCounter(): Int {
        return ++partCounter
    }
}
