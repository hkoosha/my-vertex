package io.koosha.vertx.a_heat;

public final class Config {


    private Config() {
        throw new UnsupportedOperationException("can not instantiate utility class");
    }

    public static final int PORT = 8080;

    public static final String ENDPOINT__SENSOR_UPDATES = "sensor.updates";
    public static final String ENDPOINT__SENSOR_AVERAGE = "sensor.average";

    public static final String FIELD__ID = "id";
    public static final String FIELD__TEMPERATURE = "temperature";
    public static final String FIELD__AVERAGE = "average";
}
