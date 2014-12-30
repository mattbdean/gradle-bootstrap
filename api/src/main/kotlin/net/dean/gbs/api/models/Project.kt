package net.dean.gbs.api.models

import java.util.HashSet
import java.util.HashMap

/**
 * Represents a Gradle project
 */
public class Project(val name: String, val group: String, val version: String = "0.0.1") {
    /** Set of directories to create */
    public val directoriesToCreate: MutableSet<String> = HashSet()
    public var license: License = License.NONE

    private val internalLanguages: MutableSet<Language> = HashSet()
    public val languages: Set<Language>
        get() = internalLanguages

    private val internalRawFileWrites: MutableMap<String, String> = HashMap()
    public val rawFileWrites: Map<String, String>
        get() = internalRawFileWrites

    /** Represents the conceptual build.gradle file for this project */
    public val build: GradleBuild = GradleBuild()

    /**
     * Adds a language to be used in this project
     */
    public fun add(lang: Language) {
        internalLanguages.add(lang)
        val name = lang.name().toLowerCase()
        val packageFolders = group.replace('.', '/')
        for (sourceSet in array("main", "test")) {
            // Example: src/main/java/com/example/app
            directoriesToCreate.add("src/$sourceSet/$name/$packageFolders")
        }
        lang.configureOnto(build)
    }

    public fun enqueueRawFileWrite(file: String, text: String) {
        internalRawFileWrites.put(file, text)
    }
}

/**
 * A collection of languages that will be used in this project
 */
public enum class Language : ModularGradleComponent {
    JAVA
    GROOVY
    SCALA
    KOTLIN {
        public override fun configureOnto(build: GradleBuild) {
            // See http://kotlinlang.org/docs/reference/using-gradle.html#configuring-dependencies
            build.metaContext.add(Repository.MAVEN_CENTRAL)
            build.metaContext.add(Dependency("org.jetbrains.kotlin", "kotlin-gradle-plugin", scope = Scope.CLASSPATH))
            build.plugins.add("kotlin")

            build.projectContext.add(Repository.MAVEN_CENTRAL)
            build.projectContext.add(Dependency("org.jetbrains.kotlin", "kotlin-stdlib"))
        }
    }

    public override fun configureOnto(build: GradleBuild) {
        build.plugins.add(name().toLowerCase())
    }
}

/**
 * The different licenses to choose from
 */
public enum class License {
    NONE
    APACHE
    GPL2
    MIT
}

