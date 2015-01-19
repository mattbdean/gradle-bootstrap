package net.dean.gbs.web.resources

import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import net.dean.gbs.web.views.IndexView
import javax.ws.rs.GET

Path("/")
Produces(MediaType.TEXT_HTML)
public class IndexResource {
    GET public fun get(): IndexView = IndexView()
}
