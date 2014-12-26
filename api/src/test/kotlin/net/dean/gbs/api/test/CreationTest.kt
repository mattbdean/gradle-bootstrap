package net.dean.gbs.api.test

import org.testng.annotations.Test as test
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.io.IOException
import org.testng.Assert
import net.dean.gbs.api.License
import net.dean.gbs.api.Language
import net.dean.gbs.api.LoggingFramework
import net.dean.gbs.api.TestingFramework
import net.dean.gbs.api.Repository
import net.dean.gbs.api.Dependency
import net.dean.gbs.api.Scope
import net.dean.gbs.api.Exporter
import net.dean.gbs.api.ProjectRenderer
import net.dean.gbs.api.Project

public class CreationTest {
    public test fun basicCreate() {
        val (proj, path) = newProject("basic")
        proj.license = License.APACHE
        proj.add(Language.JAVA)
        proj.build.logging = LoggingFramework.SLF4J
        proj.build.testing = TestingFramework.TESTNG
        proj.build.projectContext.add(Repository.MAVEN_CENTRAL)
        // Random plugin
        proj.build.addGradlePlugin(Dependency("net.swisstech", "gradle-dropwizard", scope = Scope.CLASSPATH))
        proj.build.plugins.add("application")

        // Render and export the project
        Exporter().export(proj, path, ProjectRenderer().render(proj))
        validateGradleBuild(path)
    }

    /**
     * Executes "gradle build" in a given directory and asserts that the exit code of that process is equal to 0.
     */
    private fun validateGradleBuild(rootPath: Path) {
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

    /**
     * Returns a Pair of a Project to its root Pat based on the calling method. The group will be
     * "com.example.$name" and its base path will be "build/projects/$name
     */
    private fun newProject(name: String): Pair<Project, Path> {
        val path = Paths.get("build/projects/$name")
        val proj = Project(name, "com.example.$name")
        // Delete the files before generating it so that if we want to examine the crated files after creation, we can.
        delete(path)
        return proj to path
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
}
