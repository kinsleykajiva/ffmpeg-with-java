package demo;

import io.github.kinsleykajiva.ffmpeg.FFmpeg;
import io.github.kinsleykajiva.ffmpeg.model.AudioMetadata;

/**
 * Demo for probing audio metadata using Project Panama bindings.
 */
public class MetadataDemo {
    public static void main(String[] args) {
        String inputPath = "media-files/song.mp3";
        
        System.out.println("=== FFmpeg Metadata Probing Demo ===");
        
        try {
            AudioMetadata metadata = FFmpeg.input(inputPath).probe();
            
            System.out.println("File: " + inputPath);
            System.out.println("Format: " + metadata.format());
            System.out.println("Duration: " + metadata.durationSeconds() + " seconds");
            System.out.println("Bitrate: " + (metadata.bitrate() / 1000) + " kbps");
            
            System.out.println("\nTags:");
            metadata.tags().forEach((k, v) -> System.out.println("  " + k + ": " + v));
            
        } catch (Exception e) {
            System.err.println("Error probing metadata: " + e.getMessage());
        }
    }
}
