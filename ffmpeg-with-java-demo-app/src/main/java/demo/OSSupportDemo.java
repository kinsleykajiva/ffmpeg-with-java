package demo;

import io.github.kinsleykajiva.ffmpeg.FFmpeg;
import io.github.kinsleykajiva.ffmpeg.FFmpegBinary;

/**
 * Demo for verifying Cross-Platform OS detection and path mapping.
 */
public class OSSupportDemo {
    public static void main(String[] args) {
        System.out.println("=== FFmpeg Cross-Platform OS Support Demo ===");
        // /mnt/c/Users/Kinsley/IdeaProjects/ffmpeg-with-java-demo/ffmpeg-with-java-lib/ffmpeg-builds/linux64/bin
        FFmpeg.setBinPath("C:\\Users\\Kinsley\\IdeaProjects\\ffmpeg-with-java-demo\\ffmpeg-with-java-lib\\ffmpeg-builds\\win64\\bin");
        FFmpegBinary.OS os = FFmpegBinary.getCurrentOS();
        System.out.println("\nDetected Operating System: " + os.name());
        System.out.println("  Executable Extension: '" + os.getExeExtension() + "'");
        System.out.println("  Library Extension: '" + os.getLibExtension() + "'");

        try {
            System.out.println("\nResolving components for " + os.name() + "...");
            System.out.println("  FFmpeg EXE: " + FFmpegBinary.getFfmpeg().getAbsolutePath());
            System.out.println("  FFprobe EXE: " + FFmpegBinary.getFfprobe().getAbsolutePath());
            System.out.println("  Lib Example (avutil-60): " + FFmpegBinary.getLibraryFilename("avutil-60"));
            System.out.println("\nPASSED: Component naming resolved for " + os.name());
        } catch (Exception e) {
            System.err.println("\nFAILED: Could not locate binaries: " + e.getMessage());
        }
    }
}
