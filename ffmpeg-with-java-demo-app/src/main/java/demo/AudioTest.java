package demo;

import io.github.kinsleykajiva.ffmpeg.FFmpeg;
import io.github.kinsleykajiva.ffmpeg.builder.AudioFilters;
import io.github.kinsleykajiva.ffmpeg.model.AudioCodec;
import io.github.kinsleykajiva.ffmpeg.model.AudioMetadata;
import io.github.kinsleykajiva.ffmpeg.model.SampleRate;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class AudioTest {
    public static void main(String[] args) {
        System.out.println("=== FFmpeg Java Modular Demo Suite ===");
        
        // Run all demos sequentially
        MetadataDemo.main(args);
        System.out.println("\n-----------------------------------\n");
        ComprehensiveMetadataDemo.main(args);
        System.out.println("\n-----------------------------------\n");
        ValidationDemo.main(args);
        System.out.println("\n-----------------------------------\n");
        ConfigDemo.main(args);
        System.out.println("\n-----------------------------------\n");
        TranscodingDemo.main(args);
        System.out.println("\n-----------------------------------\n");
        StreamingDemo.main(args);
        
        System.out.println("\n=== All Samples Completed ===");
    }
}


