package net.dean.gbs.api.test

import org.junit.Test as test
import net.dean.gbs.api.models.Language
import net.dean.gbs.api.models.LoggingFramework
import net.dean.gbs.api.models.Dependency
import net.dean.gbs.api.models.Scope
import net.dean.gbs.api.models.Project

public class ProjectConfigTest {
    public test fun testProperDirectories() {
        val proj = newProject(*Language.values())
        for (lang in Language.values()) {
            assert("src/main/${lang.name().toLowerCase()}/com/example/app" in proj.directoriesToCreate,
                    "Did not find main source set for $lang")
            assert("src/test/${lang.name().toLowerCase()}/com/example/app" in proj.directoriesToCreate,
                    "Did not find test source set for $lang")
        }

        assert("src/main/resources" in proj.directoriesToCreate, "src/main/resources not found")
        assert("src/test/resources" in proj.directoriesToCreate, "src/test/resources not found")
    }

    public test fun testAddFrameworks() {
        val proj = newProject(Language.JAVA)
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

    private fun newProject(vararg langs: Language): Project {
        if (langs.isEmpty())
            throw IllegalArgumentException("Must have more than one language")

        return Project("test-proj", "com.example.app", languages = setOf(*langs))
    }
}
