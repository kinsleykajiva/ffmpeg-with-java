package io.github.kinsleykajiva.ffmpeg.model;

/**
 * Standard RTP Payload Types (PT).
 */
public final class RTPPayloadTypes {
    private RTPPayloadTypes() {}

    public static final int PCMU = 0;
    public static final int PCMA = 8;
    public static final int G722 = 9;
    public static final int L16_STEREO = 10;
    public static final int L16_MONO = 11;
    public static final int DVI4_8000 = 5;
    public static final int DVI4_16000 = 6;
    public static final int LPC = 7;
    
    // Dynamic Payload Types (96-127)
    public static final int OPUS = 96;
    public static final int AAC_LATM = 97;
}
