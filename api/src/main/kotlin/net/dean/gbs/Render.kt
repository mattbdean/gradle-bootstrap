package net.dean.gbs

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.util.ArrayList
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This trait forms the basis for rendering abstract objects into one or more files
 */
public trait Renderer<in T> {
    public val charset: Charset
    public fun render(obj: T): Map<String, List<String>>
    protected val gen: CodeGenerator
}

public abstract class AbstractRenderer<in T> : Renderer<T> {
    public override var charset: Charset = StandardCharsets.UTF_8
    protected override val gen: CodeGenerator = CodeGenerator()
}

public class ProjectRenderer : AbstractRenderer<Project>() {
    override fun render(obj: Project): Map<String, List<String>> {

        val buildGradle: MutableList<String> = ArrayList()
        buildGradle.addAll(renderBuildscript(obj.build))
        buildGradle.addAll(obj.build.plugins.map { "apply plugin: '$it'" })
        buildGradle.addAll(renderDependencyContext(obj.build.projectContext))
        buildGradle.add("group = '${obj.group}'")
        buildGradle.add("version = '${obj.version}'")
        buildGradle.addAll(renderWrapperTask())
        buildGradle.addAll(renderTestBlock(obj.build.testing))

        val settingsGradle = listOf("rootProject.name = '${obj.name}'")
        // TODO: It might be more efficient to just copy the license instead of storing it in memory for some time and
        // then just regurgitating it after some time
        val license = Files.readAllLines(
                Paths.get(javaClass<ProjectRenderer>().getResource("/licenses/${obj.license}.txt").toURI()),
                charset)
        return mapOf(
                "build.gradle" to buildGradle,
                "settings.gradle" to settingsGradle,
                "LICENSE" to license
        )
    }

    private fun renderBuildscript(build: GradleBuild): List<String> {
        val context = build.metaContext
        if (context.deps.isEmpty() && context.repos.isEmpty())
        // Nothing to do
            return listOf()

        val list: MutableList<String> = ArrayList()
        list.add(gen.open("buildscript"))
        list.addAll(renderDependencyContext(context))
        list.add(gen.close())
        return list
    }

    private fun renderWrapperTask(): List<String> {
        return listOf(
                gen.open("task wrapper(type: Wrapper)"),
                gen.statement("gradleVersion = '${GradleBuild.LATEST_GRADLE_VERSION}'"),
                gen.close()
        )
    }

    private fun renderTestBlock(testing: TestingFramework): List<String> {
        if (testing != TestingFramework.TESTNG) {
            return listOf()
        }

        return listOf(
                gen.open("test"),
                gen.statement("useTestNG()"),
                gen.close()
        )
    }

    private fun renderDependencyContext(context: DependencyContext): List<String> {
        val list: MutableList<String> = ArrayList()
        with(list) {
            if (context.repos.isNotEmpty()) {
                add(gen.open("repositories"))
                for (repo in context.repos.sort())
                    add(gen.statement(repo.method))
                add(gen.close())
            }
            if (context.deps.isNotEmpty()) {
                add(gen.open("dependencies"))
                for (dep in context.deps.sort())
                    add(gen.statement(dep.gradleFormat()))
                add(gen.close())
            }
        }.toString()
        return list
    }
}

private class CodeGenerator {
    private val indents: MutableMap<Int, String> = HashMap()
    private var currentIndent = 0
        set(value) {
            if (value < 0) {
                return
            }
            if (!indents.containsKey(value)) {
                val sb = StringBuilder()
                var temp = 0
                while (temp < value) {
                    sb.append(indent)
                    temp++
                }

                indents.put(value, sb.toString())
            }
            $currentIndent = value
        }
    private val indent = "    "

    {
        currentIndent = 0
    }

    public fun open(blockName: String): String {
        val block = statement("$blockName {")
        currentIndent++
        return block
    }

    public fun close(): String {
        currentIndent--
        return statement("}")
    }

    public fun statement(code: String): String {
        return "${indents.get(currentIndent)}$code"
    }
}
