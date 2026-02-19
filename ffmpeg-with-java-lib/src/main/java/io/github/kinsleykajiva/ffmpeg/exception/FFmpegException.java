package io.github.kinsleykajiva.ffmpeg.exception;

/**
 * Base sealed class for all FFmpeg-related exceptions.
 * Permits specific subclasses for granular error handling.
 */
public sealed class FFmpegException extends RuntimeException 
    permits BinaryNotFoundException, CodecException, ExecutionException, TimeoutException, 
            NetworkCongestionException, StreamDiscoveryException, SDPCreationFailedException {
    
    public FFmpegException(String message) {
        super(message);
    }

    public FFmpegException(String message, Throwable cause) {
        super(message, cause);
    }
}
