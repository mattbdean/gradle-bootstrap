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
import java.util.Date
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

public trait Resource<M : Model<*>> {
    /**
     * Throws the given lazy-evaluated RequestException when [test] is true
     */
    public fun throwWhenTrue(test: Boolean, createException: () -> RequestException) {
        if (test) throw createException()
    }

    /**
     * Throws a MissingRequiredParamException when the given parameter is null or it is a String and is emtpy
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

    /**
     * Returns [param] if it is non-null or [alt] otherwise
     */
    public fun alternative<T>(param: T, alt: T): T = param ?: alt
}


/** Provides an endpoint to create a project */
Path("/project")
Produces(MediaType.APPLICATION_JSON)
Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class ProjectResource(private val dao: ProjectDao, private val builder: ProjectBuilder) : Resource<ProjectModel> {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    public fun get(param: Parameter<UUID>, dao: ProjectDao): ProjectModel {
        val dbLookup = dao.get(param.value)
        throwWhenTrue(dbLookup == null,
                { InvalidParamException("No model by that ID", ErrorCode.NOT_FOUND, param) })
        return dbLookup!!
    }
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
    Timed
    POST public fun create(Context uriInfo: UriInfo,
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
                      Parameter("languages", name, ParamLocation.BODY, uriInfo))

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
        val model = ProjectModel.fromProject(proj, UUID.randomUUID(), createdAt, createdAt, BuildStatus.ENQUEUED)
        dao.insert(model)
        builder.enqueue(model)
        return model
    }

    Path("list")
    GET public fun getAll(): Iterator<ProjectModel> {
        // TODO: Pagination
        return dao.getAll()
    }

    Path("{id}")
    GET public fun getById(Context uriInfo: UriInfo, PathParam("id") id: String): ProjectModel {
        val param = Parameter("id", id, ParamLocation.URI, uriInfo)
        val uuidParam = Parameter("id", assertValidUuid(param), ParamLocation.URI, uriInfo)
        return get(uuidParam, dao)
    }

    Path("{id}/download")
    Produces("application/zip")
    GET public fun download(Context uriInfo: UriInfo, PathParam("id") id: String): StreamingOutput {
        log.info("Request to download project with id of $id")
        val param = Parameter("id", id, ParamLocation.URI, uriInfo)
        val uuid = assertValidUuid(param)
        val project = dao.get(uuid)
        throwWhenTrue(project == null, { NotFoundException("No project by that ID", ErrorCode.NOT_FOUND, param) })
        throwWhenTrue(!builder.downloadAvailable(project!!), {
            RequestException(code = ErrorCode.DOWNLOAD_NOT_READY,
                    why = "Download is not ready for that project (status is '${project.getStatus()}')",
                    status = 403,
                    param = param)
        })
        return builder.download(project)
    }

    Path("options")
    GET public fun listOptions() : List<String> = ProjectOption.values().map { it.name().toLowerCase() }

    Path("options/{option}")
    GET public fun fetch(Context uriInfo: UriInfo, PathParam("option") option: String): List<String> {
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

