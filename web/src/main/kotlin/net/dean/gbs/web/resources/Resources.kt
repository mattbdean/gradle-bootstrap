package net.dean.gbs.web.resources

import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Consumes
import javax.ws.rs.PathParam
import net.dean.gbs.api.models.TestingFramework
import net.dean.gbs.api.models.LoggingFramework
import net.dean.gbs.api.models.Language
import net.dean.gbs.api.models.License
import javax.ws.rs.FormParam
import net.dean.gbs.api.models.Project
import java.util.UUID
import net.dean.gbs.web.db.ProjectDao
import kotlin.properties.Delegates
import net.dean.gbs.web.Parameter
import net.dean.gbs.web.ParamLocation
import net.dean.gbs.web.models.ProjectModel
import net.dean.gbs.web.GradleBootstrapConf
import net.dean.gbs.web.models.BuildStatus
import com.codahale.metrics.annotation.Timed
import net.dean.gbs.web.ProjectBuilder
import net.dean.gbs.web.db.DataAccessObject
import net.dean.gbs.web.models.Model
import javax.ws.rs.core.StreamingOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Context
import javax.ws.rs.core.UriInfo
import javax.ws.rs.QueryParam
import io.dropwizard.hibernate.UnitOfWork
import org.hibernate.context.internal.ManagedSessionContext
import javax.ws.rs.core.Response
import org.hibernate.SessionFactory

public trait Resource {
    /**
     * Throws the given lazy-evaluated RequestException when [test] is true
     */
    public fun throwWhenTrue(test: Boolean, createException: () -> RequestException) {
        if (test) throw createException()
    }

    /**
     * Throws a MissingRequiredParamException when the given parameter is null or it is a String and is empty
     */
    public fun assertPresent(param: Parameter<*>) {
        throwWhenTrue(param.value == null || (param.value is String && (param.value as String).isEmpty()),
                { MissingRequiredParamException(param) })
    }

    /**
     * Calls [assertPresent] on multiple Parameters
     */
    public fun assertPresent(param: Parameter<*>, vararg others: Parameter<*>) {
        // Test the first parameter
        assertPresent(param)

        // Test the other parameters
        for (p in others)
            assertPresent(p)
    }

    /**
     * Asserts that the given string is one of the enum values in [allValues]
     *
     * canBeNull: If true, then this method will [assertPresent] on the given parameter
     */
    public fun assertStringIsEnumValue<T : Enum<T>>(param: Parameter<String>, allValues: Array<T>) {
        for (enumValue in allValues) {
            if ((param.value).equalsIgnoreCase(enumValue.name())) {
                return
            }
        }

        throw InvalidParamException(why = "One of ${allValues.map { it.name().toLowerCase() }.toString()} (case insensitive) was not provided",
                param = param,
                errorId = ErrorCode.NOT_ENUM_VALUE
        )
    }

    /**
     * Asserts that the given parameter is:
     *
     * 1. Present (assertPresent)
     * 2. param.value is a String
     * 3. param.value is a valid UUID
     *
     * Returns a valid java.util.UUID if and only if all of the above is true.
     */
    public fun assertValidUuid(param: Parameter<String>): UUID {
        assertPresent(param)

        val uuid = param.value
        val initException = { InvalidParamException("No resource could be found by that ID", ErrorCode.MALFORMED_UUID, param) }

        try {
            // http://stackoverflow.com/a/10693997/1275092
            // fromString() does not have good validation logic
            val fromStringUUID = UUID.fromString(uuid);
            val toStringUUID = fromStringUUID.toString();
            if (!toStringUUID.equals(uuid)) throw initException()
            return fromStringUUID
        } catch (e: IllegalArgumentException) {
            throw initException()
        }
    }

    public fun assertInDatabase<T : Model>(idParam: Parameter<String>, dao: DataAccessObject<T>, humanFriendlyName: String): T {
        val uuid = assertValidUuid(idParam)
        val model = dao.get(uuid)
        throwWhenTrue(model == null, {
            NotFoundException(
                    why = "No $humanFriendlyName by that ID",
                    param = idParam
            )
        })

        return model!!
    }

    /**
     * Returns [param] if it is non-null or [alt] otherwise
     */
    public fun alternative<T>(param: T, alt: T): T = param ?: alt
}


