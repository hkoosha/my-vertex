package io.koosha.vertx.b_stream;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public final class Jukebox extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(Jukebox.class);

    private final Queue<String> playlist = new ArrayDeque<>();
    private final Set<HttpServerResponse> streamers = new HashSet<>();
    private State state = State.PAUSED;
    private AsyncFile currentFile;
    private int positionInFile = 0;

    @Override
    public void start() {
        vertx.eventBus().consumer(Config.ENDPOINT__LIST, this::list);
        vertx.eventBus().consumer(Config.ENDPOINT__PAUSE, this::pause);
        vertx.eventBus().consumer(Config.ENDPOINT__PLAY, this::play);
        vertx.eventBus().consumer(Config.ENDPOINT__SCHEDULE, this::schedule);

        vertx.createHttpServer()
             .requestHandler(this::httpHandler)
             .listen(Config.PORT)
             .onFailure(event -> log.error("http server failed to start", event.getCause()))
             .onSuccess(event -> log.info("http server started on: {}", Config.PORT));

        vertx.setPeriodic(Config.DELAY, this::tick);
    }


    private void tick(final long timerId) {
        if (this.state == State.PAUSED)
            return;

        if (this.currentFile == null && playlist.isEmpty()) {
            log.info("no more files, pausing");
            this.state = State.PAUSED;
            return;
        }

        if (this.currentFile == null)
            this.openNextFile();

        this.currentFile.read(Buffer.buffer(Config.BUFFER_SIZE), 0, positionInFile, Config.BUFFER_SIZE, ar -> {
            if (ar.failed()) {
                log.error("read failed", ar.cause());
                this.closeCurrentFile();
                return;
            }

            final Buffer buffer = ar.result();
            this.positionInFile += buffer.length();
            if (buffer.length() == 0)
                this.closeCurrentFile();
            else
                for (final HttpServerResponse streamer : this.streamers)
                    if (!streamer.writeQueueFull())
                        streamer.write(buffer.copy());
        });
    }


    private void openNextFile() {
        final String file = this.playlist.poll();
        final String path = Paths.get(Config.DIR, file).toString();
        log.info("opening file={}", path);

        this.currentFile = vertx.fileSystem().openBlocking(path, Util.forRead());
        this.positionInFile = 0;
    }

    private void closeCurrentFile() {
        this.currentFile.close().onFailure(event -> log.error("closing file failed", event.getCause()));
        this.currentFile = null;
        this.positionInFile = 0;
    }


    private void httpHandler(final HttpServerRequest req) {
        log.info("req for path: {}", req.path());

        final String cmd;
        if (req.path().equals("/") || req.path().equals(""))
            cmd = "/";
        else if (req.path().startsWith("/") && req.path().split("/")[1].equals("download")
            || req.path().equals("download"))
            cmd = "download";
        else
            cmd = "NONE";

        switch (cmd) {
            case "/":
                this.openStream(req);
                break;

            case "download":
                this.download(req);
                break;

            default:
                req.response().setStatusCode(404).end();
        }
    }

    private void download(final HttpServerRequest req) {
        final String sanitizedPath = req.path().replaceAll("/", "");
        final Path file = Paths.get(Config.DIR, sanitizedPath);
        log.info("download request from={}, file={}", req.remoteAddress(), file);
        vertx.fileSystem().exists(file.toString())
             .onFailure(event -> {
                 log.error("error checking if exists, file={}", file, event.getCause());
                 req.response().setStatusCode(500).end();
             })
             .onSuccess(exists -> {
                 if (!exists) {
                     log.warn("requested file does not exist: {}", file);
                     req.response().setStatusCode(404).end();
                     return;
                 }

                 vertx.fileSystem().open(file.toString(), Util.forRead(), ar -> {
                     if (ar.failed()) {
                         log.error("error opening, file={}", file, ar.cause());
                         req.response().setStatusCode(500).end();
                         return;
                     }

                     final AsyncFile fileToDownload = ar.result();
                     log.info("streaming to={}, file={}", req.remoteAddress(), fileToDownload);
                     req.response()
                        .setStatusCode(200)
                        .putHeader(Util.HEADER__CONTENT_TYPE, Util.HEADER__CONTENT_TYPE__AUDIO_MPEG)
                        .setChunked(true);
                     fileToDownload.pipeTo(req.response());
                 });
             });
    }

    private void openStream(final HttpServerRequest req) {
        log.info("streamer joined: {}", req.remoteAddress());

        final HttpServerResponse res = req
            .response()
            .putHeader(Util.HEADER__CONTENT_TYPE, Util.HEADER__CONTENT_TYPE__AUDIO_MPEG)
            .setChunked(true);

        this.streamers.add(res);

        final String addr = req.remoteAddress().toString();
        res.endHandler(event -> {
            streamers.remove(res);
            log.info("streamer left: {}", addr);
        });
    }

    private void schedule(final Message<JsonObject> request) {
        final String file = request.body().getString(Config.PARAM__FILE_NAME);

        if (file == null || file.isEmpty()) {
            log.warn("no file!");
            return;
        }

        if (this.playlist.isEmpty() && this.state == State.PAUSED) {
            log.info("playing...");
            this.state = State.PLAYING;
        }

        log.info("scheduling, file={}", file);
        playlist.offer(file);
    }


    private void play(final Message<?> message) {
        log.info("play request");
        this.state = State.PLAYING;
    }

    private void pause(final Message<?> message) {
        log.info("pause request");
        this.state = State.PAUSED;
    }


    private void list(final Message<?> req) {
        log.info("list request, dir={}", Config.DIR);
        vertx.fileSystem().readDir(Config.DIR, ".*\\.mp3$", ar -> {
            if (ar.failed()) {
                log.error("readDir failed", ar.cause());
                req.fail(500, ar.cause().getMessage());
            }
            else {
                final List<String> files = ar.result()
                                             .stream()
                                             .map(File::new)
                                             .map(File::getName)
                                             .collect(Collectors.toList());

                if (files.isEmpty())
                    log.warn("no file");

                final JsonObject json = Util.obj(Config.PARAM__FILES, Util.arr(files));
                req.reply(json);
            }
        });
    }

}
