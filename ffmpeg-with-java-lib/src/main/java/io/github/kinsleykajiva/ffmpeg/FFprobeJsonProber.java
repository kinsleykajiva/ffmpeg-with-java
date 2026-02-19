package io.github.kinsleykajiva.ffmpeg;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kinsleykajiva.ffmpeg.exception.ExecutionException;
import io.github.kinsleykajiva.ffmpeg.model.AudioMetadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cross-platform metadata prober that uses the ffprobe CLI with JSON output.
 * This avoids the need for platform-specific Panama bindings, making
 * metadata extraction work on Windows, Linux, and macOS.
 */
public final class FFprobeJsonProber {

    private static final Gson GSON = new Gson();

    private FFprobeJsonProber() {}

    /**
     * Probes an audio file using ffprobe and returns structured metadata.
     *
     * @param inputPath Path to the audio file to probe.
     * @return A fully populated AudioMetadata record.
     * @throws ExecutionException if ffprobe fails or produces invalid output.
     */
    public static AudioMetadata probe(String inputPath) {
        File ffprobe = FFmpegBinary.getFfprobe();

        // Resolve to absolute path so ffprobe subprocess can always find the file
        File inputFile = new File(inputPath);
        String resolvedPath = inputFile.getAbsolutePath();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                ffprobe.getAbsolutePath(),
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                resolvedPath
            );

            Process process = pb.start();

            // Capture stdout (JSON) and stderr (errors) separately
            String jsonOutput;
            String stderrOutput;
            try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                jsonOutput = stdoutReader.lines().collect(Collectors.joining("\n"));
                stderrOutput = stderrReader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || jsonOutput.isBlank()) {
                String detail = stderrOutput.isBlank() ? "no output" : stderrOutput;
                throw new ExecutionException(exitCode, 
                    "ffprobe failed for: " + resolvedPath + " (exit code: " + exitCode + ")\n" + detail);
            }

            return parseJson(jsonOutput);

        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecutionException(-1, "Failed to execute ffprobe: " + e.getMessage());
        }
    }

    private static AudioMetadata parseJson(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);

        // --- Extract from "format" ---
        JsonObject format = root.getAsJsonObject("format");
        String formatName = getStringOrDefault(format, "format_name", "unknown");
        double duration = getDoubleOrDefault(format, "duration", 0.0);
        long bitrate = getLongOrDefault(format, "bit_rate", 0L);

        // --- Extract tags from format ---
        Map<String, String> tags = new HashMap<>();
        if (format != null && format.has("tags")) {
            JsonObject tagsObj = format.getAsJsonObject("tags");
            for (Map.Entry<String, JsonElement> entry : tagsObj.entrySet()) {
                tags.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        // --- Extract from first audio stream ---
        int sampleRate = 0;
        String channelLayout = "unknown";

        if (root.has("streams")) {
            JsonArray streams = root.getAsJsonArray("streams");
            for (JsonElement streamEl : streams) {
                JsonObject stream = streamEl.getAsJsonObject();
                String codecType = getStringOrDefault(stream, "codec_type", "");
                if ("audio".equals(codecType)) {
                    sampleRate = getIntOrDefault(stream, "sample_rate", 0);
                    channelLayout = getStringOrDefault(stream, "channel_layout", 
                        getIntOrDefault(stream, "channels", 0) + "ch");
                    break; // Use first audio stream
                }
            }
        }

        return new AudioMetadata(formatName, duration, bitrate, sampleRate, channelLayout, tags);
    }

    // --- Utility helpers for safe JSON extraction ---

    private static String getStringOrDefault(JsonObject obj, String key, String defaultVal) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultVal;
    }

    private static double getDoubleOrDefault(JsonObject obj, String key, double defaultVal) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            try { return Double.parseDouble(obj.get(key).getAsString()); } catch (NumberFormatException e) { /* ignore */ }
        }
        return defaultVal;
    }

    private static long getLongOrDefault(JsonObject obj, String key, long defaultVal) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            try { return Long.parseLong(obj.get(key).getAsString()); } catch (NumberFormatException e) { /* ignore */ }
        }
        return defaultVal;
    }

    private static int getIntOrDefault(JsonObject obj, String key, int defaultVal) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            try { return Integer.parseInt(obj.get(key).getAsString()); } catch (NumberFormatException e) { /* ignore */ }
        }
        return defaultVal;
    }
}
