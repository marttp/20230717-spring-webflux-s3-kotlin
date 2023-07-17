# Spring WebFlux 3.1 + Kotlin + S3

Spring WebFlux with Kotlin programming languages to using the S3 upload functionality.

### Prerequisite
- JDK 17+
- Docker
- Postman

### Command
1. Need to setup Localstack S3 bucket but we need to change permission to be able to execute the script.
    ```shell
    chmod +x ./aws/init-aws.sh
    ```
2. Run docker compose
    ```shell
    docker compose up -d
    ```

### Resources
- Webflux - S3 - Localstack.postman_collection.json (Postman collection)

### Reference
[AWS S3 with Spring WebFlux](https://boottechnologies-ci.medium.com/aws-s3-with-spring-webflux-fda9af665397)