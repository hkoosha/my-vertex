package io.koosha.vertx.b_stream;

import io.koosha.vertx.Util;

public final class JukeboxMain {

    public static void main(final String... args) {
        Util.deploy(Jukebox.class, NetControl.class);
    }

}
