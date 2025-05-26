package com.sgrecu.homeassignment.config

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import reactor.netty.http.server.HttpServer

@Configuration
class NettyConfig(private val appProperties: AppProperties) {

    @Bean
    @Primary
    @Order(-1)
    fun customNettyWebServerFactoryCustomizer(): WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            factory.addServerCustomizers(
                { httpServer: HttpServer ->
                    httpServer.httpRequestDecoder { spec ->
                        spec.maxInitialLineLength(appProperties.server.maxInitialLineLength)
                            .maxHeaderSize(appProperties.server.maxHeaderSize)
                    }
                })
        }
    }
} 