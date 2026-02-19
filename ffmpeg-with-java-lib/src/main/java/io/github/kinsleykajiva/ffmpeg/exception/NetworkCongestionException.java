package io.github.kinsleykajiva.ffmpeg.exception;

/**
 * Thrown when streaming speed falls below real-time (1.0x).
 */
public final class NetworkCongestionException extends FFmpegException {
    public NetworkCongestionException(double speed) {
        super("Network congestion detected: speed is " + speed + " (expected >= 1.0)");
    }
}
