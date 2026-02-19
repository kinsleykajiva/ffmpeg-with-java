package demo;

import io.github.kinsleykajiva.ffmpeg.FFmpeg;
import io.github.kinsleykajiva.ffmpeg.builder.AudioFilters;
import io.github.kinsleykajiva.ffmpeg.model.AudioCodec;
import io.github.kinsleykajiva.ffmpeg.model.EncodingResult;
import io.github.kinsleykajiva.ffmpeg.model.SampleRate;

import java.util.concurrent.CompletableFuture;

/**
 * Demo for high-performance audio transcoding using the fluent builder API.
 */
public class TranscodingDemo {
    public static void main(String[] args) {
        String inputPath = "media-files/song.mp3";
        String outputPath = "media-files/song_processed.opus";

        System.out.println("=== FFmpeg Audio Transcoding Demo ===");

        try {
            CompletableFuture<EncodingResult> future = FFmpeg.input(inputPath)
                .output(outputPath)
                .withCodec(AudioCodec.LIBOPUS)
                .withBitrate("128k")
                .withSampleRate(SampleRate.SR_48000)
                .addFilter(AudioFilters.volume(1.2))
                .onProgress((pct, frame, br) -> {
                    System.out.print("\r  Transcoding Progress: Frame " + frame + " | Bitrate: " + br + " kbps   ");
                })
                .executeAsync();

            EncodingResult result = future.join();
            
            System.out.println("\n\nTranscoding Complete:");
            System.out.println("  Output: " + result.outputPath());
            System.out.println("  Size: " + String.format("%.2f MB", result.fileSizeMB()));
            System.out.println("  Duration: " + result.timeTakenMillis() + " ms");

        } catch (Exception e) {
            System.err.println("Transcoding failed: " + e.getMessage());
        }
    }
}
