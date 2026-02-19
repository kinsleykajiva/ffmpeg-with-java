package io.github.kinsleykajiva.ffmpeg.model;

import java.nio.file.Path;

/**
 * Immutable record representing the result of an encoding job.
 */
public record EncodingResult(
    Path outputPath,
    long timeTakenMillis,
    long finalFileSize
) {
    public double fileSizeKB() {
        return finalFileSize / 1024.0;
    }

    public double fileSizeMB() {
        return finalFileSize / (1024.0 * 1024.0);
    }
}
