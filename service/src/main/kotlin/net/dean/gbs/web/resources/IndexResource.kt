package net.dean.gbs.web.resources

import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import net.dean.gbs.web.views.IndexView
import javax.ws.rs.GET

Path("/")
public class IndexResource {
    Produces(MediaType.TEXT_HTML)
    GET public fun get(): IndexView = IndexView()

    Path("googlec536b80d9a503e51.html")
    Produces(MediaType.TEXT_HTML)
    GET public fun googleWebmaster(): String =
        """google-site-verification: googlec536b80d9a503e51.html"""

    Path("sitemap.xml")
    Produces(MediaType.TEXT_XML)
    GET public fun sitemap(): String =
"""<?xml version="1.0" encoding="UTF-8"?>
<urlset
      xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9
            http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
<url>
  <loc>http://gradle-bootstrap.herokuapp.com/</loc>
  <lastmod>2015-01-22T14:00:39+00:00</lastmod>
  <changefreq>weekly</changefreq>
</url>
</urlset>
"""

    Produces(MediaType.TEXT_PLAIN)
    Path("robots.txt")
    GET public fun robotsTxt(): String =
"""User-Agent: *
Disallow: /project/*/download
Disallow: /admin

Sitemap: http://gradle-bootstrap.herokuapp.com/sitemap.xml
"""
}
