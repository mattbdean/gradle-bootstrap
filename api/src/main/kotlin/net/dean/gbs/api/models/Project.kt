package net.dean.gbs.api.models

import java.util.HashSet
import java.util.HashMap
import java.util.EnumSet

/**
 * Represents a Gradle project
 */
public class Project(val name: String, val group: String, val version: String = "0.0.1", languages: Collection<Language>) {
    /** Set of directories to create */
    public val directoriesToCreate: MutableSet<String> = HashSet()
    public var license: License = License.NONE
    /** Represents the conceptual build.gradle file for this project */
    public val build: GradleBuild = GradleBuild()
    public val languages: Set<Language> = HashSet(languages);

    {
        if (languages.size() == 0)
            throw IllegalArgumentException("Must have at least one language")

        for (l in languages) {
            val name = l.name().toLowerCase()
            val packageFolders = group.replace('.', '/')
            for (sourceSet in array("main", "test")) {
                // Example: src/main/java/com/example/app
                directoriesToCreate.add("src/$sourceSet/$name/$packageFolders")
            }
            l.configureOnto(build)
        }
    }
}

/**
 * A collection of languages that will be used in this project
 */
public enum class Language : ModularGradleComponent {
    JAVA {   override val dep: Dependency? = null }
    GROOVY { override val dep: Dependency? = Dependency("org.codehaus.groovy", "grovy-all") }
    SCALA {  override val dep: Dependency? = Dependency("org.scala-lang", "scala-library") }
    KOTLIN {
        override val dep: Dependency? = Dependency("org.jetbrains.kotlin", "kotlin-stdlib")

        override fun configureOnto(build: GradleBuild) {
            // See http://kotlinlang.org/docs/reference/using-gradle.html#configuring-dependencies
            build.metaContext.add(Repository.MAVEN_CENTRAL)
            build.metaContext.add(Dependency("org.jetbrains.kotlin", "kotlin-gradle-plugin", scope = Scope.CLASSPATH))
            build.plugins.add("kotlin")

            build.projectContext.add(Repository.MAVEN_CENTRAL)
            build.projectContext.add(dep!!)
        }
    }

    public abstract val dep: Dependency?
    public override fun configureOnto(build: GradleBuild) {
        build.plugins.add(name().toLowerCase())
        if (dep != null)
            build.projectContext.add(dep!!)
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

