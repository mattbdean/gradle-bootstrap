package net.dean.gbs.web.test

import net.dean.gbs.web.models.ProjectModel
import net.dean.gbs.api.models.TestingFramework
import net.dean.gbs.api.models.LoggingFramework
import net.dean.gbs.api.models.Language
import net.dean.gbs.api.models.License
import java.nio.file.Path
import java.util.UUID
import net.dean.gbs.web.models.BuildStatus
import net.dean.gbs.web.models.Model
import javax.ws.rs.core.MediaType
import java.nio.file.Files
import net.dean.gbs.api.io.relativePath
import javax.ws.rs.client.Client
import java.io.InputStream
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.ext.MessageBodyReader
import java.lang.reflect.Type
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.Produces
import javax.ws.rs.ext.Provider
import org.glassfish.jersey.filter.LoggingFilter
import org.slf4j.LoggerFactory

public trait GbsApi {
    /*
    1. Create project
    2. Wait for it to build
    3. Download the project
     */
    public fun createProject(name: String,
                             group: String,
                             version: String,
                             testing: TestingFramework,
                             logging: LoggingFramework,
                             license: License,
                             languages: Set<Language>): ProjectModel

    public fun isDownloadReady(id: UUID): Boolean

    public fun download(project: ProjectModel, directory: Path): Path
}

public class GbsApiImpl(baseUrl: String, private val client: Client) : GbsApi {
    private val projectClass = javaClass<ProjectModel>()
    private val baseUrl: String
    private val target: WebTarget

    {
        // Remove trailing slash
        this.baseUrl = if (!baseUrl.endsWith("/")) baseUrl else baseUrl.substring(0, baseUrl.length() - 1)
        this.target = client.target(baseUrl)
        target.register(LoggingFilter())
    }

    override fun createProject(name: String, group: String, version: String, testing: TestingFramework, logging: LoggingFramework, license: License, languages: Set<Language>): ProjectModel {
        return post("/project", projectClass,
                "name" to name,
                "group" to group,
                "version" to version,
                "testing" to testing,
                "logging" to logging,
                "license" to license,
                "language" to languages.map { it.name().toLowerCase() }.join(",")
        )
    }

    override fun isDownloadReady(id: UUID): Boolean {
        val project = get("/project/$id", projectClass)
        return project.getStatus().toUpperCase() == BuildStatus.READY.name()
    }

    override fun download(project: ProjectModel, directory: Path): Path {
        return client.target(baseUrl)
                .path("project/${project.getId()}/download")
                .request()
                        .accept("application/zip")
                        .get(javaClass<Path>())
    }

    private fun post<T : Model>(path: String, modelClass: Class<T>, vararg urlEncodedFormParams: Pair<String, Any>): T {
        val map = MultivaluedHashMap<String, String>()
        for ((key, value) in urlEncodedFormParams)
            map.add(key, value.toString())
        return client.target(baseUrl)
                .path(path)
                .request(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(map, MediaType.APPLICATION_FORM_URLENCODED), modelClass)
    }

    private fun get<T : Model>(path: String, modelClass: Class<T>): T {
        return client.target(baseUrl)
                .path(path)
                .request(MediaType.APPLICATION_JSON)
                        .get(modelClass)
    }
}

Provider
Produces("application/zip")
class ZipBodyReader(private val directory: Path) : MessageBodyReader<Path> {
    override fun isReadable(type: Class<out Any?>?, genericType: Type?, annotations: Array<out Annotation>?, mediaType: MediaType?): Boolean {
        return type == javaClass<Path>()
    }

    override fun readFrom(type: Class<Path>?, genericType: Type, annotations: Array<out Annotation>, mediaType: MediaType, httpHeaders: MultivaluedMap<String, String>, entityStream: InputStream): Path {
        if (!Files.isDirectory(directory))
            Files.createDirectories(directory)

        val cdHeader = "Content-Disposition"
        if (cdHeader !in httpHeaders)
            throw IllegalStateException("Expecting Content-Disposition header")

        val file = relativePath(directory, "project.zip")
        if (Files.exists(file)) {
            Files.delete(file);
        }
        Files.copy(entityStream, file)
        return file
    }

}
