package com.sgrecu.homeassignment.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.web.reactive.config.WebFluxConfigurer

/**
 * Configuration for Spring WebFlux.
 * Sets up codecs and other web-related configurations.
 */
@Configuration
class WebConfig : WebFluxConfigurer {

    /**
     * Configure codec for proper JSON handling
     */
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
    }
} 