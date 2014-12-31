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
import net.dean.gbs.api.models.Project
import net.dean.gbs.api.io.RenderReport
import net.dean.gbs.api.io.delete
import net.dean.gbs.api.io.relativePath
import net.dean.gbs.api.io.ZipHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

public class CreationTest {
    private val processLogger = ProcessOutputAdapter()
    private var renderer: ProjectRenderer by Delegates.notNull()
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    public test fun testSignificantPermutations() {
        // License: NONE vs <any other>
        val licenseOptions = array(License.NONE, License.GPL2)
        // Testing framework: NONE vs TESTNG vs <any other>
        // TestNG tests require a useTestNG() call in the buildscript, while the others don't
        val testingOptions = array(TestingFramework.NONE, TestingFramework.TESTNG, TestingFramework.JUNIT)
        // Logging framework: NONE vs <SLF4J or LOG4J> vs <any other>
        // SLF4J and Log4J both require multiple dependencies, while the others don't
        val loggingOptions = array(LoggingFramework.NONE, LoggingFramework.LOGBACK_CLASSIC, LoggingFramework.APACHE_COMMONS)
        // Languages: <none> vs <any other> vs KOTLIN
        // Kotlin requires a custom meta-dependency (plugin) and a compile-time dependency, while the others require
        // a built-in plugin and a single compile-time dependency
        val languageOptions = array(Language.GROOVY, Language.KOTLIN)
        val projectAmount = licenseOptions.size() * testingOptions.size() * loggingOptions.size() * languageOptions.size()
        log.info("Testing $projectAmount unique projects")

        var counter = 0
        for (license in licenseOptions)
            for (testing in testingOptions)
                for (logging in loggingOptions)
                    for (lang in languageOptions) {
                        val id = "$license-$testing-$logging-$lang"
                        log.info("Evaluating project #${++counter}: (license=$license,testing=$testing,logging=$logging," +
                                "lang=$lang)")
                        val (proj, path) = newProject("permutation-$counter-$id", lang = lang)
                        proj.license = license
                        proj.build.testing = testing
                        proj.build.logging = logging
                        validateProject(proj, path)
                    }
    }

    /**
     * Runs a full suite of validation tests, including validating the report from ProjectRenderer.render(), creating a
     * zip archive of the directory structure, and making sure a 'gradle build' succeeds for the given project
     */
    private fun validateProject(proj: Project, root: Path, zip: Boolean = false) {
        validateProjetExportReport(ProjectRenderer(root).render(proj))
        // Testing zip functionality is only required for one project. Testing multiple will only serve to make the test longer
        if (zip)
            testZip(proj, root)
        validateGradleBuild(proj, root)
    }

    /**
     * Executes "gradle build" in a given directory and asserts that the exit code of that process is equal to 0.
     */
    private fun validateGradleBuild(proj: Project, rootPath: Path) {
        val task = if (proj.languages.size() == 0) {
            // No 'build' task unless there are languages applied, use "tasks" since it still checks syntax
            "tasks"
        } else "build"
        val command = array("gradle", task)
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
    private fun newProject(name: String, lang: Language): Pair<Project, Path> {
        val path = Paths.get("api/build/projects/normal/$name")
        val proj = Project(name, "com.example.$name", lang = lang)
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
