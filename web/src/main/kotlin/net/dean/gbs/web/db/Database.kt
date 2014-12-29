package net.dean.gbs.web.db

import java.sql.ResultSet
import org.skife.jdbi.v2.StatementContext
import java.util.Date
import net.dean.gbs.api.Language
import net.dean.gbs.api.TestingFramework
import net.dean.gbs.api.LoggingFramework
import org.skife.jdbi.v2.sqlobject.Binder
import org.skife.jdbi.v2.SQLStatement
import net.dean.gbs.web.ProjectModel
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.tweak.ResultSetMapper
import net.dean.gbs.api.Project
import net.dean.gbs.api.License
import org.skife.jdbi.v2.sqlobject.BinderFactory
import java.util.UUID

val tableName = "projects"

private object Col {
    val id = "id"
    val name = "name"
    val group = "group_name"
    val version = "version"
    val created = "created_at"
    val updated = "updated_at"
    val testing = "testing_framework"
    val logging = "logging_framework"
    val license = "license"
    val languages = "languages" // Stored as comma separated values
}

/**
 * This class provides a way to map a ResultSet to a ProjectModel
 */
public class ProjectModelMapper : ResultSetMapper<net.dean.gbs.web.ProjectModel> {
    override fun map(index: Int, r: ResultSet, ctx: StatementContext?): ProjectModel {
        // Use the Project class and ProjectModel.fromProject as a shortcut. Can be modified to use only a ProjectModel
        // later if necessary
        val proj = Project(r.getString(Col.name), r.getString(Col.group))
        proj.license = License.valueOf(r.getString(Col.license).toUpperCase())
        proj.build.logging = LoggingFramework.valueOf(r.getString(Col.logging).toUpperCase())
        proj.build.testing = TestingFramework.valueOf(r.getString(Col.testing).toUpperCase())

        for (lang in r.getString(Col.languages).split(",")) {
            // If there are no languages, the first and only index will be an empty string
            if (lang.isEmpty()) continue
            proj.add(Language.valueOf(lang.toUpperCase()))
        }

        return ProjectModel.fromProject(proj,
                r.getObject("id") as UUID,
                Date(r.getLong(Col.created)),
                Date(r.getLong(Col.updated)))
    }
}

// Use the ProjectModelBinderFactory class to bind instances of ProjectModels to a SQL query
/**
 * This class provides a way to notate that this ProjectModel parameter will be bound to the SQLStatement using the
 * [ProjectModelBinderFactory] class.
 */
org.skife.jdbi.v2.sqlobject.BindingAnnotation(javaClass<ProjectModelBinderFactory>())
java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)
public annotation class BindProjectModel(/*public val exceptions: Array<String> = array()*/)

/**
 * This class provides a way to bind a ProjectModel's values to a SQLStatement
 */
public class ProjectModelBinderFactory : BinderFactory {
    override fun build(annotation: Annotation): Binder<out Annotation, out Any> {
        return object: Binder<BindProjectModel, ProjectModel> {
            override fun bind(q: SQLStatement<*>, bind: BindProjectModel, arg: ProjectModel) {
                // Bind SQL query parameters to ProjectModel properties
                val bindings = mapOf(
                        Col.id to arg.getId(),
                        Col.created to arg.getCreatedAt().getTime(),
                        Col.updated to arg.getUpdatedAt().getTime(),
                        Col.name to arg.getName(),
                        Col.group to arg.getGroup(),
                        Col.version to arg.getVersion(),
                        Col.testing to arg.getTestingFramework(),
                        Col.logging to arg.getLoggingFramework(),
                        Col.license to arg.getLicense(),
                        Col.languages to arg.getLanguages().join(",")
                )

                for ((key, value) in bindings)
//                    if (key !in bind.exceptions)
                        q.bind(key, value)
            }
        }
    }
}

/**
 * This trait provides the ability to perform CR(UD) operations on ProjectModels.
 */
RegisterMapper(javaClass<ProjectModelMapper>())
public trait ProjectDao {

    /**
     * Creates a table whose name is [tableName] if it does already exist.
     */
    SqlUpdate(
"""CREATE TABLE IF NOT EXISTS $tableName (
    ${Col.id} UUID NOT NULL,
    ${Col.created} BIGINT NOT NULL,
    ${Col.updated} BIGINT NOT NULL,
    ${Col.name} VARCHAR NOT NULL,
    ${Col.group} VARCHAR NOT NULL,
    ${Col.version} VARCHAR NOT NULL,
    ${Col.testing} VARCHAR NOT NULL,
    ${Col.logging} VARCHAR NOT NULL,
    ${Col.license} VARCHAR NOT NULL,
    ${Col.languages} VARCHAR NOT NULL,

    PRIMARY KEY (${Col.id})
)
"""
    )
    fun createTable()

    /**
     * Inserts a ProjectModel into the table
     */
    SqlUpdate(
"""INSERT INTO $tableName (
        ${Col.name},${Col.group},${Col.version},${Col.testing},${Col.logging},${Col.license},${Col.languages},${Col.id},
        ${Col.created},${Col.updated}
    )
    VALUES (
        :${Col.name},:${Col.group},:${Col.version},:${Col.testing},:${Col.logging},:${Col.license},:${Col.languages},
        :${Col.id},:${Col.created},:${Col.updated}
    )"""
    )
    fun insert(BindProjectModel project: ProjectModel)

    /**
     * Retrieves a ProjectModel by its ID
     */
    SqlQuery("SELECT * FROM $tableName WHERE ${Col.id} = :${Col.id}")
    fun get(Bind(Col.id) id: String): ProjectModel

    /**
     * Retrieves a list of ProjectModels ordered by group
     */
    SqlQuery("SELECT * FROM $tableName ORDER BY ${Col.group} DESC")
    fun getOrdered(): Iterator<ProjectModel>
}

