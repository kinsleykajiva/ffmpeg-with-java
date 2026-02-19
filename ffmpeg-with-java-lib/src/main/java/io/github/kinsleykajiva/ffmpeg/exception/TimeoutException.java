package io.github.kinsleykajiva.ffmpeg.exception;

/**
 * Thrown when an FFmpeg process times out.
 */
public final class TimeoutException extends FFmpegException {
    public TimeoutException(long timeoutSeconds) {
        super("FFmpeg process timed out after " + timeoutSeconds + " seconds.");
    }
}
