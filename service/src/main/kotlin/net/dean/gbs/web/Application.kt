package net.dean.gbs.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.hibernate.HibernateBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import net.dean.gbs.web.db.DataAccessObject
import net.dean.gbs.web.db.ProjectDao
import net.dean.gbs.web.models.GitProperties
import net.dean.gbs.web.models.ProjectModel
import net.dean.gbs.web.resources.ProjectResource
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpUtils
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import kotlin.collections.forEach
import com.fasterxml.jackson.annotation.JsonProperty as jsonProperty
import org.hibernate.validator.constraints.NotEmpty as notEmpty
import javax.validation.Valid as valid
import javax.validation.constraints.NotNull as notNull
import javax.ws.rs.core.Context as context
import javax.ws.rs.ext.Provider as provider

public fun main(args: Array<String>) {
    GradleBootstrap().run(*args)
}

public class GradleBootstrap : Application<GradleBootstrapConf>() {
    val hibernate = object: HibernateBundle<GradleBootstrapConf>(ProjectModel::class.java, GitProperties::class.java) {
        override fun getDataSourceFactory(configuration: GradleBootstrapConf?): DataSourceFactory? {
            return configuration!!.database
        }
    }

    override fun initialize(bootstrap: Bootstrap<GradleBootstrapConf>) {
        bootstrap.addBundle(AssetsBundle("/assets", "/", "index.html"))
        bootstrap.addBundle(hibernate)
    }

    override fun run(configuration: GradleBootstrapConf, environment: Environment) {
        // Configure the object mapper
        GradleBootstrapConf.configureObjectMapper(environment.objectMapper)

        // Initialize database
        val sessionFactory = hibernate.sessionFactory
        val projectDao: DataAccessObject<ProjectModel> = ProjectDao(sessionFactory)
        val projectBuilder = ProjectBuilder(projectDao, File(configuration.downloadDirectory), sessionFactory)

        arrayOf(
                ProjectResource(projectDao, projectBuilder),
                UnhandledExceptionLogger()
        ).forEach {
            environment.jersey().register(it)
        }
    }
}

public class GradleBootstrapConf : Configuration() {
    public @valid @notNull @jsonProperty val database: DataSourceFactory = DataSourceFactory()
    public @valid @notNull @jsonProperty val downloadDirectory: String = ""
    companion object {
        public val timeZone: DateTimeZone = DateTimeZone.UTC

        /** The amount of time that can pass before one download pass expires. Equivalent to one hour. */
        public val expirationDuration: Duration = Duration.standardHours(1)

        public @JvmStatic fun configureObjectMapper(mapper: ObjectMapper) {
            // Dates will now automatically be serialized into the ISO-8601 format
            mapper.setDateFormat(ISO8601DateFormat())
        }

        public @JvmStatic fun getCurrentDate(): DateTime = DateTime(timeZone)
        public @JvmStatic fun getPassExpirationDate(date: DateTime = getCurrentDate()): DateTime = date.plus(expirationDuration)
    }
}

public @provider class UnhandledExceptionLogger : ExceptionMapper<Throwable> {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    public @context var request: HttpServletRequest? = null

    override fun toResponse(t: Throwable?): Response? {
        if (t is WebApplicationException) {
            // These will be shown to the user, don't mess with this
            return t.response
        }

        log.error("Unhandled exception", t)
        log.error("URL: ${HttpUtils.getRequestURL(request)}")
        return Response.status(500)
                .build()
    }
}

