package io.github.hfhbd.githubreleases.gradle.workactions

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.content.LocalFileContent
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.jsonIo
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import org.gradle.api.logging.Logging as GradleLogging

abstract class PublishWorker : WorkAction<PublishWorker.PublishParameters> {
    interface PublishParameters : WorkParameters {
        val apiUrl: Property<String>
        val uploadUrl: Property<String>
        val file: RegularFileProperty
        val digests: MapProperty<String, String>
        val token: Property<String>
    }

    private val gradleLogger = GradleLogging.getLogger(PublishWorker::class.java)

    override fun execute() {
        val client = HttpClient(Java) {
            defaultRequest {
                url(parameters.apiUrl.get())
            }
            configureGitHub(token = parameters.token.get())

            install(Logging) {
                level = if (gradleLogger.isDebugEnabled) {
                    LogLevel.ALL
                } else {
                    LogLevel.INFO
                }
                logger = object : Logger {
                    override fun log(message: String) {
                        if (gradleLogger.isDebugEnabled) {
                            gradleLogger.debug(message)
                        } else {
                            gradleLogger.info(message)
                        }
                    }
                }
            }
        }
        runBlocking {
            val createdAsset = client.uploadReleaseAsset(
                uploadUrl = parameters.uploadUrl.get(),
                asset = parameters.file.get().asFile,
            )
            if (createdAsset.digest != null) {
                val (digestAlg, remoteDigest) = createdAsset.digest.split(":")
                val localDigest = parameters.digests.orNull?.get(digestAlg.lowercase()) ?: return@runBlocking
                require(localDigest == remoteDigest) {
                    "Remote digest ${createdAsset.digest} of ${createdAsset.url} doesn't match local digest $localDigest using $digestAlg algorithm."
                }
            }
        }
    }
}

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.configureGitHub(
    token: String,
) {
    expectSuccess = true
    defaultRequest {
        bearerAuth(token)
    }
    install(ContentNegotiation) {
        jsonIo()
    }
}

private suspend fun HttpClient.uploadReleaseAsset(
    uploadUrl: String,
    asset: File,
): CreatedAsset {
    val cleanUploadUrl = uploadUrl.substringBefore('{')
    return post(cleanUploadUrl) {
        parameter("name", asset.name)
        contentType(ContentType.Application.OctetStream)
        setBody(
            LocalFileContent(
                asset,
                contentType = ContentType.Application.OctetStream,
            )
        )
    }.body<CreatedAsset>()
}

@Serializable
data class CreatedAsset(
    val url: String,
    val id: Long,
    val digest: String? = null,
)
