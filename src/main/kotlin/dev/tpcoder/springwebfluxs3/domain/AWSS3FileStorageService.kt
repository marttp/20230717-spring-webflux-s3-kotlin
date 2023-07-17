package dev.tpcoder.springwebfluxs3.domain

import dev.tpcoder.springwebfluxs3.domain.model.AWSS3Object
import dev.tpcoder.springwebfluxs3.domain.model.FileResponse
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


interface AWSS3FileStorageService {

    /**
     * Upload object in Amazon S3
     * @param filePart - the request part containing the file to be saved
     * @return Mono of [FileResponse] representing the result of the operation
     */
    fun uploadObject(filePart: FilePart): Mono<FileResponse>

    /**
     * Retrieves byte objects from Amazon S3.
     * @param key object key
     * @return object byte[]
     */
    fun getByteObject(key: String): Mono<ByteArray>

    /**
     * Delete multiple objects from a bucket
     * @param objectKey object key
     */
    fun deleteObject(objectKey: String): Mono<Void>

    /**
     * Returns some or all (up to 1,000) of the objects in a bucket.
     * @return Flux of object key
     */
    fun getObjects(): Flux<AWSS3Object>
}