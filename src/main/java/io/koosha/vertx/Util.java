package io.koosha.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public final class Util {

    private Util() {
        throw new UnsupportedOperationException("can not instantiate utility class");
    }

    public static final String HEADER__CONTENT_TYPE = "Content-Type";
    public static final String HEADER__CONTENT_TYPE__EVENT_STREAM = "text/event-stream";
    public static final String HEADER__CONTENT_TYPE__AUDIO_MPEG = "audio/mpeg";

    public static final String HEADER__CACHE_CONTROL = "Cache-Control";
    public static final String HEADER__CACHE_CONTROL__NO_CACHE = "no-cache";


    public static String readResource(final String name) throws IOException {
        //noinspection ConstantConditions
        try (final InputStream is = Util.class.getClassLoader().getResourceAsStream(name);
             final InputStreamReader isr = new InputStreamReader(is);
             final BufferedReader br = new BufferedReader(isr)) {
            return br.lines().collect(Collectors.joining("\n"));
        }
        catch (NullPointerException npe) {
            throw new IOException("no such resource: " + name);
        }
    }


    public static DeploymentOptions instances(final int count) {
        return new DeploymentOptions().setInstances(count);
    }

    public static DeploymentOptions configed(final String key, final Object value) {
        return new DeploymentOptions().setConfig(obj(key, value));
    }


    public static JsonObject obj(final String key, final Object value) {
        return new JsonObject().put(key, value);
    }

    public static JsonArray arr(final List<?> values) {
        return new JsonArray(values);
    }


    public static OpenOptions forRead() {
        return new OpenOptions().setRead(true);
    }


    public static void deploy(final Class<?>... verticles) {
        final Vertx vertx = Vertx.vertx();
        for (final Class<?> verticle : verticles)
            vertx.deployVerticle(verticle.getName());
    }

}
