package io.github.kinsleykajiva.ffmpeg.execution;

import io.github.kinsleykajiva.ffmpeg.FFmpegBinary;
import io.github.kinsleykajiva.ffmpeg.exception.ExecutionException;
import io.github.kinsleykajiva.ffmpeg.exception.TimeoutException;
import io.github.kinsleykajiva.ffmpeg.model.EncodingResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the execution of FFmpeg processes and parses progress output.
 */
public class FFmpegExecutor {

    // Regex to parse FFmpeg's progress output from stderr
    private static final Pattern PROGRESS_PATTERN = Pattern.compile(
        "frame=\\s*(\\d+).*bitrate=\\s*([\\d\\.]+)kbits/s.*speed=\\s*([\\d\\.]+)x"
    );

    /**
     * Executes the command synchronously.
     */
    public static EncodingResult execute(List<String> args, 
                                       OnProgressListener progressListener, 
                                       OnStreamStatsListener statsListener, 
                                       long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            
            if (timeoutSeconds > 0) {
                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new TimeoutException(timeoutSeconds);
                }
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    parseAndNotify(line, progressListener, statsListener);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ExecutionException(exitCode, output.toString());
            }

            // For streaming destinations, the last arg might not be a file path we can check
            String lastArg = args.get(args.size() - 1);
            Path outputPath = lastArg.startsWith("rtp://") || lastArg.startsWith("udp://") 
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
     * Executes the command asynchronously.
     */
    public static CompletableFuture<EncodingResult> executeAsync(List<String> args, 
                                                               OnProgressListener progressListener, 
                                                               OnStreamStatsListener statsListener) {
        return CompletableFuture.supplyAsync(() -> execute(args, progressListener, statsListener, 0));
    }

    private static void parseAndNotify(String line, OnProgressListener progress, OnStreamStatsListener stats) {
        Matcher matcher = PROGRESS_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                long frame = Long.parseLong(matcher.group(1));
                double bitrateKbps = Double.parseDouble(matcher.group(2));
                double speed = Double.parseDouble(matcher.group(3));

                if (progress != null) {
                    progress.onProgress(0, frame, bitrateKbps);
                }
                
                if (stats != null) {
                    stats.onStatsUpdate((long)(bitrateKbps * 1000), speed, 0);
                    // Detection of congestion: if speed is significantly below 1.0 for a live stream
                    // (Note: in a real implementation we might want a grace period or average)
                }
            } catch (Exception ignored) {}
        }
    }
}
