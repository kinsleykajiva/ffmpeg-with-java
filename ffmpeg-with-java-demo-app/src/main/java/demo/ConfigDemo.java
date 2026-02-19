package demo;

import io.github.kinsleykajiva.ffmpeg.FFmpeg;
import io.github.kinsleykajiva.ffmpeg.exception.BinaryNotFoundException;
import io.github.kinsleykajiva.ffmpeg.model.AudioMetadata;

/**
 * Demo for programmatically configuring the FFmpeg binary path.
 */
public class ConfigDemo {
    public static void main(String[] args) {
        System.out.println("=== FFmpeg Global Path Configuration Demo ===");

        
        try {
            FFmpeg.setBinPath("C:\\Users\\Kinsley\\IdeaProjects\\ffmpeg-with-java-demo\\ffmpeg-with-java-lib\\ffmpeg-builds\\win64\\bin");
            System.out.println("Successfully set FFmpeg binary path.");
        } catch (BinaryNotFoundException e) {
            System.err.println("PASSED: Caught expected error for invalid path:");
            System.err.println("  Message: " + e.getMessage());
        }
    }
}
