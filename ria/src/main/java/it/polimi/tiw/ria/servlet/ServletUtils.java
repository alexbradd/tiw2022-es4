package it.polimi.tiw.ria.servlet;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.functional.ApiResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Miscellaneous utilities
 */
public class ServletUtils {
    private static final Supplier<ApiError> wrongTypeErrorSupplier =
            () -> new ApiError(400, "Wrong content type");
    private static final Supplier<ApiError> invalidFormatSupplier =
            () -> new ApiError(400, "Object is not formatted correctly");

    /**
     * Sends a formatted JSON error message with status code 400. To be used when the request does not contain JSON.
     *
     * @param res the {@link HttpServletResponse}
     * @throws IOException if an IO error is encountered
     */
    public static void sendWrongTypeError(HttpServletResponse res) throws IOException {
        sendJson(res, 400, fromApiErrorToJSON(wrongTypeErrorSupplier.get()));
    }

    /**
     * Sends a formatted JSON error message with status code 400. To be used when the request does contain JSON, but
     * is incompatible with the format expected (e.g. excepting an object and getting a string).
     *
     * @param res the {@link HttpServletResponse}
     * @throws IOException if an IO error is encountered
     */
    public static void sendInvalidFormatError(HttpServletResponse res) throws IOException {
        sendJson(res, 400, fromApiErrorToJSON(invalidFormatSupplier.get()));
    }

    /**
     * Converts an {@link ApiError} to an error JSON object.
     *
     * @param err the {@link ApiError}
     * @return a {@link JsonObject}
     */
    public static JsonObject fromApiErrorToJSON(ApiError err) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "ERROR");
        obj.add("error", err.toJson());
        return obj;
    }

    /**
     * Sets the status to the given integer and writes the given JSON in the response.
     *
     * @param res    the {@link HttpServletResponse}
     * @param status the status
     * @param json   the JSON
     * @throws IOException if an IO error is encountered
     */
    public static void sendJson(HttpServletResponse res, int status, JsonElement json) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().println(json.toString());
    }

    /**
     * Check that the body of the request is JSON and that it deserializes to the given class. Then apply the given
     * {@link Predicate} to check whether the parsed object is acceptable or not.
     *
     * @param gson         the {@link Gson} instance to use
     * @param req          the {@link HttpServletRequest} whose body to parse
     * @param requestClass the class containing the data of the request
     * @param invalidCheck the {@link Predicate} that tests whether the parsed object is invalid or not
     * @param <T>          the type of the request class
     * @return an {@link ApiResult} containing the parsed object or a suitable {@link ApiError}
     * @throws IOException if any IO exception happened
     */
    public static <T> ApiResult<T> checkRequestFormat(Gson gson, HttpServletRequest req, Class<T> requestClass, Predicate<T> invalidCheck) throws IOException {
        return checkRequestFormat(gson, req, requestClass, Function.identity(), invalidCheck);
    }

    /**
     * Check that the body of the request is JSON and apply the mapper. Then deserialize the object to the given class
     * and apply the {@link Predicate} to check whether the parsed object is acceptable or not.
     *
     * @param gson         the {@link Gson} instance to use
     * @param req          the {@link HttpServletRequest} whose body to parse
     * @param requestClass the class containing the data of the request
     * @param jsonModifier the mapper that modifies the read JSON before being deserialized to {@code requestClass}
     * @param invalidCheck the {@link Predicate} that tests whether the parsed object is invalid or not
     * @param <T>          the type of the request class
     * @return an {@link ApiResult} containing the parsed object or a suitable {@link ApiError}
     * @throws IOException if any IO exception happened
     */
    public static <T> ApiResult<T> checkRequestFormat(Gson gson,
                                                      HttpServletRequest req,
                                                      Class<T> requestClass,
                                                      Function<JsonElement, JsonElement> jsonModifier,
                                                      Predicate<T> invalidCheck) throws IOException {
        if (hasNotJSONContentType(req))
            return ApiResult.error(wrongTypeErrorSupplier.get());

        T ret;
        try (JsonReader reader = new JsonReader(req.getReader())) {
            JsonElement elem = gson.fromJson(reader, JsonElement.class);
            elem = jsonModifier.apply(elem);
            ret = gson.fromJson(elem, requestClass);
            return ret == null || invalidCheck.test(ret)
                    ? ApiResult.error(invalidFormatSupplier.get())
                    : ApiResult.ok(ret);
        } catch (JsonParseException | IllegalStateException | ClassCastException e) {
            return ApiResult.error(invalidFormatSupplier.get());
        }
    }


    /**
     * Returns true if the given request has not a JSON Content-Type header.
     *
     * @param req the {@link HttpServletRequest}
     * @return true if the given request has not a JSON Content-Type header
     */
    public static boolean hasNotJSONContentType(HttpServletRequest req) {
        return !Objects.equals(req.getContentType(), "application/json");
    }

    /**
     * Collects the given list of objects into a {@link JsonArray} of {@link JsonObject}.
     *
     * @param gson the {@link Gson} instance used to convert the objects to json
     * @param list the {@link List} to collect
     * @param peek a {@link Consumer} that modifies each {@link JsonObject}
     * @param <T>  the type of the object to parse
     * @return a {@link JsonArray}
     */
    public static <T> JsonArray listToJsonArray(Gson gson, List<T> list, Consumer<JsonObject> peek) {
        return list.stream()
                .map(gson::toJsonTree)
                .map(JsonElement::getAsJsonObject)
                .peek(peek)
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
    }

    /**
     * Collects the given list of objects into a {@link JsonArray} of {@link JsonElement}.
     *
     * @param gson the {@link Gson} instance used to convert the objects to json
     * @param list the {@link List} to collect
     * @param <T>  the type of the object to parse
     * @return a {@link JsonArray}
     */
    public static <T> JsonArray listToJsonArray(Gson gson, List<T> list) {
        return list.stream()
                .map(gson::toJsonTree)
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
    }
}
