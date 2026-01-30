import io.github.hfhbd.githubreleases.gradle.*
import io.github.hfhbd.githubreleases.gradle.ktorClientContentNegotiation
import io.github.hfhbd.githubreleases.gradle.ktorJava
import io.github.hfhbd.githubreleases.gradle.ktorLogging
import io.github.hfhbd.githubreleases.gradle.ktorSerializationKotlinxJson

plugins {
    id("maven-publish")
}

val githubReleasesWorker = configurations.dependencyScope("githubReleasesWorker")
val githubReleasesWorkerClassPath = configurations.resolvable("githubReleasesWorkerClasspath") {
    extendsFrom(githubReleasesWorker.get())
}

dependencies {
    githubReleasesWorker(ktorJava)
    githubReleasesWorker(ktorLogging)
    githubReleasesWorker(ktorClientContentNegotiation)
    githubReleasesWorker(ktorSerializationKotlinxJson)
}

val projectGroup = provider { group.toString() }
val projectName = name
val projectVersion = provider { version.toString() }

val localGitHubRepoDir = projectVersion.flatMap { layout.buildDirectory.dir("localgithub/$it/repo") }
val repoFiles = files(localGitHubRepoDir)

val publishToGitHubRelease by tasks.registering(PublishToGitHubRelease::class) {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    this.uploadFiles.from(repoFiles)
    workerClassPath.from(githubReleasesWorkerClassPath)
}

tasks.publish {
    dependsOn(publishToGitHubRelease)
}

publishing {
    val repoName = "localGitHub"

    repositories.maven {
        name = repoName
        url = uri(localGitHubRepoDir)
    }

    publications.withType<MavenPublication>().all {
        val pubName = name.replaceFirstChar { it.uppercaseChar() }

        val publishToLocalGitHub = tasks.named(
            "publish${pubName}PublicationTo${repoName.replaceFirstChar { it.uppercaseChar() }}Repository",
        )
        repoFiles.builtBy(publishToLocalGitHub)
    }
}
