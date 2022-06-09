package it.polimi.tiw.api.beans;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.Instant;

/**
 * {@link Instant} type adapter for Gson.
 *
 * @see JsonSerializer
 */
public class InstantTypeAdapter implements JsonSerializer<Instant> {
    /**
     * {@inheritDoc}
     */
    @Override
    public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
