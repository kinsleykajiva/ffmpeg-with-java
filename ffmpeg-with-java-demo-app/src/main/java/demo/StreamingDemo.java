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
        FFmpeg.setBinPath("C:\\Users\\Kinsley\\IdeaProjects\\ffmpeg-with-java-demo\\ffmpeg-with-java-lib\\ffmpeg-builds\\win64\\bin");
        
        try {
            System.out.println("Starting RTP stream to 127.0.0.1:5004...");
            
            FFmpeg.input(inputPath)
                .asLiveSource()                      // -re, fflags nobuffer
                .withWallclock(false)                // Ensure no wallclock interference
                .withReadRate(1.0)                   // Precise 1.0x pacing
                .withInstantStartup()                // near-zero probe/analyze
                .withCodec(AudioCodec.LIBOPUS)
                .withBitrate("96k")
                .withNetworkConfig(new NetworkConfig(32, 2*1024*1024, true, 5005))
                .toStream(StreamDestination.rtp("127.0.0.1", 5004))
                .saveSdpTo(Path.of(sdpPath))
                .onStreamStats((br, speed, dropped) -> {
                    IO.print("\r  Stream Stats: " + (br/1000) + " kbps | Speed: " + speed + "x   ");
                    IO.println(" | Dropped Packets: " + dropped);
                    if (speed < 1.0) System.err.print(" [NETWORK LAG] ");
                })
               // .timeout(10) // Stream for 10 seconds
                .executeAsync()
                .handle((res, ex) -> {
                    if (ex != null && ex.getCause() instanceof TimeoutException) {
                        IO.println("\r\nStreaming session finished successfully (timeout reached).");
                    } else if (ex != null) {
                        IO.println("\r\nStreaming failed: " + ex.getMessage());
                    }
                    return null;
                }).join();
            
            IO.println("SDP file generated at: " + sdpPath);
            IO.println("You can open this SDP file in VLC to listen to the stream.");

        } catch (Exception e) {
            System.err.println("Streaming demo error: " + e.getMessage());
        }
    }
}
