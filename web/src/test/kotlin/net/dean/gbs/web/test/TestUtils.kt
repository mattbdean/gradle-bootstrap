package net.dean.gbs.web.test

import javax.ws.rs.core.MultivaluedMap
import net.dean.gbs.web.models.ProjectModel
import kotlin.platform.platformStatic
import net.dean.gbs.api.models.Project
import net.dean.gbs.web.models.BuildStatus
import java.util.UUID
import org.joda.time.DateTime
import net.dean.gbs.api.models.Language
import net.dean.gbs.api.models.License
import net.dean.gbs.api.models.LoggingFramework
import net.dean.gbs.api.models.TestingFramework
import net.dean.gbs.web.GradleBootstrapConf
import java.nio.file.Files
import java.nio.file.Path
import java.io.File
import com.google.common.io.Resources
import javax.ws.rs.core.MultivaluedHashMap

public object TestUtils {
    private val name = "app"
    private val group = "com.example.app"
    private val version = "1.0"
    private val testing = TestingFramework.TESTNG
    private val logging = LoggingFramework.SLF4J
    private val license = License.APACHE
    private val languages = listOf(Language.JAVA, Language.KOTLIN)
    private val created = DateTime(2000, 1, 1, 0, 0, GradleBootstrapConf.timeZone)
    private val uuid = UUID.fromString("f3b4d46c-e691-4c6b-b7c7-feed491d0dbd")

    public platformStatic fun newProject(): Project {
        val proj = Project(name, group, version, languages)
        proj.build.testing = testing
        proj.build.logging = logging
        proj.license = license
        return proj
    }

    public platformStatic fun newProjectModel(): ProjectModel {
        val proj = ProjectModel.fromProject(newProject(), created, created, BuildStatus.ENQUEUED)
        proj.setId(uuid)
        return proj
    }

    public platformStatic fun toMultivaluedMap(model: ProjectModel): MultivaluedMap<String, String> {
        val map = MultivaluedHashMap<String, String>()
        mapOf(
                "name" to model.getName(),
                "group" to model.getGroup(),
                "version" to model.getVersion(),
                "testing" to model.getTestingFramework(),
                "logging" to model.getLoggingFramework(),
                "license" to model.getLicense(),
                "languages" to model.getLanguages().join(",")
        ).forEach { if (it.getValue() != null) map.add(it.getKey(), it.getValue()!!) }
        return map
    }

    public platformStatic fun createTempDir(): Path = Files.createTempDirectory("gradle-bootstrap")

    public platformStatic fun getResource(name: String): String = File(Resources.getResource(name).toURI()).getAbsolutePath()
}
