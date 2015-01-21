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
import net.dean.gbs.web.Parameter
import net.dean.gbs.web.ParamLocation
import net.dean.gbs.web.models.ProjectModel
import net.dean.gbs.web.GradleBootstrapConf
import net.dean.gbs.web.models.BuildStatus
import net.dean.gbs.web.ProjectBuilder
import net.dean.gbs.web.db.DataAccessObject
import net.dean.gbs.web.models.Model
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Context
import javax.ws.rs.core.UriInfo
import io.dropwizard.hibernate.UnitOfWork
import javax.ws.rs.core.Response
import net.dean.gbs.api.models.HumanReadable
import net.dean.gbs.web.models.Constraints
import java.util.regex.Pattern
import net.dean.gbs.web.models.ProjectOptionModel
import java.net.URL
import java.net.MalformedURLException

public trait ModelResource {
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
     * Returns the enum value of the given constant's name
     */
    public fun assertStringIsEnumValue<T : Enum<T>>(param: Parameter<String>, allValues: Array<T>): T {
        for (enumValue in allValues) {
            if ((param.value).equalsIgnoreCase(enumValue.name())) {
                return enumValue
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
     * 2. param.value is a valid UUID
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

    public fun assertLengthInRange(param: Parameter<String?>, min: Int, max: Int, readableName: String) {
        assertPresent(param)
        val range = min..max
        val length = param.value!!.length()
        throwWhenTrue(length !in range) {
            // First letter is capitalized, the rest are not
            val name = Character.toUpperCase(readableName[0]) + readableName.substring(1).toLowerCase()
            InvalidParamException(
                    why = "$name length must be between ${range.start} and ${range.end}",
                    errorId = ErrorCode.BAD_LENGTH,
                    param = param)
        }
    }

    public fun assertMatches(regex: Pattern, value: String, createException: () -> RequestException) {
        throwWhenTrue(!regex.matcher(value).matches(), createException)
    }

    public fun assertGitRepoUrl(upstream: Parameter<String?>): String? {
        if (upstream.value == null)
            return null
        try {
            val url = URL(upstream.value!!)
            val acceptableProtocols = array("git", "http", "https")
            if (url.getProtocol() !in acceptableProtocols) {
                throw InvalidParamException(
                        why = "Only acceptable schemes are ${acceptableProtocols.join(", ")}",
                        errorId = ErrorCode.BAD_GIT_URL,
                        param = upstream
                )
            }

            return url.toString()
        } catch (e: MalformedURLException) {
            throw InvalidParamException(
                    why = "Malformed URL",
                    errorId = ErrorCode.MALFORMED_URL,
                    param = upstream
            )
        }
    }

    /**
     * Returns true if and only if the given parameter's value is equal to "true" (case insensitive)
     */
    public fun assertBoolean(param: Parameter<String?>): Boolean {
        if (param.value == null) {
            return false
        }

        if (param.value!!.toLowerCase() == "true")
            return true
        return false
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
                             private val builder: ProjectBuilder) : ModelResource {

    private val log: Logger = LoggerFactory.getLogger(javaClass)


    /**
     * Creates a new project
     *
     * name: Project's name. Required.
     * group: Project's group/package. Required.
     * version: Project's version.
     * testing: Testing framework. Must be one of the values in [TestingFramework].
     * logging: Logging framework. Must be one of the values in [LoggingFramework].
     * license: Project's license. Must be one of the values in [License].
     * language: Comma-separated list of zero or more values in [Language].
     */
    POST public fun createProject(Context uriInfo: UriInfo,
                           FormParam("name") name: String?,
                           FormParam("group") group: String?,
                           FormParam("version") version: String?,
                           FormParam("testing") testing: String?,
                           FormParam("logging") logging: String?,
                           FormParam("license") license: String?,
                           FormParam("language") languages: String?,
                           FormParam("git_init") gitInit: String?,
                           FormParam("git_url") gitUrl: String?): ProjectModel {
        // name, and group, and lang are required
        val nameParam = Parameter("name", name, ParamLocation.BODY, uriInfo)
        val groupParam = Parameter("group", group, ParamLocation.BODY, uriInfo)
        assertPresent(nameParam, groupParam, Parameter("language", languages, ParamLocation.BODY, uriInfo))
        validateName(nameParam)
        validateGroup(groupParam)

        // Make sure that each language is supported
        for (lang in languages!!.split(',')) {
            assertStringIsEnumValue(Parameter("languages", lang, ParamLocation.BODY, uriInfo), Language.values())
        }

        // testing and logging are optional, but they must be one of the values in their respective enums
        if (testing != null) assertStringIsEnumValue(Parameter("testing", testing, ParamLocation.BODY, uriInfo), TestingFramework.values())
        if (logging != null) assertStringIsEnumValue(Parameter("logging", logging, ParamLocation.BODY, uriInfo), LoggingFramework.values())
        if (license != null) assertStringIsEnumValue(Parameter("license", license, ParamLocation.BODY, uriInfo), License.values())

        // Use the default version if one is not provided
        val effectiveVersion = alternative(version, ProjectModel.DEFAULT_VERSION)!!
        validateVersion(Parameter("version", effectiveVersion, ParamLocation.BODY, uriInfo))

        val gitUrlParam = Parameter("git_url", gitUrl, ParamLocation.BODY, uriInfo)
        val gitUpstream = assertGitRepoUrl(gitUrlParam)

        // Initialize the git repo if git_url was specified OR git_init was equal to "true"
        val gitInitParam = Parameter("git_init", gitInit, ParamLocation.BODY, uriInfo)
        val gitInitBool: Boolean = gitUpstream != null || assertBoolean(gitInitParam)

        // Create a project
        val proj = Project(name!!, group!!, effectiveVersion, gitUpstream, gitInitBool, languages.split(",").map { Language.valueOf(it.toUpperCase())} )

        // Choose an alternative before evaluating the string because enum evaluation requires a fully upper case input,
        // which must be provided by calling testing!!.toUpperCase(). If testing was null, it would throw an exception
        val effectiveTesting = alternative(testing, ProjectOption.TESTING.default.toString())
        proj.build.testing = TestingFramework.valueOf(effectiveTesting!!.toUpperCase())
        val effectiveLogging = alternative(logging, ProjectOption.LOGGING.default.toString())
        proj.build.logging = LoggingFramework.valueOf(effectiveLogging!!.toUpperCase())
        val effectiveLicense = alternative(license, ProjectOption.LICENSE.default.toString())
        proj.license = License.valueOf(effectiveLicense!!.toUpperCase())

        val createdAt = GradleBootstrapConf.getCurrentDate()
        val model = ProjectModel.fromProject(proj, createdAt, createdAt, BuildStatus.ENQUEUED)

        builder.enqueue(model)
        return model
    }

    private fun validateName(name: Parameter<String?>) {
        assertLengthInRange(name, Constraints.NAME_MIN_LENGTH, Constraints.NAME_MAX_LENGTH, "name")
    }

    private fun validateGroup(group: Parameter<String?>) {
        assertPresent(group)
        assertLengthInRange(group, Constraints.GROUP_MIN_LENGTH, Constraints.GROUP_MAX_LENGTH, "group")
        val parts = group.value!!.split("\\.")

        // Make sure each part is under the limit for directory names
        for (part in parts) {
            assertLengthInRange(Parameter(group.name, part, group.location, group.uriInfo),
                    Constraints.DIR_MIN_LENGTH, Constraints.DIR_MAX_LENGTH, "group")
        }

        // Make sure the group is a valid Java identifier
        assertMatches(Constraints.GROUP_PATTERN, group.value!!) {
            InvalidParamException(
                    why = "That is not a valid Java identifier",
                    errorId = ErrorCode.INVALID_IDENTIFIER,
                    param = group
            )
        }
    }


    private fun validateVersion(ver: Parameter<String?>) {
        assertLengthInRange(ver, Constraints.VERSION_MIN_LENGTH, Constraints.VERSION_MAX_LENGTH, "version")
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
    GET public fun getOptions(): ProjectOptionModel = ProjectOptionModel.INSTANCE
}

public enum class ProjectOption(public val values: Array<out HumanReadable>, public val default: HumanReadable) {
    LICENSE: ProjectOption(
            values = License.values(),
            default = License.NONE)
    LANGUAGE: ProjectOption(
            values = Language.values(),
            default = Language.JAVA)
    TESTING : ProjectOption(
            values = TestingFramework.values(),
            default = TestingFramework.NONE)
    LOGGING : ProjectOption(
            values = LoggingFramework.values(),
            default = LoggingFramework.NONE)
}

