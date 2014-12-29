package net.dean.gbs.web.resources

import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Consumes
import javax.ws.rs.PathParam
import net.dean.gbs.api.TestingFramework
import net.dean.gbs.api.LoggingFramework
import net.dean.gbs.api.Language
import net.dean.gbs.api.License
import javax.ws.rs.FormParam
import net.dean.gbs.api.Project
import java.util.UUID
import java.util.Date
import net.dean.gbs.web.db.ProjectDao
import kotlin.properties.Delegates
import net.dean.gbs.web.Parameter
import net.dean.gbs.web.ParamLocation
import net.dean.gbs.web.models.ProjectModel
import net.dean.gbs.web.GradleBootstrapConf

public trait Resource {
    /**
     * Throws the given lazy-evaluated RequestException when [test] is true
     */
    public fun throwWhenTrue(test: Boolean, createException: () -> RequestException) {
        if (test) throw createException()
    }

    /**
     * Throws a MissingRequiredParamException when the given parameter is null or it is a String and is emtpy
     */
    public fun assertPresent(param: Parameter) {
        throwWhenTrue(param.value == null || (param.value is String && (param.value as String).isEmpty()),
                { MissingRequiredParamException(javaClass, param) })
    }

    /**
     * Calls [assertPresent] on multiple Parameters
     */
    public fun assertPresent(param: Parameter, vararg others: Parameter) {
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
    public fun assertStringIsEnumValue<T : Enum<T>>(param: Parameter, allValues: Array<T>, canBeNull: Boolean = false) {
        if (!canBeNull && param.value == null)
            assertPresent(param)

        // Make sure the parameter's value was actually a string
        if (param.value !is String)
            throw IllegalArgumentException("Parameter value is not java.lang.String (was ${param.value!!.javaClass.getName()})")

        for (enumValue in allValues) {
            if ((param.value!! as String).equalsIgnoreCase(enumValue.name())) {
                return
            }
        }

        throw InvalidParamException(resourceClass = javaClass<ProjectOptionsResource>(),
                why = "One of ${allValues.map { it.name().toLowerCase() }.toString()} (case insensitive) was not provided",
                param = param
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
    public fun assertValidUuid(param: Parameter): UUID {
        assertPresent(param)

        val uuid = if (param.value is String) param.value!! as String else throw IllegalArgumentException("param.value must be a String")
        val initException = { InvalidParamException(javaClass, "Invalid ID", param) }

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

/**
 * Provides an abstraction for resources that interact with a ProjectDao
 */
public abstract class ProjectResource(protected val dao: ProjectDao) : Resource

/** Provides an endpoint to create a project */
Path("/project")
Produces(MediaType.APPLICATION_JSON)
Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class ProjectCreationResource(dao: ProjectDao) : ProjectResource(dao) {
    private val defaultVersion = "0.0.1"
    private val defaultTesting = TestingFramework.NONE.name()
    private val defaultLogging = LoggingFramework.NONE.name()
    private val defaultLicense = License.NONE.name()

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
    POST public fun create(FormParam("name") name: String?,
                           FormParam("group") group: String?,
                           FormParam("version") version: String?,
                           FormParam("testing") testing: String?,
                           FormParam("logging") logging: String?,
                           FormParam("license") license: String?,
                           FormParam("languages") languages: String?): ProjectModel {
        // name and group are required
        assertPresent(Parameter("name", name, ParamLocation.BODY),
                      Parameter("group", group, ParamLocation.BODY))

        // testing and logging are optional, but they must be one of the values in their respective enums

        if (testing != null) assertStringIsEnumValue(Parameter("testing", testing, ParamLocation.BODY), TestingFramework.values())
        if (logging != null) assertStringIsEnumValue(Parameter("logging", logging, ParamLocation.BODY), LoggingFramework.values())
        if (license != null) assertStringIsEnumValue(Parameter("license", license, ParamLocation.BODY), License.values())
        if (languages != null) {
            for (lang in languages.split(',')) {
                assertStringIsEnumValue(Parameter("languages", lang, ParamLocation.BODY), Language.values())
            }
        }

        // Create a project with the given name and group, using the default version if none was provided
        val proj = Project(name!!, group!!, alternative(version, defaultVersion)!!)

        // Choose an alternative before evaluating the string because enum evaluation requires a fully upper case input,
        // which must be provided by calling testing!!.toUpperCase(). If testing was null, it would throw an exception
        val effectiveTesting = alternative(testing, defaultTesting)
        proj.build.testing = TestingFramework.valueOf(effectiveTesting!!.toUpperCase())
        val effectiveLogging = alternative(logging, defaultLogging)
        proj.build.logging = LoggingFramework.valueOf(effectiveLogging!!.toUpperCase())
        val effectiveLicense = alternative(license, defaultLicense)
        proj.license = License.valueOf(effectiveLicense!!.toUpperCase())

        if (languages != null && languages.trim().length() > 0) {
            for (lang in languages.split(',')) {
                proj.add(Language.valueOf(lang.toUpperCase()))
            }
        }

        val createdAt = GradleBootstrapConf.getCurrentDate()
        val model = ProjectModel.fromProject(proj, UUID.randomUUID(), createdAt, createdAt)
        dao.insert(model)
        return model
    }
}

/**
 * Provides an endpoint to list all projects
 */
Path("/projects")
Produces(MediaType.APPLICATION_JSON)
public class ProjectBulkLookupResource(dao: ProjectDao) : ProjectResource(dao) {
    GET public fun fetch(): Iterator<ProjectModel> {
        return dao.getAll()
    }
}

/**
 * Provides an endpoint to find projects by ID
 */
Path("/project/{id}")
Produces(MediaType.APPLICATION_JSON)
Consumes(MediaType.APPLICATION_JSON)
public class ProjectLookupResource(dao: ProjectDao) : ProjectResource(dao) {
    GET public fun find(PathParam("id") id: String): ProjectModel {
        // TODO: Filter input
        val uuid = assertValidUuid(Parameter("id", id, ParamLocation.URI))
        return dao.get(uuid)
    }
}

/**
 * Provides an endpoint to find possible project options
 */
Produces(MediaType.APPLICATION_JSON)
Path("/project/options/{option}")
public class ProjectOptionsResource : Resource {
    public val options: List<String> = listOf(
            "license",
            "language",
            "framework_testing",
            "framework_logging"
    )
    // calculate this before hand to avoid calling toString() over and over again
    public val optionsString: String by Delegates.lazy { options.toString() }

    GET public fun fetch(PathParam("option") option: String): List<String> {
        assertStringIsEnumValue(Parameter("option", option, ParamLocation.URI), ProjectOption.values())
        return ProjectOption.valueOf(option.toUpperCase()).values
    }
}

public enum class ProjectOption(public val values: List<String>) {
    LICENSE: ProjectOption(License.values().map { it.name().toLowerCase() })
    LANGUAGE: ProjectOption(Language.values().map { it.name().toLowerCase() })
    FRAMEWORK_TESTING: ProjectOption(TestingFramework.values().map { it.name().toLowerCase() })
    FRAMEWORK_LOGGING: ProjectOption(LoggingFramework.values().map { it.name().toLowerCase() })
}

