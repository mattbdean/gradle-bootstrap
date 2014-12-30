package net.dean.gbs.test

import net.dean.gbs.api.models.Project
import net.dean.gbs.web.models.ProjectModel
import org.joda.time.DateTimeZone
import org.joda.time.DateTime
import java.util.UUID
import net.dean.gbs.api.models.TestingFramework
import net.dean.gbs.api.models.LoggingFramework
import net.dean.gbs.api.models.License
import net.dean.gbs.api.models.Language
import kotlin.platform.platformStatic
import javax.ws.rs.core.MultivaluedMap
import com.sun.jersey.core.util.MultivaluedMapImpl

public object TestUtils {
    private val name = "app"
    private val group = "com.example.app"
    private val version = "1.0"
    private val testing = TestingFramework.TESTNG
    private val logging = LoggingFramework.SLF4J
    private val license = License.APACHE
    private val languages = listOf(Language.JAVA, Language.KOTLIN)
    private val created = DateTime(2000, 1, 1, 0, 0, DateTimeZone.UTC).toDate()
    private val uuid = UUID.fromString("f3b4d46c-e691-4c6b-b7c7-feed491d0dbd")

    public platformStatic fun newProject(): Project {
        val proj = Project(name, group, version)
        proj.build.testing = testing
        proj.build.logging = logging
        proj.license = license
        for (lang in languages)
            proj.add(lang)
        return proj
    }

    public platformStatic fun newProjectModel(): ProjectModel {
        return ProjectModel.fromProject(newProject(), uuid, created, created)
    }

    public platformStatic fun ProjectModel.toMultivaluedMap(): MultivaluedMap<String, String> {
        val map = MultivaluedMapImpl()
        mapOf(
                "name" to getName(),
                "group" to getGroup(),
                "version" to getVersion(),
                "testing" to getTestingFramework(),
                "logging" to getLoggingFramework(),
                "license" to getLicense(),
                "languages" to getLanguages().map { it.name().toLowerCase() }.join(",")
        ).forEach { if (it.getValue() != null) map.add(it.getKey(), it.getValue()!!) }
        return map
    }

}
