package dev.tpcoder.springwebfluxs3.domain

import dev.tpcoder.springwebfluxs3.common.FileUtils
import dev.tpcoder.springwebfluxs3.common.FileUtils.checkSdkResponse
import dev.tpcoder.springwebfluxs3.configuration.AwsProperties
import dev.tpcoder.springwebfluxs3.domain.model.AWSS3Object
import dev.tpcoder.springwebfluxs3.domain.model.FileResponse
import dev.tpcoder.springwebfluxs3.domain.model.UploadStatus
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

@Service
class AWSS3FileStorageServiceImpl(
    private val s3AsyncClient: S3AsyncClient,
    private val s3ConfigProperties: AwsProperties
) : AWSS3FileStorageService {

    private val logger = LoggerFactory.getLogger(AWSS3FileStorageServiceImpl::class.java)

    override fun getObjects(): Flux<AWSS3Object> {
        logger.info("Listing objects in S3 bucket: {}", s3ConfigProperties.s3BucketName)
        return Flux.from(
            s3AsyncClient.listObjectsV2Paginator(
                ListObjectsV2Request.builder()
                    .bucket(s3ConfigProperties.s3BucketName)
                    .build()
            )
        )
            .flatMap { response ->
                Flux.fromIterable(response.contents())
            }
            .map { s3Object ->
                AWSS3Object(
                    s3Object.key(),
                    s3Object.lastModified(),
                    s3Object.eTag(),
                    s3Object.size()
                )
            }
    }

    override fun deleteObject(objectKey: String): Mono<Void> {
        logger.info("Delete Object with key: {}", objectKey)
        val requestDelete = DeleteObjectRequest.builder()
            .bucket(s3ConfigProperties.s3BucketName)
            .key(objectKey)
            .build()
        return Mono.just(requestDelete)
            .map { s3AsyncClient.deleteObject(it) }
            .flatMap { Mono.fromFuture(it) }
            .then()
    }

    override fun getByteObject(key: String): Mono<ByteArray> {
        logger.debug(
            "Fetching object as byte array from S3 bucket: {}, key: {}",
            s3ConfigProperties.s3BucketName,
            key
        )
        val objectRequest = GetObjectRequest.builder()
            .bucket(s3ConfigProperties.s3BucketName)
            .key(key)
            .build()
        return Mono.just(objectRequest)
            .map { it: GetObjectRequest -> s3AsyncClient.getObject(it, AsyncResponseTransformer.toBytes()) }
            .flatMap { Mono.fromFuture(it) }
            .map { obj -> obj.asByteArray() }
    }


    override fun uploadObject(filePart: FilePart): Mono<FileResponse> {
        val filename = filePart.filename()
        val metadata = mapOf(
            "filename" to filename
        )
        // get media type
        val mediaType: MediaType = filePart.headers().contentType ?: MediaType.APPLICATION_OCTET_STREAM
        val s3AsyncClientMultipartUpload: CompletableFuture<CreateMultipartUploadResponse> = s3AsyncClient
            .createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                    .contentType(mediaType.toString())
                    .key(filename)
                    .metadata(metadata)
                    .bucket(s3ConfigProperties.s3BucketName)
                    .build()
            )
        val uploadStatus = UploadStatus(filename, mediaType.toString())
        return Mono.fromFuture(s3AsyncClientMultipartUpload)
            .flatMapMany { response ->
                checkSdkResponse(response)
                uploadStatus.uploadId = response.uploadId()
                logger.info("Upload object with ID={}", response.uploadId())
                filePart.content()
            }
            .bufferUntil { dataBuffer ->
                // Collect incoming values into multiple List buffers that will be emitted by the resulting Flux each time the given predicate returns true.
                uploadStatus.addBuffered(dataBuffer.readableByteCount())
                if (uploadStatus.buffered >= s3ConfigProperties.multipartMinPartSize) {
                    logger.info(
                        "BufferUntil - returning true, bufferedBytes={}, partCounter={}, uploadId={}",
                        uploadStatus.buffered, uploadStatus.partCounter, uploadStatus.uploadId
                    )
                    // reset buffer
                    uploadStatus.buffered = 0
                    return@bufferUntil true
                }
                false
            }
            .map { FileUtils.dataBufferToByteBuffer(it) } // upload part
            .flatMap { byteBuffer: ByteBuffer ->
                uploadPartObject(
                    uploadStatus,
                    byteBuffer
                )
            }
            .onBackpressureBuffer()
            .reduce(uploadStatus) { status, completedPart ->
                logger.info("Completed: PartNumber={}, etag={}", completedPart.partNumber(), completedPart.eTag())
                status.completedParts
                status.completedParts[completedPart.partNumber()] = completedPart
                status
            }
            .flatMap { completeMultipartUpload(it) }
            .map { response ->
                checkSdkResponse(response)
                logger.info("upload result: {}", response.toString())
                FileResponse(
                    filename,
                    uploadStatus.uploadId!!,
                    response.location(),
                    uploadStatus.contentType,
                    response.eTag()
                )
            }
    }

    /**
     * Uploads a part in a multipart upload.
     */
    private fun uploadPartObject(uploadStatus: UploadStatus, buffer: ByteBuffer): Mono<CompletedPart> {
        val partNumber: Int = uploadStatus.getAddedPartCounter()
        logger.info("UploadPart - partNumber={}, contentLength={}", partNumber, buffer.capacity())
        val uploadPartResponseCompletableFuture: CompletableFuture<UploadPartResponse> = s3AsyncClient.uploadPart(
            UploadPartRequest.builder()
                .bucket(s3ConfigProperties.s3BucketName)
                .key(uploadStatus.fileKey)
                .partNumber(partNumber)
                .uploadId(uploadStatus.uploadId)
                .contentLength(buffer.capacity().toLong())
                .build(),
            AsyncRequestBody.fromPublisher(Mono.just(buffer))
        )
        return Mono.fromFuture(uploadPartResponseCompletableFuture)
            .map { uploadPartResult: UploadPartResponse ->
                checkSdkResponse(uploadPartResult)
                logger.info("UploadPart - complete: part={}, etag={}", partNumber, uploadPartResult.eTag())
                CompletedPart.builder()
                    .eTag(uploadPartResult.eTag())
                    .partNumber(partNumber)
                    .build()
            }
    }

    /**
     * This method is called when a part finishes uploading. It's primary function is to verify the ETag of the part
     * we just uploaded.
     */
    private fun completeMultipartUpload(uploadStatus: UploadStatus): Mono<CompleteMultipartUploadResponse> {
        logger.info(
            "CompleteUpload - fileKey={}, completedParts.size={}",
            uploadStatus.fileKey, uploadStatus.completedParts.size
        )
        val multipartUpload = CompletedMultipartUpload.builder()
            .parts(uploadStatus.completedParts.values)
            .build()
        return Mono.fromFuture(
            s3AsyncClient.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                    .bucket(s3ConfigProperties.s3BucketName)
                    .uploadId(uploadStatus.uploadId)
                    .multipartUpload(multipartUpload)
                    .key(uploadStatus.fileKey)
                    .build()
            )
        )
    }
}