package io.github.kinsleykajiva.ffmpeg.exception;

/**
 * Thrown when FFmpeg or FFprobe binaries cannot be located.
 */
public final class BinaryNotFoundException extends FFmpegException {
    public BinaryNotFoundException(String binaryName, String path) {
        super("Could not find " + binaryName + " binary at: " + path);
    }
    
    public BinaryNotFoundException(String message) {
        super(message);
    }
}
