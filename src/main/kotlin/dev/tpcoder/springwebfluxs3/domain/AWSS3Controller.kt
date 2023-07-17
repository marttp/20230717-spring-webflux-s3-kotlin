package dev.tpcoder.springwebfluxs3.domain

import dev.tpcoder.springwebfluxs3.common.FileUtils
import dev.tpcoder.springwebfluxs3.common.FileUtils.filePartValidator
import dev.tpcoder.springwebfluxs3.domain.model.SuccessResponse
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.text.MessageFormat


@RestController
@RequestMapping("/object")
@Validated
class AWSS3Controller(private val fileStorageService: AWSS3FileStorageService) {


    @PostMapping("/upload")
    fun upload(@RequestPart("file") filePart: Mono<FilePart>): Mono<SuccessResponse> {
        return filePart
            .map { file: FilePart ->
                filePartValidator(file)
                file
            }
            .flatMap { fileStorageService.uploadObject(it) }
            .map {
                SuccessResponse(it, "Upload successfully")
            }
    }

    @GetMapping(path = ["/{fileKey}"])
    fun download(@PathVariable("fileKey") fileKey: String): Mono<SuccessResponse> {
        return fileStorageService.getByteObject(fileKey)
            .map { byteArray -> SuccessResponse(byteArray, "Object byte response") }
    }

    @GetMapping(path = ["/{fileKey}/preview"])
    fun downloadWithPreview(@PathVariable("fileKey") fileKey: String): Mono<ResponseEntity<InputStreamResource>> {
        return fileStorageService.getByteObject(fileKey)
            .map { byteArray ->
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileKey\"")
                    .header(HttpHeaders.CONTENT_TYPE, FileUtils.getMediaType(fileKey).toString())
                    .body(InputStreamResource(byteArray.inputStream()))
            }
    }

    @DeleteMapping(path = ["/{objectKey}"])
    fun deleteFile(@PathVariable("objectKey") objectKey: String): Mono<SuccessResponse> {
        return fileStorageService.deleteObject(objectKey)
            .thenReturn(
                SuccessResponse(
                    null,
                    MessageFormat.format("Object with key: {0} deleted successfully", objectKey)
                )
            )
    }

    @GetMapping
    fun getObject(): Flux<SuccessResponse> {
        return fileStorageService.getObjects()
            .map { objectKey -> SuccessResponse(objectKey, "Result found") }
    }
}