package io.github.kinsleykajiva.ffmpeg.execution;

/**
 * Functional interface for tracking FFmpeg progress.
 */
@FunctionalInterface
public interface OnProgressListener {
    /**
     * Called when FFmpeg reports progress.
     * @param percentage completion percentage (0.0 to 100.0)
     * @param frame current frame number
     * @param bitrate current processing bitrate
     */
    void onProgress(double percentage, long frame, double bitrate);
}
