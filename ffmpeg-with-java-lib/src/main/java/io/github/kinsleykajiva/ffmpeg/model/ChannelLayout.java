package io.github.kinsleykajiva.ffmpeg.model;

/**
 * Standardized channel layouts.
 */
public enum ChannelLayout {
    MONO("1", "mono"),
    STEREO("2", "stereo"),
    SURROUND_5_1("6", "5.1");

    private final String channels;
    private final String name;

    ChannelLayout(String channels, String name) {
        this.channels = channels;
        this.name = name;
    }

    public String getChannels() {
        return channels;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
