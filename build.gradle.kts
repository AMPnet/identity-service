import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    val kotlinVersion = "1.5.10"
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion

    id("org.springframework.boot") version "2.5.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.google.cloud.tools.jib") version "2.8.0"
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    id("io.gitlab.arturbosch.detekt").version("1.16.0")
    idea
    jacoco
}

group = "com.ampnet"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.github.microutils:kotlin-logging:2.0.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jib {
    val dockerUsername: String = System.getenv("DOCKER_USERNAME") ?: "DOCKER_USERNAME"
    val dockerPassword: String = System.getenv("DOCKER_PASSWORD") ?: "DOCKER_PASSWORD"
    to {
        image = "ampnet/identity-service:$version"
        auth {
            username = dockerUsername
            password = dockerPassword
        }
        tags = setOf("latest")
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}

jacoco.toolVersion = "0.8.7"
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        xml.destination = file("$buildDir/reports/jacoco/report.xml")
        csv.isEnabled = false
        html.destination = file("$buildDir/reports/jacoco/html")
    }
    sourceDirectories.setFrom(listOf(file("${project.projectDir}/src/main/kotlin")))
    classDirectories.setFrom(
        fileTree("$buildDir/classes/kotlin/main").apply {
            exclude("**/model/**", "**/pojo/**")
        }
    )
    dependsOn(tasks.test)
}
tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(sourceSets.main.get().output.asFileTree)
    violationRules {
        rule {
            limit {
                minimum = "0.2".toBigDecimal()
            }
        }
    }
    mustRunAfter(tasks.jacocoTestReport)
}

detekt {
    input = files("src/main/kotlin")
    config = files("detekt-config.yml")
}

task("qualityCheck") {
    dependsOn(tasks.ktlintCheck, tasks.detekt, tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}

tasks.asciidoctor {
    attributes(
        mapOf(
            "snippets" to file("build/generated-snippets"),
            "version" to version,
            "date" to SimpleDateFormat("yyyy-MM-dd").format(Date())
        )
    )
    dependsOn(tasks.test)
}

tasks.register<Copy>("copyDocs") {
    from(file("$buildDir/asciidoc/html5"))
    into(file("src/main/resources/static/docs"))
    dependsOn(tasks.asciidoctor)
}
