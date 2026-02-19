package io.github.kinsleykajiva.ffmpeg.model;

/**
 * Configuration for network-related streaming parameters.
 * 
 * @param ttl Time-to-live for multicast packets.
 * @param bufferSize Real-time buffer size (rtbufsize) in bytes.
 * @param noBuffer Whether to disable internal buffering (-fflags nobuffer).
 * @param rtcpport Port for RTCP control reports.
 */
public record NetworkConfig(
    int ttl,
    int bufferSize,
    boolean noBuffer,
    Integer rtcpport
) {
    public static NetworkConfig defaultLowLatency() {
        return new NetworkConfig(32, 2 * 1024 * 1024, true, null);
    }
}
