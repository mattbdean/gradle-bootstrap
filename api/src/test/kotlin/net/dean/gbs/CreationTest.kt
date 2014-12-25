package net.dean.gbs

import org.testng.annotations.Test as test
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.testng.Assert

public class CreationTest {
    public test fun basicCreate() {
        val projName = "Example"
        val projPath = Paths.get("/home/matthew/test/abc/$projName")
        delete(projPath)
        val proj = Project(projName, "com.example.app")
        proj.license = License.APACHE
        proj.add(Language.JAVA)
        proj.build.logging = LoggingFramework.SLF4J
        proj.build.testing = TestingFramework.TESTNG
        proj.build.projectContext.add(Repository.MAVEN_CENTRAL)
        proj.build.addGradlePlugin(Dependency("net.swisstech", "gradle-dropwizard", scope = Scope.CLASSPATH))
        proj.build.plugins.add("application")

        Exporter().export(proj, projPath, ProjectRenderer().render(proj))
        validateGradleBuild(projPath)
    }

    private fun delete(path: Path) {
        if (!Files.exists(path))
            return
        if (Files.isDirectory(path)) {
            // Recursively delete file tree
            Files.walkFileTree(path, object: SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            })
        } else {
            // Delete file
            Files.delete(path)
        }
    }

    /**
     * Executes "gradle build" in a given directory and asserts that the exit code of that process is equal to 0.
     */
    public fun validateGradleBuild(rootPath: Path) {
        val command = array("gradle", "build")
        val dir = rootPath.toFile()
        println("Executing command '${command.join(" ")}' in directory '${dir.getAbsolutePath()}'")
        val process = ProcessBuilder()
                .directory(dir)
                .command(*command)
                .inheritIO()
                .start()
        // Should have an exit value of 0
        val exitCode = process.waitFor()
        println("Finished")
        Assert.assertEquals(exitCode, 0)
    }
}
