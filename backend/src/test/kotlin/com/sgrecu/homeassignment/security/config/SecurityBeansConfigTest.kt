package com.sgrecu.homeassignment.security.config

import com.sgrecu.homeassignment.config.AppProperties
import com.sgrecu.homeassignment.security.jwt.JwtProperties
import com.sgrecu.homeassignment.security.oauth.GoogleAuthProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SecurityBeansConfig::class])
@EnableConfigurationProperties(value = [
    JwtProperties::class, 
    GoogleAuthProperties::class, 
    AppProperties::class
])
class SecurityBeansConfigTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext
    
    @Test
    fun `security beans should be properly initialized`() {
        // Verify that all required property beans are available in the context
        val jwtProperties = applicationContext.getBean(JwtProperties::class.java)
        val googleAuthProperties = applicationContext.getBean(GoogleAuthProperties::class.java)
        val appProperties = applicationContext.getBean(AppProperties::class.java)
        
        assertNotNull(jwtProperties)
        assertNotNull(googleAuthProperties)
        assertNotNull(appProperties)
    }
    
    @Test
    fun `security beans config should be properly initialized`() {
        // Verify the security beans config is available
        val securityBeansConfig = applicationContext.getBean(SecurityBeansConfig::class.java)
        assertNotNull(securityBeansConfig)
    }
} 