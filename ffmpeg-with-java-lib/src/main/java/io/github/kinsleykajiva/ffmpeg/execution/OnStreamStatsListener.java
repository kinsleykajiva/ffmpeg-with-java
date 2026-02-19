package io.github.kinsleykajiva.ffmpeg.execution;

/**
 * Listener for real-time stream statistics.
 */
@FunctionalInterface
public interface OnStreamStatsListener {
    /**
     * Called with updated streaming metrics.
     * 
     * @param bitrate current bits per second
     * @param speed processing speed (1.0 is real-time)
     * @param droppedFrames frames dropped due to latency
     */
    void onStatsUpdate(long bitrate, double speed, int droppedFrames);
}
