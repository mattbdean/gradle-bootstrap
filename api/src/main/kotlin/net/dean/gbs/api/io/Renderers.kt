package net.dean.gbs.api.io

import java.nio.file.Path
import net.dean.gbs.api.models.Project
import java.util.ArrayList
import java.nio.file.Files
import net.dean.gbs.api.models.DependencyContext
import net.dean.gbs.api.models.GradleBuild
import net.dean.gbs.api.models.TestingFramework
import net.dean.gbs.api.models.License

/** Represents the files and directories created by a FileSetRenderer */
public data class RenderReport(public val files: List<Path>, public val directories: List<Path>)

/** This class provides a way to generate code based off the given object */
public trait CodeRenderer<in T> {
    /** Generates code based on a given ojbect */
    public fun render(obj: T, gen: CodeGenerator)
}

/**
 * This trait provides a standard and less error-prone method for creating blocks of code. A code block will
 * automatically be opened that has the same name as [name], and [renderBlock] will be called inside the context of that
 * block. After [renderBlock] returns, the block will be closed.
 */
public trait CodeBlockRenderer<in T> : CodeRenderer<T> {
    /** The name of the code block that will be created */
    public val name: String
    public override final fun render(obj: T, gen: CodeGenerator) {
        if (!willRender(obj)) {
            return
        }
        gen.openBlock(name)
        renderBlock(obj, gen)
        gen.closeBlock()
    }


    /** Checks if this renderer will create a code block for the given object */
    public fun willRender(obj: T): Boolean = true

    /** Renders the contents of a code block */
    protected fun renderBlock(obj: T, gen: CodeGenerator)
}

/**
 * This trait provides a standard interface for creating files and a log of files and directories that were created
 */
public trait FileSetRenderer<in T> {
    protected val gen: CodeGenerator
    public fun render(obj: T): RenderReport
}

/**
 * This class is responsible for rendering Projects.
 */
public class ProjectRenderer(private val basePath: Path) : FileSetRenderer<Project> {
    class object {
        private val dependencyContext = DependencyContextRenderer()
        private val buildscriptBlock = BuildscriptBlockRenderer(dependencyContext)
        private val plugins = PluginsRenderer()
        private val metadata = MetadataRenderer()
        private val wrapperTask = WrapperTaskRenderer()
        private val testBlock = TestBlockRenderer()
    }

    protected override val gen: CodeGenerator = CodeGenerator(basePath)

    public override fun render(obj: Project): RenderReport {
        // Create directories
        val directoryCreations: MutableList<Path> = ArrayList()
        Files.createDirectories(basePath)
        directoryCreations.add(basePath)
        for (path in obj.directoriesToCreate) {
            val p = relativePath(basePath, path)
            Files.createDirectories(p)
            directoryCreations.add(p)
        }

        ///// build.gradle /////
        gen.init("build.gradle")
        // 'buildscript' block (applies Gradle plugins among other things)
        buildscriptBlock.render(obj.build, gen)
        // Apply plugins (apply plugin: '$name')
        plugins.render(obj.build, gen)
        // Add the 'repositories' and 'dependencies' blocks
        dependencyContext.render(obj.build.projectContext, gen)
        // Metadata such as the group and version
        metadata.render(obj, gen)
        // Create a wrapper task
        wrapperTask.render(obj, gen)
        // Apply useTestNG() is using TestNG
        testBlock.render(obj.build, gen)
        // Wrap it up, we're done
        gen.close()

        ///// settings.gradle /////
        gen.init("settings.gradle")
        gen.statement("rootProject.name = '${obj.name}'")
        gen.close()

        val fileWrites: MutableList<Path> = ArrayList()

        ///// LICENSE /////
        if (obj.license != License.NONE) {
            val licenseSource = licensePath(obj.license)
            val licenseDest = relativePath(basePath, "LICENSE")
            Files.copy(licenseSource, licenseDest)
            fileWrites.add(licenseDest)
        }

        fileWrites.addAll(gen.history)

        if (obj.gitInit) {
            val repo = GitHelper.initialize(basePath)
            if (obj.gitRepo != null) {
                GitHelper.setUpstream(repo, obj.gitRepo)
            }
        }

        return RenderReport(fileWrites, directoryCreations)
    }
}

/**
 * This class is responsible for rendering a DependencyContext. This will generate two blocks of code, 'repositories'
 * and 'dependencies'. All repositories in the context will be sorted by name and then added via [Repository.method].
 * All dependencies will be sorted and then added in the Gradle shorthand format. See [Dependency.gradleFormat()] for
 * more.
 */
public class DependencyContextRenderer : CodeRenderer<DependencyContext> {
    public override fun render(obj: DependencyContext, gen: CodeGenerator) {
        if (obj.repos.isNotEmpty()) {
            gen.openBlock("repositories")
            for (repo in obj.repos.sort())
                gen.statement(repo.method)
            gen.closeBlock()
        }
        if (obj.deps.isNotEmpty()) {
            gen.openBlock("dependencies")
            for (dep in obj.deps.sort())
                gen.statement(dep.gradleFormat())
            gen.closeBlock()
        }
    }
}

/**
 * This class is responsible for rendering the 'buildscript' block.
 */
public class BuildscriptBlockRenderer(private val depContext: DependencyContextRenderer) : CodeBlockRenderer<GradleBuild> {
    override val name: String = "buildscript"

    override fun willRender(obj: GradleBuild): Boolean {
        // There has to be some content for this to render
        return obj.metaContext.deps.isNotEmpty() || obj.metaContext.repos.isNotEmpty()
    }

    protected override fun renderBlock(obj: GradleBuild, gen: CodeGenerator) {
        // Just open up the block and let DependencyContextRenderer do its thing
        depContext.render(obj.metaContext, gen)
    }
}

/**
 * This class is responsible for applying plugins. For each plugin in the GradleBuild, a line will be generated that
 * applies that plugin.
 */
public class PluginsRenderer : CodeRenderer<GradleBuild> {
    override fun render(obj: GradleBuild, gen: CodeGenerator) {
        for (p in obj.plugins) {
            gen.statement("apply plugin: '$p'")
        }
    }
}

/**
 * This class renders project metadata such as the group and version
 */
public class MetadataRenderer : CodeRenderer<Project> {
    override fun render(obj: Project, gen: CodeGenerator) {
        gen.statement("group = '${obj.group}'")
        gen.statement("version = '${obj.version}'")
    }
}

/**
 * This class generates a Wrapper task. See http://www.gradle.org/docs/current/userguide/gradle_wrapper.html
 */
public class WrapperTaskRenderer : CodeBlockRenderer<Project> {
    override val name: String = "task wrapper(type: Wrapper)"

    override fun renderBlock(obj: Project, gen: CodeGenerator) {
        gen.statement("gradleVersion = '${GradleBuild.GRADLE_WRAPPER_VERSION}'")
    }
}

/**
 * This class will add useTestNG() to the 'test' block if the GradleBuild is using TestNG
 */
public class TestBlockRenderer : CodeBlockRenderer<GradleBuild> {
    override val name: String = "test"

    override fun willRender(obj: GradleBuild): Boolean {
        return obj.testing == TestingFramework.TESTNG
    }

    override fun renderBlock(obj: GradleBuild, gen: CodeGenerator) {
        gen.statement("useTestNG()")
    }
}

