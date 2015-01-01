package net.dean.gbs.web.models

import com.fasterxml.jackson.annotation.JsonProperty as jsonProperty
import java.util.UUID
import net.dean.gbs.api.models.Project
import org.joda.time.DateTime

public trait Model<T> {
    public var id: UUID?
    public var createdAt: DateTime?
    public var updatedAt: DateTime?
}

public fun fromProject(proj: Project, id: UUID, createdAt: DateTime, updatedAt: DateTime): ProjectModel {
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
