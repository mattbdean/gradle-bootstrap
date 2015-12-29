package net.dean.gbs.web.resources

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType
import net.dean.gbs.web.Parameter
import javax.ws.rs.core.UriInfo

/**
 * Reasons why a request did not succeed
 */
public enum class ErrorCode {
    BAD_GIT_URL,
    BAD_LENGTH,
    DOWNLOAD_NOT_READY,
    INVALID_IDENTIFIER,
    MALFORMED_UUID,
    MALFORMED_URL,
    MISSING_PARAM,
    NOT_ENUM_VALUE,
    NOT_FOUND
}

/**
 * This class provides a model to be serialized into JSON and shown to the client
 */
public data class JsonError(public val error: ErrorCode,
                            public val why: String,
                            public val websiteWhy: String,
                            public val param: Parameter<*>,
                            public val path: String)

/**
 * This class provides an abstraction for forming the basic parts of an error message
 *
 * resourceClass: The resource class which is being requested
 * errorId: A constant shared between all errors of similar natures
 * why: Why this exception is being thrown and what can be done to fix it
 * status: The HTTP status to respond to the client with
 * param: The parameter in question
 */
public open class RequestException(errorId: ErrorCode,
                                   why: String,
                                   websiteWhy: String,
                                   status: Int,
                                   public val param: Parameter<*>) : WebApplicationException(
        Response.status(status)
                .entity(JsonError(path = param.uri,
                        error = errorId,
                        why = why,
                        websiteWhy = websiteWhy,
                        param = param))
                .type(MediaType.APPLICATION_JSON)
                .build()) {

    companion object {
        public fun formatParam(p: Parameter<*>): String = "<${p.name}>"
    }
}

/**
 * This class provides an abstraction for classes whose parameters are invalid. The response HTTP status will always
 * be 422 Unprocessable Entity.
 */
public abstract class MalformedParameterException(errorId: ErrorCode,
                                                  why: String,
                                                  websiteWhy: String,
                                                  param: Parameter<*>) : RequestException (
        errorId = errorId,
        why = why,
        websiteWhy = websiteWhy,
        status = 422, // 422 Unprocessable Entity
        param = param
)

/**
 * This class is thrown when a parameter was checked to be missing, but was required.
 */
public class MissingRequiredParamException(param: Parameter<*>) : MalformedParameterException(
                errorId = ErrorCode.MISSING_PARAM,
                why = "Missing or empty value for ${RequestException.formatParam(param)}",
                websiteWhy = "Please enter a value",
                param = param
        )

/**
 * This class is thrown when an parameter in the client's request was not in the format of what was expected by the server.
 */
public class InvalidParamException(why: String,
                                   errorId: ErrorCode,
                                   websiteWhy: String,
                                   param: Parameter<*>) : MalformedParameterException(
                errorId = errorId,
                why = why,
                websiteWhy = websiteWhy,
                param = param
        )

public class NotFoundException(why: String,
                               websiteWhy: String,
                               param: Parameter<*>) : RequestException(
                errorId = ErrorCode.NOT_FOUND,
                why = why,
                websiteWhy = websiteWhy,
                param = param,
                status = 404
)

public class ForbiddenException(why: String,
                                websiteWhy: String,
                                errorId: ErrorCode,
                                param: Parameter<*>) : RequestException(
        errorId = errorId,
        why = why,
        websiteWhy = websiteWhy,
        param = param,
        status = 403
)
