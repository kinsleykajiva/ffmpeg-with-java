package io.github.kinsleykajiva.ffmpeg.model;

import java.net.URI;

/**
 * Represents a type-safe stream destination using URI.
 */
public record StreamDestination(URI uri) {
    public static StreamDestination rtp(String host, int port) {
        return new StreamDestination(URI.create("rtp://" + host + ":" + port));
    }
    
    public static StreamDestination srt(String host, int port) {
        return new StreamDestination(URI.create("srt://" + host + ":" + port));
    }
    
    public String toString() {
        return uri.toString();
    }
}
