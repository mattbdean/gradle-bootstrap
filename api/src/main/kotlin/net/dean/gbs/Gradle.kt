package net.dean.gbs

import java.util.HashSet

/**
 * Represents the skeleton of a build.gradle file
 */
public class GradleBuild {
    class object {
        public val LATEST_GRADLE_VERSION: String = "2.2.1"
    }
    /** Dependencies and repositories for the Gradle buildscript */
    public val metaContext: DependencyContext = DependencyContext()
    /** Dependencies and repositories for the Gradle project */
    public val projectContext: DependencyContext = DependencyContext()
    public var testing: TestingFramework = TestingFramework.NONE
        set(value) {
            $testing.deconfigureFrom(this)
            value.configureOnto(this)
            $testing = value
        }
    public var logging: LoggingFramework = LoggingFramework.NONE
        set(value) {
            $logging.deconfigureFrom(this)
            value.configureOnto(this)
            $logging = value
        }

    public val plugins: MutableSet<String> = HashSet()

    /**
     * Adds a Gradle Plugin (see http://plugins.gradle.org)
     */
    public fun addGradlePlugin(plugin: Dependency) {
        // TODO: Use plugins {} closure instead of the clunky buildscript dependencies
        if (Repository.JCENTER !in metaContext.repos) {
            // Gradle plugins are required to be in jCenter
            metaContext.add(Repository.JCENTER)
        }

        metaContext.add(plugin)
    }
}

/**
 * List of dependency scopes
 */
public enum class Scope(public val method: String) {
    COMPILE: Scope("compile")
    RUNTIME: Scope("runtime")
    TEST_COMPILE: Scope("testCompile")
    TEST_RUNTIME: Scope("testRuntime")
    CLASSPATH: Scope("classpath")
}

public data class Dependency(public val group: String,
                             public val name: String,
                             public val version: String = "+", // use dynamic latest version
                             public val extension: String = "",
                             public val scope: Scope = Scope.COMPILE) {
    /**
     * Formats this dependency like how it would appear in a build.gradle file
     */
    public fun depString(): String = "${scope.method} '${format()}'"

    /**
     * Formats this dependency in the <group>:<name>:<version> format, accounting for the extension if present.
     */
    public fun format(): String = "$group:$name:$version${if (extension.isNotEmpty()) "@$extension" else ""}"
}

public enum class Repository(val method: String) {
    class object {
        val DEFAULT: Repository = MAVEN_CENTRAL
    }
    JCENTER: Repository("jcenter()")
    MAVEN_CENTRAL: Repository("mavenCentral()")

    public fun configureOnto(build: GradleBuild, meta: Boolean) {
        (if (meta) build.metaContext else build.projectContext).add(this)
    }
}

/**
 * Contains a set of dependencies and a set of repositories
 */
public class DependencyContext {
    // Expose only the read-only dependencies
    public val deps: Set<Dependency>
        get() = internalDeps

    // Use this internally
    private val internalDeps: MutableSet<Dependency> = HashSet()

    public val repos: Set<Repository>
        get() = internalRepos
    private val internalRepos: MutableSet<Repository> = HashSet()

    fun add(dep: Dependency) {
        for (d in internalDeps) {
            if (d.group.equalsIgnoreCase(dep.group) && d.name.equalsIgnoreCase(dep.name)) {
                // Only add unique dependencies
                break
            }
        }

        internalDeps.add(dep)
    }

    public fun add(dep: Dependency, vararg others: Dependency) {
        // Add the first dependency
        add(dep)
        // Add the others
        for (d in others) {
            add(d)
        }
    }

    public fun add(repo: Repository) {
        if (repo !in internalRepos) {
            internalRepos.add(repo)
        }
    }

    public fun add(repo: Repository, vararg others: Repository) {
        // Add the first repo
        add(repo)
        // Add the rest
        for (r in others) {
            add(r)
        }
    }

    public fun remove(dep: Dependency) {
        internalDeps.remove(dep)
    }

    public fun remove(repo: Repository) {
        internalRepos.remove(repo)
    }
}
