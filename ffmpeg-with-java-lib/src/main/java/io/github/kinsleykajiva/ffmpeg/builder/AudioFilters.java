package io.github.kinsleykajiva.ffmpeg.builder;

import io.github.kinsleykajiva.ffmpeg.model.SampleRate;

/**
 * Static factory for common FFmpeg audio filters.
 */
public final class AudioFilters {
    private AudioFilters() {}

    /**
     * Adjusts the volume. 
     * @param volume 1.0 is original, 2.0 is double, 0.5 is half. Max 10.0.
     * @throws IllegalArgumentException if volume is negative or exceeds 10.0.
     */
    public static String volume(double volume) {
        if (volume < 0 || volume > 10.0) {
            throw new IllegalArgumentException("Volume must be between 0.0 and 10.0. Provided: " + volume);
        }
        return "volume=" + volume;
    }

    /**
     * Trims the audio.
     * @param start start time in seconds
     * @param duration duration in seconds
     */
    public static String trim(double start, double duration) {
        return "atrim=start=" + start + ":duration=" + duration;
    }

    /**
     * Resamples the audio to a specific rate.
     */
    public static String resample(SampleRate rate) {
        return "aresample=" + rate.getRate();
    }
}
