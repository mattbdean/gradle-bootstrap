package net.dean.gbs.web

import io.dropwizard.Configuration
import org.hibernate.validator.constraints.NotEmpty as notEmpty
import com.fasterxml.jackson.annotation.JsonProperty as jsonProperty

public class HelloWorldConf : Configuration() {
    public notEmpty jsonProperty val template: String? = null
    public notEmpty jsonProperty val defaultName: String = "Stranger"
}
