package dev.tpcoder.springwebfluxs3.exception

import org.springframework.core.io.buffer.DataBufferLimitException
import org.springframework.http.HttpStatus
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler
import java.net.URI
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(FileValidatorException::class)
    fun handleFileValidatorException(e: FileValidatorException): ErrorResponse? {
        return ErrorResponse.builder(e, HttpStatus.BAD_REQUEST, e.message!!)
            .title("File Validator Exception")
            .type(URI.create("https://api.error.code"))
            .property("timestamp", Instant.now())
            .build()
    }

    @ExceptionHandler(DataBufferLimitException::class)
    fun handleLimitException(e: DataBufferLimitException): ErrorResponse? {
        return ErrorResponse.builder(e, HttpStatus.BAD_REQUEST, e.message!!)
            .title("File Limit Exception")
            .type(URI.create("https://api.error.code"))
            .property("timestamp", Instant.now())
            .build()
    }

}