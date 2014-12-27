package net.dean.gbs.web

import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.GET
import net.dean.gbs.api.Project
import javax.ws.rs.QueryParam
import com.google.common.base.Optional
import net.dean.gbs.api.Language

Path("/basic-project")
Produces(MediaType.APPLICATION_JSON)
public class SampleProjectResource {

    GET public fun sampleProject(QueryParam("name") name: Optional<String>,
                                 QueryParam("group") group: Optional<String>): Project {
        val proj = Project(name.or("basic-project"), group.or("com.example.app"))
        proj.add(Language.JAVA)
        return proj
    }

}
