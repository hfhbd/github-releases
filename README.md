# github-releases

Project isolation compatible publishing plugin to upload files to an existing GitHub Release.

## Install

This package/Gradle plugin is uploaded to MavenCentral and GitHub packages.

```kotlin
// settings.gradle (.kts)
pluginManagement {
  repositories {
    mavenCentral()
  }
}
```

## Usage

Apply the plugin in each project.

```kotlin
// build.gradle (.kts)
plugins {
  id("io.github.hfhbd.github-releases") version "LATEST"
}
```

You need to setup and configure the publications using the core `maven-publish` plugins.

To publish the publications, call `./gradlew publishToGitHub -PgithubToken=GITHUB_TOKEN`.
Publishing uses the automatic behavior.
