package io.github.kinsleykajiva.ffmpeg.exception;

/**
 * Thrown when a stream cannot be discovered or analyzed.
 */
public final class StreamDiscoveryException extends FFmpegException {
    public StreamDiscoveryException(String message) {
        super(message);
    }
}
