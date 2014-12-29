package net.dean.gbs.web.resources

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType
import java.net.URLDecoder
import javax.ws.rs.core.UriBuilder
import net.dean.gbs.web.ParamLocation
import net.dean.gbs.web.Parameter
import java.util.HashMap
import javax.ws.rs.Path

/**
 * Reasons why a request did not succeed
 */
public enum class RequestError {
    MISSING_PARAM
    INVALID_PARAM
}

/**
 * This class provides a model to be serialized into JSON and shown to the client
 */
public data class JsonError(public val error: RequestError,
                            public val why: String,
                            public val param: Parameter,
                            public val path: String)

/**
 * This singleton class provides a way to retrieve the value of a class' @Path annotation
 */
public object ResourceUriCache {
    private val map: MutableMap<Class<*>, String> = HashMap()

    public fun get(klass: Class<*>): String {
        val pathClass = javaClass<Path>()
        if (!klass.isAnnotationPresent(pathClass)) {
            throw IllegalArgumentException("Class ${klass.getName()} is not annotated with ${pathClass.getName()}")
        }

        val uri = klass.getAnnotation(pathClass).value()

        if (klass !in map) {
            map.put(klass, uri)
        }
        return map.get(klass)!!
    }
}

/**
 * This class provides an abstraction for forming the basic parts of an error message
 *
 * resourceClass: The resource class which is being requested
 * errorId: A constant shared between all errors of similar natures
 * why: Why this exception is being thrown and what can be done to fix it
 * status: The HTTP status to respond to the client with
 * param: The parameter in question
 */
public abstract class RequestException(resourceClass: Class<*>,
                                       errorId: RequestError,
                                       why: String,
                                       status: Int,
                                       public val param: Parameter) : WebApplicationException(
        Response.status(status)
                .entity(JsonError(path = ResourceUriCache.get(resourceClass),
                        error = errorId,
                        why = why,
                        param = param))
                .type(MediaType.APPLICATION_JSON)
                .build()) {

    class object {
        public fun formatParam(p: Parameter): String = "<${p.name}>"
    }
}

/**
 * This class provides an abstraction for classes whose parameters are invalid. The response HTTP status will always
 * be 422 Unprocessable Entity.
 */
public abstract class MalformedParameterException(resourceClass: Class<*>,
                                                 errorId: RequestError,
                                                 why: String,
                                                 param: Parameter) : RequestException (
        resourceClass = resourceClass,
        errorId = errorId,
        why = why,
        status = 422, // 422 Unprocessable Entity
        param = param
)

/**
 * This class is thrown when a parameter was checked to be missing, but was required.
 */
public class MissingRequiredParamException(resourceClass: Class<*>,
                                           param: Parameter) : MalformedParameterException(
                resourceClass = resourceClass,
                errorId = RequestError.MISSING_PARAM,
                why = "Missing or empty value for ${RequestException.formatParam(param)}",
                param = param
        )

/**
 * This class is thrown when an parameter in the client's request was not in the format of what was expected by the server.
 */
public class InvalidParamException(resourceClass: Class<*>,
                                   why: String,
                                   param: Parameter) : MalformedParameterException(
                resourceClass = resourceClass,
                errorId = RequestError.INVALID_PARAM,
                why = why,
                param = param
        )