plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    jacoco
    alias(libs.plugins.pitest)
}

group = "com.sgrecu"
version = "0.0.1-SNAPSHOT"

val basePackage = "${project.group}.homeassignment"
val packageFilters = listOf(
    "chat", "config", "security", "metrics", "monitoring"
)

val excludedPackages = listOf(
    "$basePackage.HomeAssignmentApplication", "*.dto.*", "*.model.*"
)

// PIT common configuration values
val pitAvoidCallsTo = listOf("kotlin.jvm.internal", "kotlin.Result", "reactor.core.publisher", "kotlinx.coroutines")
val pitExcludedMethods = listOf("toString", "hashCode", "equals", "flatMap", "subscribe")
val pitTimeoutConstant = (findProperty("pitest.timeoutConstant") ?: "4000").toString()
val pitTimeoutFactor = (findProperty("pitest.timeoutFactor") ?: "2.0").toString()
val pitJvmArgs = listOf(
    "-Xmx2048m",
    "-XX:+UseG1GC",
    "-Dpitest.timeoutConstant=$pitTimeoutConstant",
    "-Dpitest.timeoutFactor=$pitTimeoutFactor"
)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Spring Boot & Kotlin
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.bundles.kotlin.base)
    implementation(libs.spring.ai.ollama.starter)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)

    // Database
    implementation(libs.spring.boot.starter.data.r2dbc)
    implementation(libs.r2dbc.h2)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.r2dbc.postgresql)
    implementation(libs.postgresql)

    // Security dependencies
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.security.oauth2.jose)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.google.api.client)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.pitest)
    testImplementation(libs.pitest.junit5)
    testImplementation(libs.mockito.kotlin)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Integration testing dependencies
    testImplementation("org.springframework.security:spring-security-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${libs.versions.springAi.get()}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/HomeAssignmentApplication*.*", "**/config/FlywayConfig.*"
                )
            }
        })
    )

    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(tasks.jacocoTestReport.get().classDirectories)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = (findProperty("jacocoLineCoverageThreshold") ?: "0.90").toString().toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = (findProperty("jacocoBranchCoverageThreshold") ?: "0.80").toString().toBigDecimal()
            }
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

pitest {
    junit5PluginVersion.set(libs.versions.pitestJunit5.get())
    pitestVersion.set(libs.versions.pitest.get())

    targetClasses.set(packageFilters.map { "$basePackage.$it.*" })

    excludedClasses.set(excludedPackages + listOf("$basePackage.chat.controller.UpdateConversationRequest"))

    threads.set(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
    outputFormats.set(listOf("XML", "HTML"))
    timestampedReports.set(false)
    mutationThreshold.set(50)
    coverageThreshold.set(90)

    useClasspathFile.set(true)
    verbose.set(false)

    avoidCallsTo.set(pitAvoidCallsTo)
    excludedMethods.set(pitExcludedMethods)
    targetTests.set(listOf("com.sgrecu.homeassignment.*Test"))
    jvmArgs.set(pitJvmArgs)
}

tasks.named("check") {
    dependsOn("pitest")
}

