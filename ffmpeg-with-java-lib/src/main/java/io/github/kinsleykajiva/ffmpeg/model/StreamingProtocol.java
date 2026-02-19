package io.github.kinsleykajiva.ffmpeg.model;

/**
 * Supported protocols for real-time streaming.
 */
public enum StreamingProtocol {
    RTP("rtp"), 
    UDP("udp"), 
    SRT("srt"), 
    RTSP("rtsp");

    private final String protocol;

    StreamingProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }
}
