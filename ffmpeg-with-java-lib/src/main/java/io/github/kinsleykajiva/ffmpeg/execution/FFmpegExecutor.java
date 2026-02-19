package io.github.kinsleykajiva.ffmpeg.execution;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.kinsleykajiva.ffmpeg.exception.ExecutionException;
import io.github.kinsleykajiva.ffmpeg.exception.TimeoutException;
import io.github.kinsleykajiva.ffmpeg.model.EncodingResult;

/**
 * Handles the execution of FFmpeg processes and parses progress output.
 */
public class FFmpegExecutor {

    // Regex to parse FFmpeg's progress output. Works for both video and audio-only streams.
    // Captures: frame (optional), bitrate, and speed.
    private static final Pattern PROGRESS_PATTERN = Pattern.compile(
        "(?:frame=\\s*(\\d+)|(?:size|time|out_time)=\\s*\\S+).*bitrate=\\s*([\\d\\.]+)kbits/s.*speed=\\s*([\\d\\.]+)x"
    );

    /**
     * Executes the command synchronously, with optional timeout.
     * Output is drained on a background thread to avoid pipe-buffer deadlocks.
     */
    public static EncodingResult execute(List<String> args,
                                         OnProgressListener progressListener,
                                         OnStreamStatsListener statsListener,
                                         long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true); // merge stderr into stdout

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            // Drain the process output on a dedicated thread to prevent pipe-buffer deadlock.
            Future<?> readerFuture = Executors.newSingleThreadExecutor().submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        parseAndNotify(line, progressListener, statsListener);
                    }
                } catch (Exception ignored) {}
            });

            // Apply optional timeout: destroy the process after the deadline.
            if (timeoutSeconds > 0) {
                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    // Wait for the reader to drain any last output before throwing.
                    try { readerFuture.get(2, TimeUnit.SECONDS); } catch (Exception ignored) {}
                    throw new TimeoutException(timeoutSeconds);
                }
            }

            // Wait for the process and the reader to both finish.
            int exitCode = process.waitFor();
            try { readerFuture.get(5, TimeUnit.SECONDS); } catch (Exception ignored) {}

            if (exitCode != 0) {
                throw new ExecutionException(exitCode, output.toString());
            }

            // For streaming destinations the last arg is a URL, not a file path.
            String lastArg = args.get(args.size() - 1);
            Path outputPath = (lastArg.startsWith("rtp://")
                    || lastArg.startsWith("udp://")
                    || lastArg.startsWith("srt://"))
                ? null : Path.of(lastArg);

            long duration = System.currentTimeMillis() - startTime;
            long fileSize = (outputPath != null && outputPath.toFile().exists())
                ? outputPath.toFile().length() : 0;

            return new EncodingResult(outputPath, duration, fileSize);
        } catch (Exception e) {
            if (e instanceof io.github.kinsleykajiva.ffmpeg.exception.FFmpegException fe) throw fe;
            throw new ExecutionException(-1, e.getMessage());
        }
    }

    /**
     * Executes the command asynchronously, with optional timeout.
     */
    public static CompletableFuture<EncodingResult> executeAsync(List<String> args,
                                                                  OnProgressListener progressListener,
                                                                  OnStreamStatsListener statsListener,
                                                                  long timeoutSeconds) {
        return CompletableFuture.supplyAsync(
            () -> execute(args, progressListener, statsListener, timeoutSeconds));
    }

    /**
     * Executes the command asynchronously without a timeout (runs until FFmpeg finishes).
     *
     * @deprecated Prefer {@link #executeAsync(List, OnProgressListener, OnStreamStatsListener, long)}
     *             so that timeouts are honoured.
     */
    @Deprecated
    public static CompletableFuture<EncodingResult> executeAsync(List<String> args,
                                                                  OnProgressListener progressListener,
                                                                  OnStreamStatsListener statsListener) {
        return executeAsync(args, progressListener, statsListener, 0);
    }

    private static boolean parseAndNotify(String line, OnProgressListener progress, OnStreamStatsListener stats) {
        Matcher matcher = PROGRESS_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                String frameStr = matcher.group(1);
                long frame = (frameStr != null) ? Long.parseLong(frameStr) : 0;
                double bitrateKbps = Double.parseDouble(matcher.group(2));
                double speed = Double.parseDouble(matcher.group(3));

                if (progress != null) {
                    progress.onProgress(0, frame, bitrateKbps);
                }

                if (stats != null) {
                    stats.onStatsUpdate((long)(bitrateKbps * 1000), speed, 0);
                }
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
