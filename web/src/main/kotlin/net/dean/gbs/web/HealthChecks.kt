package net.dean.gbs.web

import com.codahale.metrics.health.HealthCheck

public class TemplateHealthCheck(private val template: String) : HealthCheck() {
    override fun check(): HealthCheck.Result? {
        val saying = template.format("TEST")
        if (!saying.contains("TEST"))
            return HealthCheck.Result.unhealthy("Template does not include a name")
        return HealthCheck.Result.healthy()
    }
}
