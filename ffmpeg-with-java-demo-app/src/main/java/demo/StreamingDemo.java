package demo;

import io.github.kinsleykajiva.ffmpeg.FFmpeg;
import io.github.kinsleykajiva.ffmpeg.model.AudioCodec;
import io.github.kinsleykajiva.ffmpeg.model.NetworkConfig;
import io.github.kinsleykajiva.ffmpeg.model.StreamDestination;
import io.github.kinsleykajiva.ffmpeg.exception.TimeoutException;

import java.nio.file.Path;

/**
 * Demo for Real-Time RTP streaming with low-latency tuning and monitoring.
 */
public class StreamingDemo {
    public static void main(String[] args) {
        String inputPath = "media-files/song.mp3";
        String sdpPath = "media-files/stream.sdp";

        System.out.println("=== FFmpeg Real-Time Streaming Demo ===");

        try {
            System.out.println("Starting RTP stream to 127.0.0.1:5004...");
            
            FFmpeg.input(inputPath)
                .asLiveSource()                      // -re, fflags nobuffer
                .withInstantStartup()                // near-zero probe/analyze
                .withCodec(AudioCodec.LIBOPUS)
                .withBitrate("96k")
                .withNetworkConfig(new NetworkConfig(32, 2*1024*1024, true, 5005))
                .toStream(StreamDestination.rtp("127.0.0.1", 5004))
                .saveSdpTo(Path.of(sdpPath))
                .onStreamStats((br, speed, dropped) -> {
                    System.out.print("\r  Stream Stats: " + (br/1000) + " kbps | Speed: " + speed + "x   ");
                    if (speed < 1.0) System.err.print(" [NETWORK LAG] ");
                })
                .timeout(10) // Stream for 10 seconds
                .executeAsync()
                .handle((res, ex) -> {
                    if (ex != null && ex.getCause() instanceof TimeoutException) {
                        System.out.println("\r\nStreaming session finished successfully (timeout reached).");
                    } else if (ex != null) {
                        System.err.println("\r\nStreaming failed: " + ex.getMessage());
                    }
                    return null;
                }).join();

            System.out.println("SDP file generated at: " + sdpPath);
            System.out.println("You can open this SDP file in VLC to listen to the stream.");

        } catch (Exception e) {
            System.err.println("Streaming demo error: " + e.getMessage());
        }
    }
}
