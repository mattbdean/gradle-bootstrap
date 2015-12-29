package net.dean.gbs.web.test

import com.google.common.io.Resources
import net.dean.gbs.api.models.*
import net.dean.gbs.web.GradleBootstrapConf
import net.dean.gbs.web.models.BuildStatus
import net.dean.gbs.web.models.ProjectModel
import org.joda.time.DateTime
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import kotlin.collections.forEach
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.mapOf

public object TestUtils {
    private val name = "app"
    private val group = "com.example.app"
    private val version = "1.0"
    private val testing = TestingFramework.TESTNG
    private val logging = LoggingFramework.SLF4J
    private val license = License.APACHE2
    private val languages = listOf(Language.JAVA, Language.KOTLIN)
    private val created = DateTime(2000, 1, 1, 0, 0, GradleBootstrapConf.timeZone)
    private val uuid = UUID.fromString("f3b4d46c-e691-4c6b-b7c7-feed491d0dbd")
    private val gitUrl: String = "https://github.com/example/example"

    public @JvmStatic fun newProject(): Project {
        val proj = Project(name, group, version, gitUrl, true, languages)
        proj.build.testing = testing
        proj.build.logging = logging
        proj.license = license
        return proj
    }

    public @JvmStatic fun newProjectModel(): ProjectModel {
        val proj = ProjectModel.fromProject(newProject(), created, created, BuildStatus.ENQUEUED)
        proj.id = uuid
        return proj
    }

    public @JvmStatic fun toMultivaluedMap(model: ProjectModel): MultivaluedMap<String, String> {
        val map = MultivaluedHashMap<String, String>()
        mapOf(
                "name" to model.name,
                "group" to model.group,
                "version" to model.version,
                "testing" to model.testingFramework,
                "logging" to model.loggingFramework,
                "license" to model.license,
                "languages" to model.languages.joinToString(",")
        ).forEach { if (it.value != null) map.add(it.key, it.value!!) }
        return map
    }

    public @JvmStatic fun createTempDir(): Path = Files.createTempDirectory("gradle-bootstrap")

    public @JvmStatic fun getResource(name: String): String = File(Resources.getResource(name).toURI()).absolutePath
}
