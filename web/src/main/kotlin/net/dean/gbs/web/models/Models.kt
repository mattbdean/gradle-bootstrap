package net.dean.gbs.web.models

import com.fasterxml.jackson.annotation.JsonProperty as jsonProperty
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID
import java.util.Date
import net.dean.gbs.api.models.Language
import java.util.HashSet
import net.dean.gbs.api.models.Project
import kotlin.platform.platformStatic
import net.dean.gbs.api.models.TestingFramework
import net.dean.gbs.api.models.LoggingFramework
import net.dean.gbs.api.models.License
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

public enum class BuildStatus {
    ENQUEUED
    BUILDING
    READY
    ERRORED
}
