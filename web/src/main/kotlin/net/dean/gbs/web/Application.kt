package net.dean.gbs.web

import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import kotlin.platform.platformStatic
import io.dropwizard.Configuration
import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import io.dropwizard.db.DataSourceFactory
import org.hibernate.validator.constraints.NotEmpty as notEmpty
import com.fasterxml.jackson.annotation.JsonProperty as jsonProperty
import javax.validation.constraints.NotNull as notNull
import javax.validation.Valid as valid
import net.dean.gbs.web.db.ProjectDao
import net.dean.gbs.web.resources.ProjectResource
import com.fasterxml.jackson.databind.ObjectMapper
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
import io.dropwizard.hibernate.HibernateBundle
import net.dean.gbs.web.models.ProjectModel
import org.joda.time.Duration
import net.dean.gbs.web.db.DataAccessObject
import javax.servlet.http.HttpUtils
import io.dropwizard.assets.AssetsBundle
import net.dean.gbs.web.resources.IndexResource
import io.dropwizard.views.ViewMessageBodyWriter
import io.dropwizard.views.freemarker.FreemarkerViewRenderer
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext

public class GradleBootstrap : Application<GradleBootstrapConf>() {
    class object {
        public platformStatic fun main(args: Array<String>) {
            GradleBootstrap().run(*args)
        }
    }

    val hibernate = object: HibernateBundle<GradleBootstrapConf>(javaClass<ProjectModel>()) {
        override fun getDataSourceFactory(configuration: GradleBootstrapConf?): DataSourceFactory? {
            return configuration!!.database
        }
    }

    override fun initialize(bootstrap: Bootstrap<GradleBootstrapConf>) {
        for (type in array("css", "js")) {
            bootstrap.addBundle(AssetsBundle("/assets/$type", "/$type", null, type))
        }
        bootstrap.addBundle(hibernate)
    }

    override fun run(configuration: GradleBootstrapConf, environment: Environment) {
        // Configure the object mapper
        GradleBootstrapConf.configureObjectMapper(environment.getObjectMapper())

        // Initialize database
        val sessionFactory = hibernate.getSessionFactory()
        val projectDao: DataAccessObject<ProjectModel> = ProjectDao(sessionFactory)
        val projectBuilder = ProjectBuilder(projectDao, Paths.get(configuration.downloadDirectory), sessionFactory)

        array(
                ProjectResource(projectDao, projectBuilder),
                IndexResource(),
                ViewMessageBodyWriter(environment.metrics(), listOf(FreemarkerViewRenderer())),
                UnhandledExceptionLogger(),
                HeaderFilter()
        ).forEach {
            environment.jersey().register(it)
        }
    }
}

public class GradleBootstrapConf : Configuration() {
    public valid notNull jsonProperty val database: DataSourceFactory = DataSourceFactory()
    public valid notNull jsonProperty val downloadDirectory: String = ""
    class object {
        public platformStatic val timeZone: DateTimeZone = DateTimeZone.UTC

        /** The amount of time that can pass before one download pass expires. Equivalent to one hour. */
        public platformStatic val expirationDuration: Duration = Duration.standardHours(1)

        public platformStatic fun configureObjectMapper(mapper: ObjectMapper) {
            // Dates will now automatically be serialized into the ISO-8601 format
            mapper.setDateFormat(ISO8601DateFormat())
        }

        public platformStatic fun getCurrentDate(): DateTime = DateTime(timeZone)
        public platformStatic fun getPassExpirationDate(date: DateTime = getCurrentDate()): DateTime = date.plus(expirationDuration)
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
        return Response.status(500)
                .build()
    }
}

public provider class HeaderFilter : ContainerResponseFilter {
    class object {
        private val headers = listOf(
                "X-Frame-Options" to "deny",
                "X-Content-Type-Options" to "nosniff"
        )
    }
    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        for ((key, value) in headers)
            responseContext.getHeaders().add(key, value)
    }
}
