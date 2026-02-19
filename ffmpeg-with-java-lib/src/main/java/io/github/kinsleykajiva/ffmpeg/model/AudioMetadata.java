package io.github.kinsleykajiva.ffmpeg.model;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable record representing audio stream metadata.
 */
public record AudioMetadata(
    String format,
    double durationSeconds,
    long bitrate,
    int sampleRate,
    String channelLayout,
    Map<String, String> tags
) {
    public AudioMetadata {
        tags = Collections.unmodifiableMap(tags);
    }

    public long durationMillis() {
        return (long) (durationSeconds * 1000);
    }
}
