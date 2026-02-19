package demo;

import io.github.kinsleykajiva.ffmpeg.FFmpeg;
import io.github.kinsleykajiva.ffmpeg.builder.AudioFilters;

/**
 * Demo showcasing the defensive validation layer of the library.
 */
public class ValidationDemo {
    public static void main(String[] args) {
        System.out.println("=== FFmpeg Defensive Validation Demo ===");

        // 1. Invalid Input Path
        testValidation("Non-existent File", () -> FFmpeg.input("ghost_file.mp3"));

        // 2. Out of Range Volume
        testValidation("Volume Out of Range (15.0)", () -> AudioFilters.volume(15.0));

        // 3. Invalid Bitrate Format
        testValidation("Malformed Bitrate ('high')", () -> 
            FFmpeg.input("media-files/song.mp3").output("test.wav").withBitrate("high")
        );
    }

    private static void testValidation(String label, Runnable task) {
        try {
            System.out.print("Testing " + label + ": ");
            task.run();
            System.out.println("FAILED (Should have thrown exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("PASSED (Caught expected error: " + e.getMessage() + ")");
        }
    }
}
