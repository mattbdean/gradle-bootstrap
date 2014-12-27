package net.dean.gbs.web

import io.dropwizard.Configuration
import kotlin.properties.Delegates
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import kotlin.platform.platformStatic
import org.hibernate.validator.constraints.Length
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import java.util.concurrent.atomic.AtomicLong
import com.codahale.metrics.annotation.Timed
import javax.ws.rs.GET
import javax.ws.rs.QueryParam
import com.google.common.base.Optional

public class GradleBootstrap : Application<HelloWorldConf>() {
    class object {
        public platformStatic fun main(args: Array<String>) {
            GradleBootstrap().run(args)
        }
    }

    override fun initialize(bootstrap: Bootstrap<HelloWorldConf>?) {

    }

    override fun run(configuration: HelloWorldConf, environment: Environment) {
        val sampleResource = SampleProjectResource()
        environment.jersey().register(sampleResource)
        // TODO: Health checks
    }

}