Path("/project")
Produces(MediaType.APPLICATION_JSON)
Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class ProjectResource(public val projectDao: DataAccessObject<ProjectModel>,
                             private val builder: ProjectBuilder) : Resource {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val defaultVersion = "0.0.1"
    private val defaultTesting = TestingFramework.NONE.name()
    private val defaultLogging = LoggingFramework.NONE.name()
    private val defaultLicense = License.NONE.name()
    public val options: List<String> = listOf(
            "license",
            "language",
            "framework_testing",
            "framework_logging"
    )
    // calculate this before hand to avoid calling toString() over and over again
    public val optionsString: String by Delegates.lazy { options.toString() }

    /**
     * Creates a new project
     *
     * name: Project's name. Required.
     * group: Project's group/package. Required.
     * version: Project's version. Defaults to 0.0.1
     * testing: Testing framework. Must be one of the values in [TestingFramework].
     * logging: Logging framework. Must be one of the values in [LoggingFramework].
     * license: Project's license. Must be one of the values in [License].
     * languages: Comma-separated list of zero or more values in [Language].
     */
    POST public fun createProject(Context uriInfo: UriInfo,
                           FormParam("name") name: String?,
                           FormParam("group") group: String?,
                           FormParam("version") version: String?,
                           FormParam("testing") testing: String?,
                           FormParam("logging") logging: String?,
                           FormParam("license") license: String?,
                           FormParam("languages") languages: String?): ProjectModel {
        // name, and group, and lang are required
        assertPresent(Parameter("name", name, ParamLocation.BODY, uriInfo),
                      Parameter("group", group, ParamLocation.BODY, uriInfo),
                      Parameter("languages", languages, ParamLocation.BODY, uriInfo))

        // Make sure that each language is supported
        for (lang in languages!!.split(',')) {
            assertStringIsEnumValue(Parameter("languages", lang, ParamLocation.BODY, uriInfo), Language.values())
        }

        // testing and logging are optional, but they must be one of the values in their respective enums
        if (testing != null) assertStringIsEnumValue(Parameter("testing", testing, ParamLocation.BODY, uriInfo), TestingFramework.values())
        if (logging != null) assertStringIsEnumValue(Parameter("logging", logging, ParamLocation.BODY, uriInfo), LoggingFramework.values())
        if (license != null) assertStringIsEnumValue(Parameter("license", license, ParamLocation.BODY, uriInfo), License.values())

        // Create a project with the given name and group, using the default version if none was provided
        val proj = Project(name!!, group!!, alternative(version, defaultVersion)!!, languages.split(",").map { Language.valueOf(it.toUpperCase())} )

        // Choose an alternative before evaluating the string because enum evaluation requires a fully upper case input,
        // which must be provided by calling testing!!.toUpperCase(). If testing was null, it would throw an exception
        val effectiveTesting = alternative(testing, defaultTesting)
        proj.build.testing = TestingFramework.valueOf(effectiveTesting!!.toUpperCase())
        val effectiveLogging = alternative(logging, defaultLogging)
        proj.build.logging = LoggingFramework.valueOf(effectiveLogging!!.toUpperCase())
        val effectiveLicense = alternative(license, defaultLicense)
        proj.license = License.valueOf(effectiveLicense!!.toUpperCase())

        val createdAt = GradleBootstrapConf.getCurrentDate()
        val model = ProjectModel.fromProject(proj, createdAt, createdAt, BuildStatus.ENQUEUED)

        builder.enqueue(model)
        return model
    }

    Path("list")
    UnitOfWork(readOnly = true)
    GET public fun getAllProjects(): List<ProjectModel> {
        // TODO: Pagination
        return projectDao.getAll()
    }

    Path("{id}")
    UnitOfWork(readOnly = true)
    GET public fun getProject(Context uriInfo: UriInfo, PathParam("id") id: String): ProjectModel =
        assertInDatabase(Parameter("id", id, ParamLocation.URI, uriInfo), projectDao, "project")

    Path("{id}/download")
    Produces("application/zip")
    UnitOfWork(readOnly = true)
    GET public fun download(Context uriInfo: UriInfo, PathParam("id") id: String): Response {
        // The project ID must be present
        val projectIdParam = Parameter("id", id, ParamLocation.URI, uriInfo)
        assertPresent(projectIdParam)

        // Project ID and pass ID must be non-null, valid UUIDs, and in the database
        val project = assertInDatabase(projectIdParam, projectDao, "project")

        // Project is not ready for downloading
        throwWhenTrue(!builder.downloadAvailable(project), {
            ForbiddenException(errorId = ErrorCode.DOWNLOAD_NOT_READY,
                    why = "Download is not ready for that project (status is '${project.getStatus()}')",
                    param = projectIdParam)
        })

        // Everything checks out, download it
        val (name, streamingOutput) = builder.stream(project)
        return Response.ok(streamingOutput)
                .type("application/zip")
                .header("Content-Disposition", "attachment; filename=\"$name\"")
                .build()
    }

    Path("options")
    GET public fun listOptions() : List<String> = ProjectOption.values().map { it.name().toLowerCase() }

    Path("options/{option}")
    GET public fun getOption(Context uriInfo: UriInfo, PathParam("option") option: String): List<String> {
        assertStringIsEnumValue(Parameter("option", option, ParamLocation.URI, uriInfo), ProjectOption.values())
        return ProjectOption.valueOf(option.toUpperCase()).values
    }
}

public enum class ProjectOption(public val values: List<String>) {
    LICENSE: ProjectOption(License.values().map { it.name().toLowerCase() })
    LANGUAGE: ProjectOption(Language.values().map { it.name().toLowerCase() })
    FRAMEWORK_TESTING: ProjectOption(TestingFramework.values().map { it.name().toLowerCase() })
    FRAMEWORK_LOGGING: ProjectOption(LoggingFramework.values().map { it.name().toLowerCase() })
}

