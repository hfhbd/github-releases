package io.github.hfhbd.githubreleases.gradle

import io.github.hfhbd.githubreleases.gradle.workactions.PublishWorker
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class PublishToGitHubRelease : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val uploadFiles: ConfigurableFileCollection

    @get:Input
    abstract val token: Property<String>

    @get:Input
    abstract val apiUrl: Property<String>

    @get:Input
    abstract val uploadUrl: Property<String>

    @get:Inject
    internal abstract val workerExecutor: WorkerExecutor

    @get:Classpath
    internal abstract val workerClassPath: ConfigurableFileCollection

    @TaskAction
    internal fun publish() {
        val worker = workerExecutor.classLoaderIsolation {
            classpath.from(workerClassPath)
        }

        for (entry in uploadFiles.getFilesAndDigests()) {
            worker.submit(PublishWorker::class.java) {
                this.apiUrl.set(this@PublishToGitHubRelease.apiUrl)
                this.uploadUrl.set(this@PublishToGitHubRelease.uploadUrl)
                this.token.set(this@PublishToGitHubRelease.token)
                this.file.set(entry.file)
                this.digests.set(entry.digests)
            }
        }
    }
}

internal fun Iterable<File>.getFilesAndDigests(): List<FileAndDigests> {
    val realFiles = mutableListOf<FileAndDigests>()

    for (entry in this) {
        for (file in entry.walk()) {
            if (file.isDirectory) {
                continue
            }

            if (file.name.startsWith("maven-metadata.xml")) {
                continue
            }

            if (
                file.name.endsWith(".md5") ||
                file.name.endsWith(".sha1") ||
                file.name.endsWith(".sha256") ||
                file.name.endsWith(".sha512")
                ) {
                continue
            }

            realFiles.add(
                FileAndDigests(
                    file = file,
                    digests = mapOf(
                        "sha256" to File(file.parentFile, file.name + ".sha256").readText(),
                        "sha512" to File(file.parentFile, file.name + ".sha512").readText(),
                    )
                )
            )
        }
    }

    return realFiles
}

internal data class FileAndDigests(
    val file: File,
    val digests: Map<String, String>,
)
