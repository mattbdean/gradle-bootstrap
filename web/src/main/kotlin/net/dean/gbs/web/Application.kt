package net.dean.gbs.web

import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import kotlin.platform.platformStatic
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.Configuration
import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import io.dropwizard.db.DataSourceFactory
import org.hibernate.validator.constraints.NotEmpty as notEmpty
import com.fasterxml.jackson.annotation.JsonProperty as jsonProperty
import javax.validation.constraints.NotNull as notNull
import javax.validation.Valid as valid
import net.dean.gbs.web.db.ProjectDao
import net.dean.gbs.web.resources.ProjectResource
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.ObjectMapper
import io.dropwizard.jackson.FuzzyEnumModule
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.nio.file.Paths
import javax.ws.rs.ext.ExceptionMapper
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider as provider
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Context as context
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpUtils

public class GradleBootstrap : Application<GradleBootstrapConf>() {
    class object {
        public platformStatic fun main(args: Array<String>) {
            GradleBootstrap().run(args)
        }
    }

    override fun initialize(bootstrap: Bootstrap<GradleBootstrapConf>?) {
    }

    override fun run(configuration: GradleBootstrapConf, environment: Environment) {
        // Configure the object mapper
        GradleBootstrapConf.configureObjectMapper(environment.getObjectMapper())

        // Initialize database
        val jdbi = DBIFactory().build(environment, configuration.database, "h2")
        val projectDao = jdbi.onDemand(javaClass<ProjectDao>())
        val projectBuilder = ProjectBuilder(projectDao, Paths.get(configuration.downloadDirectory))
        projectDao.createTable()

        // Register resources
        listOf(
                ProjectResource(projectDao, projectBuilder)
        ).forEach {
            environment.jersey().register(it)
        }
    }
}

public class GradleBootstrapConf : Configuration() {
    public valid notNull jsonProperty val database: DataSourceFactory = DataSourceFactory()
    public valid notNull jsonProperty val downloadDirectory: String = ""
    class object {
        public platformStatic val TIME_ZONE: DateTimeZone = DateTimeZone.UTC
        public platformStatic fun configureObjectMapper(mapper: ObjectMapper) {
            // Dates will now automatically be serialized into the ISO-8601 format
            mapper.setDateFormat(ISO8601DateFormat())
            // Use snake_case when serializing data
            mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
        }

        public platformStatic fun getCurrentDate(): DateTime = DateTime(TIME_ZONE)
    }
}

public provider class UnhandledExceptionLogger : ExceptionMapper<Throwable> {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    public context var request: HttpServletRequest? = null

    override fun toResponse(t: Throwable?): Response? {
        if (t is WebApplicationException) {
            // These will be shown to the user, don't mess with this
            return t.getResponse()
        }

        log.error("Unhandled exception", t)
        log.error("URL: ${HttpUtils.getRequestURL(request)}")
        return Response.status(500).build()
    }
}

