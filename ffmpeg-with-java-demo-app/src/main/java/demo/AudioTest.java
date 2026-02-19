package demo;

import io.github.kinsleykajiva.ffmpeg.FFmpeg;
import java.io.File;
import java.util.Map;

public class AudioTest {
    public static void main(String[] args) {
        String inputPath = "media-files/song.mp3";
        String outputPath = "media-files/song.wav";

        System.out.println("Starting FFmpeg Fluent API Test...");

        // 1. Probe Metadata
        // Note: tags like 'encoded_by' and 'date' are embedded inside the source MP3 file itself.
        // They describe how/when the original file was created, not something we set.
        System.out.println("\n--- Probing Source Metadata (embedded in " + inputPath + ") ---");
        Map<String, String> metadata = FFmpeg.input(inputPath).probe();
        if (metadata.isEmpty()) {
            System.out.println("No metadata found or could not open file.");
        } else {
            metadata.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        }

        // 2. Convert MP3 to WAV
        System.out.println("\n--- Converting MP3 to WAV ---");
        boolean success = FFmpeg.input(inputPath)
                                .output(outputPath)
                                .convert();

        // Verify the output file was physically created
        File outputFile = new File(outputPath);
        if (success && outputFile.exists() && outputFile.length() > 0) {
            System.out.printf("Conversion successful: %s (%.1f KB)%n",
                    outputPath, outputFile.length() / 1024.0);
        } else if (success) {
            System.out.println("WARNING: convert() returned true but output file is missing or empty!");
        } else {
            System.out.println("Conversion failed. Check ffmpeg output above.");
        }
    }
}

