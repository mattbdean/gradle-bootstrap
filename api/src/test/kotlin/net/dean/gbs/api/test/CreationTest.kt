package net.dean.gbs.api.test

import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Test as test
import org.junit.Assert
import kotlin.properties.Delegates
import net.dean.gbs.api.io.ProjectRenderer
import net.dean.gbs.api.models.License
import net.dean.gbs.api.models.Language
import net.dean.gbs.api.models.LoggingFramework
import net.dean.gbs.api.models.TestingFramework
import net.dean.gbs.api.models.Repository
import net.dean.gbs.api.models.Dependency
import net.dean.gbs.api.models.Scope
import net.dean.gbs.api.models.Project
import net.dean.gbs.api.io.RenderReport
import net.dean.gbs.api.io.delete
import net.dean.gbs.api.io.relativePath
import net.dean.gbs.api.io.ZipHelper

public class CreationTest {
    private val processLogger = ProcessOutputAdapter()
    private var renderer: ProjectRenderer by Delegates.notNull()

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
        validateProject(proj, path)
    }

    /**
     * Runs a full suite of validation tests, including validating the report from ProjectRenderer.render(), creating a
     * zip archive of the directory structure, and making sure a 'gradle build' succeeds for the given project
     */
    private fun validateProject(proj: Project, root: Path) {
        validateProjetExportReport(ProjectRenderer(root).render(proj))
        testZip(proj, root)
        validateGradleBuild(root)
    }

    /**
     * Executes "gradle build" in a given directory and asserts that the exit code of that process is equal to 0.
     */
    private fun validateGradleBuild(rootPath: Path) {
        val command = array("gradle", "build")
        val dir = rootPath.toFile()
        val process = ProcessBuilder()
                .directory(dir)
                .command(*command)
                .start()
        processLogger.attach(process, rootPath.getFileName().toString())

        val exitCode = process.waitFor()
        // An exit code of 0 means the process completed without error
        Assert.assertEquals(0, exitCode)
    }

    /**
     * Makes sure every directory and file in the report exist and are either a directory or a file respectively
     */
    fun validateProjetExportReport(report: RenderReport) {
        for (dir in report.directories) {
            assert(Files.isDirectory(dir), "Path $dir claimed to be a directory but was not")
        }
        for (file in report.files) {
            assert(Files.isRegularFile(file), "Path $file claimed to be a file but was not")
        }
    }

    /**
     * Returns a Pair of a Project to its root Pat based on the calling method. The group will be
     * "com.example.$name" and its base path will be "build/projects/$name
     */
    private fun newProject(name: String): Pair<Project, Path> {
        val path = Paths.get("api/build/projects/normal/$name")
        val proj = Project(name, "com.example.$name")
        // Delete the files before generating it so that if we want to examine the crated files after creation, we can.
        delete(path)
        return proj to path
    }

    /**
     * Makes sure that a zip file is created for the given project in the given path
     */
    private fun testZip(proj: Project, basePath: Path) {
        val zipFile = relativePath(basePath, "../../zipped/${proj.name}.zip")
        // Clean up from last time
        delete(zipFile)

        // Make sure the directories exist first
        Files.createDirectories(zipFile.getParent())
        ZipHelper.createZip(basePath, zipFile)
        assert(Files.isRegularFile(zipFile), "Output file does not exist")
    }
}
