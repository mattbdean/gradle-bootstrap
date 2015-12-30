package net.dean.gbs.api.models

import java.util.*
import kotlin.text.replace
import kotlin.text.substring
import kotlin.text.toLowerCase

/**
 * Represents a Gradle project
 */
public class Project(val name: String,
                     val group: String,
                     val version: String,
                     val gitRepo: String?,
                     val gitInit: Boolean = gitRepo != null,
                     languages: Collection<Language>) {
    /** Set of directories to create */
    public val directoriesToCreate: MutableSet<String> = HashSet()
    public var license: License = License.NONE
    /** Represents the conceptual build.gradle file for this project */
    public val build: GradleBuild = GradleBuild()
    public val languages: Set<Language> = HashSet(languages);

    init {
        if (languages.size == 0)
            throw IllegalArgumentException("Must have at least one language")

        for (sourceSet in arrayOf("main", "test")) {
            for (l in languages) {
                val name = l.name.toLowerCase()
                val packageFolders = group.replace('.', '/')
                // Example: src/main/java/com/example/app
                directoriesToCreate.add("src/$sourceSet/$name/$packageFolders")
                l.configureOnto(build)
            }

            // src/main/resources
            directoriesToCreate.add("src/$sourceSet/resources")
        }
    }
}

/**
 * A collection of languages that will be used in this project
 */
public enum class Language(public val dep: Dependency? = null) : ModularGradleComponent, HumanReadable {
    JAVA(),
    GROOVY(Dependency("org.codehaus.groovy", "grovy-all")),
    SCALA(Dependency("org.scala-lang", "scala-library")),
    KOTLIN(Dependency("org.jetbrains.kotlin", "kotlin-stdlib")) {

        override fun configureOnto(build: GradleBuild) {
            // See http://kotlinlang.org/docs/reference/using-gradle.html#configuring-dependencies
            build.metaContext.add(Repository.MAVEN_CENTRAL)
            build.metaContext.add(Dependency("org.jetbrains.kotlin", "kotlin-gradle-plugin", scope = Scope.CLASSPATH))
            build.plugins.add("kotlin")

            build.projectContext.add(Repository.MAVEN_CENTRAL)
            build.projectContext.add(dep!!)
        }
    };

    public override val humanReadable = name[0].toString() + name.substring(1).toLowerCase()
    public override fun configureOnto(build: GradleBuild) {
        build.plugins.add(name.toLowerCase())
        if (dep != null)
            build.projectContext.add(dep)
    }
}

public interface HumanReadable {
    public val humanReadable: String
}

/**
 * The different licenses to choose from
 */
public enum class License(public override val humanReadable: String) : HumanReadable {
    NONE("None"),
    APACHE2("Apache License 2.0"),
    GPL2("GNU GPL v2.0"),
    MIT("MIT License"),
    WTFPL("WTFPL")
}
