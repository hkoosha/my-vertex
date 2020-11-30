package io.koosha.vertx.b_stream;

public final class Config {


    private Config() {
        throw new UnsupportedOperationException("can not instantiate utility class");
    }

    public static final int PORT = 8080;
    public static final int NET_CONTROL_PORT = 3000;

    public static final long DELAY = 100L;
    public static final int BUFFER_SIZE = 4096;

    public static final String ENDPOINT__LIST = "jukebox.list";
    public static final String ENDPOINT__SCHEDULE = "jukebox.schedule";
    public static final String ENDPOINT__PLAY = "jukebox.play";
    public static final String ENDPOINT__PAUSE = "jukebox.pause";

    public static final String PARAM__FILE_NAME = "file";
    public static final String PARAM__FILES = "files";

    public static final String DIR = "/a";

}
