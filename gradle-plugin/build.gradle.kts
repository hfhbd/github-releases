plugins {
    `kotlin-dsl`
    id("setup")
}

kotlin.jvmToolchain(21)

dependencies {
    implementation(projects.gradleWorker)
}

gradlePlugin.plugins.configureEach {
    displayName = "hfhbd githubReleasesWorker Gradle Plugin"
    description = "hfhbd githubReleasesWorker Gradle Plugin"
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}
