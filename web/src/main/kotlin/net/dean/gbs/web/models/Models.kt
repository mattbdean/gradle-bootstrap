package net.dean.gbs.web.models

import com.fasterxml.jackson.annotation.JsonProperty as jsonProperty
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Indicates the build status of a project
 */
public data class ProjectStatus [JsonCreator](JsonProperty("status") public val status: BuildStatus)

public enum class BuildStatus {
    ENQUEUED
    BUILDING
    READY
}
