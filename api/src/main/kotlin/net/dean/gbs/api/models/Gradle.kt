package net.dean.gbs.api.models

import java.util.*
import kotlin.text.equals
import kotlin.text.isNotEmpty

/**
 * Represents the skeleton of a build.gradle file
 */
public class GradleBuild {
    companion object {
        public val GRADLE_WRAPPER_VERSION: String = "2.2.1"
    }
    /** Dependencies and repositories for the Gradle buildscript */
    public val metaContext: DependencyContext = DependencyContext()
    /** Dependencies and repositories for the Gradle project */
    public val projectContext: DependencyContext = DependencyContext()

    public var _testing: TestingFramework = TestingFramework.NONE
    public var testing: TestingFramework
        set(value) {
            _testing.deconfigureFrom(this)
            value.configureOnto(this)
            _testing = value
        }
        get() = _testing

    private var _logging: LoggingFramework = LoggingFramework.NONE
    public var logging: LoggingFramework
        set(value) {
            _logging.deconfigureFrom(this)
            value.configureOnto(this)
            _logging = value
        }
        get() = _logging

    public val plugins: MutableSet<String> = HashSet()

    /**
     * Adds a Gradle Plugin (see http://plugins.gradle.org)
     */
    public fun addGradlePlugin(plugin: Dependency) {
        if (plugin.scope != Scope.CLASSPATH) {
            throw IllegalArgumentException("Gradle plugins must use the 'classpath' scope")
        }
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
    COMPILE("compile"),
    RUNTIME("runtime"),
    TEST_COMPILE("testCompile"),
    TEST_RUNTIME("testRuntime"),
    CLASSPATH("classpath")
}

public data class Dependency(public val group: String,
                             public val name: String,
                             public val version: String = "+", // use dynamic latest version
                             public val extension: String = "",
                             public val scope: Scope = Scope.COMPILE) : Comparable<Dependency> {
    override fun compareTo(other: Dependency): Int {
        val scopeComp = scope.compareTo(other.scope)
        if (scopeComp != 0) {
            return scopeComp
        } else {
            val groupComp = group.compareTo(other.group)
            if (groupComp != 0) {
                return groupComp
            } else {
                val nameComp = name.compareTo(other.name)
                if (nameComp != 0) {
                    return nameComp
                } else {
                    val versionComp = version.compareTo(other.version)
                    if (versionComp != 0) {
                        return versionComp
                    } else {
                        return extension.compareTo(other.extension)
                    }
                }
            }
        }
    }

    /**
     * Formats this dependency like how it would appear in a build.gradle file
     */
    public fun gradleFormat(): String = "${scope.method} '${format()}'"

    /**
     * Formats this dependency in the <group>:<name>:<version> format, accounting for the extension if present.
     */
    public fun format(): String = "$group:$name:$version${if (extension.isNotEmpty()) "@$extension" else ""}"
}

public enum class Repository(val method: String) {
    JCENTER("jcenter()"),
    MAVEN_CENTRAL("mavenCentral()")
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
            if (d.group.equals(dep.group, ignoreCase = true) && d.name.equals(dep.name, ignoreCase = true)) {
                // Only add unique dependencies
                return
            }
        }

        internalDeps.add(dep)
        if (internalDeps.size > 0 && internalRepos.size == 0) {
            // Make sure we can resolve our dependencies
            internalRepos.add(Repository.MAVEN_CENTRAL)
        }
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
