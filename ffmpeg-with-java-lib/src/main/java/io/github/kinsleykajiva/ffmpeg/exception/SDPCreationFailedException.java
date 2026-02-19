package io.github.kinsleykajiva.ffmpeg.exception;

/**
 * Thrown when SDP file creation fails.
 */
public final class SDPCreationFailedException extends FFmpegException {
    public SDPCreationFailedException(String path, String error) {
        super("Failed to create SDP file at " + path + ": " + error);
    }
}
