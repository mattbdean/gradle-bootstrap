package net.dean.gbs.web.models

import com.fasterxml.jackson.annotation.JsonProperty as jsonProperty
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID
import java.util.Date
import net.dean.gbs.api.Language
import java.util.HashSet
import net.dean.gbs.api.Project
import kotlin.platform.platformStatic
import net.dean.gbs.api.TestingFramework
import net.dean.gbs.api.LoggingFramework
import net.dean.gbs.api.License
import javax.ws.rs.core.MultivaluedMap
import com.sun.jersey.core.util.MultivaluedMapImpl

public trait Model<T> {
    public var id: UUID?
    public var createdAt: Date?
    public var updatedAt: Date?
}

        public fun fromProject(proj: Project, id: UUID, createdAt: Date, updatedAt: Date): ProjectModel {
            val model = ProjectModel()
            model.id = id
            model.createdAt = createdAt
            model.updatedAt = updatedAt
            model.name = proj.name
            model.group = proj.group
            model.version = proj.version
            model.testingFramework = proj.build.testing.name().toLowerCase()
            model.loggingFramework = proj.build.logging.name().toLowerCase()
            model.license = proj.license.name().toLowerCase()
            model.languages = proj.languages
            return model
        }

    public fun ProjectModel.toProject(): Project {
        val proj = Project(name!!, group!!, version!!)
        if (testingFramework != null) proj.build.testing = TestingFramework.valueOf(testingFramework!!.toUpperCase())
        if (loggingFramework != null) proj.build.logging = LoggingFramework.valueOf(loggingFramework!!.toUpperCase())
        if (license != null) proj.license = License.valueOf(license!!.toUpperCase())
        for (lang in languages) {
            proj.add(lang)
        }
        return proj
    }


/**
 * Indicates the build status of a project
 */
public data class ProjectStatus [JsonCreator](JsonProperty("status") public val status: BuildStatus)

public enum class BuildStatus {
    ENQUEUED
    BUILDING
    READY
    ERRORED
}
