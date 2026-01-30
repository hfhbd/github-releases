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
import javax.inject.Inject

@DisableCachingByDefault
abstract class PublishToGitHubRelease : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val uploadFiles: ConfigurableFileCollection

    @get:Input abstract val token: Property<String>
    @get:Input abstract val apiUrl: Property<String>
    @get:Input abstract val uploadUrl: Property<String>

    @get:Inject
    internal abstract val workerExecutor: WorkerExecutor

    @get:Classpath
    internal abstract val workerClassPath: ConfigurableFileCollection

    @TaskAction
    internal fun publish() {
        val worker = workerExecutor.classLoaderIsolation {
            classpath.from(workerClassPath)
        }

        for (file in uploadFiles) {
            worker.submit(PublishWorker::class.java) {
                this.apiUrl.set(this@PublishToGitHubRelease.apiUrl)
                this.uploadUrl.set(this@PublishToGitHubRelease.uploadUrl)
                this.token.set(this@PublishToGitHubRelease.token)
                this.file.set(file)
            }
        }
    }
}
