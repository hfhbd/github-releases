import io.github.hfhbd.githubreleases.gradle.FileAndDigests
import io.github.hfhbd.githubreleases.gradle.getFilesAndDigests
import java.nio.file.Files
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class WalkTest {
    @Test
    fun filtering() {
        val directory = Files.createTempDirectory("walkTest")
        (directory / "ignored").createDirectory()
        (directory / "ignored" / "foo.bar").createFile().writeText("foo")
        (directory / "ignored" / "foo.bar.md5").createFile().writeText("fooMd5")
        (directory / "ignored" / "foo.bar.sha1").createFile().writeText("fooSha1")
        (directory / "ignored" / "foo.bar.sha256").createFile().writeText("fooSha256")
        (directory / "ignored" / "foo.bar.sha512").createFile().writeText("fooSha512")
        (directory / "maven-metadata.xml")

        val files = setOf(directory.toFile()).getFilesAndDigests()
        assertEquals(
            listOf(
                FileAndDigests(
                    file = (directory / "ignored" / "foo.bar").toFile(),
                    digests = mapOf("sha256" to "fooSha256", "sha512" to "fooSha512")
                ),
            ),
            files,
        )
    }
}
