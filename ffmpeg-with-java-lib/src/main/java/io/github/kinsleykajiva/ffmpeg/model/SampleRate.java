package io.github.kinsleykajiva.ffmpeg.model;

/**
 * Standardized sample rates.
 */
public enum SampleRate {
    SR_8000(8000),
    SR_16000(16000),
    SR_22050(22050),
    SR_44100(44100),
    SR_48000(48000),
    SR_96000(96000);

    private final int rate;

    SampleRate(int rate) {
        this.rate = rate;
    }

    public int getRate() {
        return rate;
    }

    @Override
    public String toString() {
        return String.valueOf(rate);
    }
}
