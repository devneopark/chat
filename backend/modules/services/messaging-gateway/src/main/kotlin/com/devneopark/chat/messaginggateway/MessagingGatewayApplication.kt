package com.devneopark.chat.messaginggateway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class MessagingGatewayApplication

fun main(args: Array<String>) {
    SpringApplication.run(MessagingGatewayApplication::class.java, *args)
}