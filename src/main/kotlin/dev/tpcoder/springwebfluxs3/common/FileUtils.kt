package dev.tpcoder.springwebfluxs3.common

import dev.tpcoder.springwebfluxs3.exception.FileValidatorException
import dev.tpcoder.springwebfluxs3.exception.UploadException
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.util.ObjectUtils
import software.amazon.awssdk.core.SdkResponse
import software.amazon.awssdk.utils.StringUtils
import java.nio.ByteBuffer
import java.text.MessageFormat
import java.util.*
import java.util.function.Consumer


object FileUtils {

    private val logger = LoggerFactory.getLogger(FileUtils.javaClass)

    private val contentTypes = arrayOf(
        "image/png",
        "image/jpg",
        "image/jpeg",
        "image/bmp",
        "image/gif",
        "image/ief",
        "image/pipeg",
        "image/svg+xml",
        "image/tiff"
    )

    private fun isValidType(filePart: FilePart): Boolean {
        return isSupportedContentType(Objects.requireNonNull(filePart.headers().contentType).toString())
    }

    private fun isEmpty(filePart: FilePart): Boolean {
        return (StringUtils.isEmpty(filePart.filename())
                && ObjectUtils.isEmpty(filePart.headers().contentType))
    }

    private fun isSupportedContentType(contentType: String): Boolean {
        return Arrays.asList(*contentTypes).contains(contentType)
    }


    fun dataBufferToByteBuffer(buffers: List<DataBuffer>): ByteBuffer {
        logger.info("Creating ByteBuffer from {} chunks", buffers.size)
        var partSize = 0
        for (b in buffers) {
            partSize += b.readableByteCount()
        }
        val partData: ByteBuffer = ByteBuffer.allocate(partSize)
        buffers.forEach(Consumer { buffer: DataBuffer -> partData.put(buffer.toByteBuffer()) })

        // Reset read pointer to first byte
        partData.rewind()
        logger.info("PartData: capacity={}", partData.capacity())
        return partData
    }

    fun checkSdkResponse(sdkResponse: SdkResponse) {
        if (AwsSdkUtil.isErrorSdkHttpResponse(sdkResponse)) {
            throw UploadException(
                MessageFormat.format(
                    "{0} - {1}",
                    sdkResponse.sdkHttpResponse().statusCode(),
                    sdkResponse.sdkHttpResponse().statusText()
                )
            )
        }
    }

    fun filePartValidator(filePart: FilePart) {
        if (isEmpty(filePart)) {
            throw FileValidatorException("File cannot be empty or null!")
        }
        if (!isValidType(filePart)) {
            throw FileValidatorException("Invalid file type")
        }
    }

    fun getMediaType(fileName: String): MediaType {
        return MediaTypeFactory.getMediaType(fileName)
            .orElse(MediaType.APPLICATION_OCTET_STREAM)
    }

}