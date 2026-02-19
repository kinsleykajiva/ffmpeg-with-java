package io.github.kinsleykajiva.ffmpeg.exception;

/**
 * Thrown when an unsupported or invalid codec is specified.
 */
public final class CodecException extends FFmpegException {
    private final String codec;

    public CodecException(String codec) {
        super("Unsupported or invalid codec: " + codec);
        this.codec = codec;
    }

    public String getCodec() {
        return codec;
    }
}
