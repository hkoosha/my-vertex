package io.koosha.vertx.c_callback;

public final class Config {

    private Config() {
        throw new UnsupportedOperationException("can not instantiate utility class");
    }

    public static final String CFG__PORT = "port";

    public static final String FIELD__ID = "id";
    public static final String FIELD__TEMPERATURE = "temperature";
    public static final String FIELD__DATA = "data";

    public static final int COLLECTOR_PORT = 8080;

    public static final int SENSOR_PORT_BASE = 3000;

    public static final Integer SNAPSHOT_PORT = 4000;

}
