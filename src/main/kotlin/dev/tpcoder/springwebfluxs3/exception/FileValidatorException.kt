package dev.tpcoder.springwebfluxs3.exception

class FileValidatorException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}