package net.dean.gbs.web

/**
 * Represents a parameter sent to the server
 */
public data class Parameter(public val name: String,
                            public val value: Any?,
                            public val location: ParamLocation)

/**
 * Where a parameter is located
 */
public enum class ParamLocation {
    /** An argument in the query string, such as /resource?name=foo */
    QUERY
    /** A positional URI param, such as /resource/{name} */
    URI
    /** An argument in the body of a request. Depends on the media type of the request. */
    BODY
    /** An argument located in the request's headers, such as X-Foo: Bar */
    HEADER
}
