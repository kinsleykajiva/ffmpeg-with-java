package io.github.kinsleykajiva.ffmpeg.model;

/**
 * Standardized audio codecs for FFmpeg.
 */
public enum AudioCodec {
    LIBMP3LAME("libmp3lame"),
    LIBOPUS("libopus"),
    AAC("aac"),
    FLAC("flAC"),
    PCM_S16LE("pcm_s16le"),
    PCM_U8("pcm_u8");

    private final String codecName;

    AudioCodec(String codecName) {
        this.codecName = codecName;
    }

    public String getCodecName() {
        return codecName;
    }

    @Override
    public String toString() {
        return codecName;
    }
}
