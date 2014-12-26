package net.dean.gbs.api.test

import org.testng.annotations.Test as test
import net.dean.gbs.api.Language
import net.dean.gbs.api.LoggingFramework
import net.dean.gbs.api.Dependency
import net.dean.gbs.api.Scope
import net.dean.gbs.api.Project

public class ProjectConfigTest {
    public test fun testAddLang() {
        val proj = newProject()
        proj.add(Language.JAVA)
        assert("src/main/java/com/example/app" in proj.directoriesToCreate)
        assert("src/test/java/com/example/app" in proj.directoriesToCreate)
    }

    public test fun testAddFrameworks() {
        val proj = newProject()
        proj.build.logging = LoggingFramework.SLF4J
        assert(proj.build.projectContext.deps.filter { it.name == "slf4j-api" && it.group == "org.slf4j" }.size() > 0)

        proj.build.logging = LoggingFramework.LOGBACK_CLASSIC
        // Make sure it removed the SLF4J dependencies and added Logback's
        assert(proj.build.projectContext.deps.filter { it.name == "slf4j-api" && it.group == "org.slf4j" }.size() == 0)
        assert(proj.build.projectContext.deps.filter { it.name == "logback-classic" && it.group == "ch.qos.logback" }.size() > 0)
    }

    public test fun formatDependency() {
        val dep = Dependency(group = "com.mydep", name = "mydep", version = "1.0.0", extension = "jar", scope = Scope.COMPILE)
        assert(dep.format() == "com.mydep:mydep:1.0.0@jar")
        assert(dep.gradleFormat() == "compile 'com.mydep:mydep:1.0.0@jar'")

        val dep2 = Dependency(group = "com.mydep", name = "mydep", version = "1.0.0", scope = Scope.TEST_RUNTIME)
        assert(dep2.format() == "com.mydep:mydep:1.0.0")
        assert(dep2.gradleFormat() == "testRuntime 'com.mydep:mydep:1.0.0'")
    }

    private fun newProject(): Project = Project("test proj", "com.example.app")
}
