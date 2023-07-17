package dev.tpcoder.springwebfluxs3

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SpringWebfluxS3Application

fun main(args: Array<String>) {
	runApplication<SpringWebfluxS3Application>(*args)
}
