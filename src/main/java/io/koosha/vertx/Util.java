package io.koosha.vertx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public final class Util {

    private Util() {
        throw new UnsupportedOperationException("can not instantiate utility class");
    }

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

}
