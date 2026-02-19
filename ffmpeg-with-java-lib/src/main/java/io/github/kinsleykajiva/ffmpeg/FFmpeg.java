package io.github.kinsleykajiva.ffmpeg;

import static io.github.kinsleykajiva.ffmpeg.ffmpeg_includes_h.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * A fluent API wrapper for FFmpeg operations using Project Panama.
 */
public class FFmpeg {
    static {
        loadLibraries();
    }

    private static boolean librariesLoaded = false;

    private static void loadLibraries() {
        if (librariesLoaded) return;

        // Core libs required for file-based decoding/encoding/probing
        String[] requiredLibs = {"avutil-60", "swresample-6", "avcodec-62", "avformat-62", "swscale-9", "avfilter-11"};
        // Optional: avdevice is only needed for camera/microphone device I/O
        String[] optionalLibs = {"avdevice-62"};

        java.io.File binDir = resolveFallbackBinDir();

        // Load required libraries
        for (String lib : requiredLibs) {
            if (!loadSingleLib(lib, binDir)) {
                System.err.println("Failed to load required FFmpeg library: " + lib);
                System.err.println("Tip: set -Djava.library.path=<path-to-ffmpeg-bin> in your VM options.");
                return;
            }
        }

        // Load optional libraries — failure is silently ignored
        for (String lib : optionalLibs) {
            loadSingleLib(lib, binDir); // ignore return value
        }

        librariesLoaded = true;
    }

    private static boolean loadSingleLib(String lib, java.io.File binDir) {
        try {
            System.loadLibrary(lib);
            return true;
        } catch (UnsatisfiedLinkError e1) {
            if (binDir != null && binDir.exists()) {
                try {
                    System.load(new java.io.File(binDir, lib + ".dll").getAbsolutePath());
                    return true;
                } catch (UnsatisfiedLinkError e2) {
                    // both paths failed
                }
            }
        }
        return false;
    }

    /**
     * Resolves the FFmpeg bin directory relative to this class's code source location.
     * Walks up to the project root and looks for ffmpeg-builds/win64/bin.
     */
    private static java.io.File resolveFallbackBinDir() {
        try {
            java.net.URL codeSource = FFmpeg.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File classLocation = new java.io.File(codeSource.toURI());
            // Walk up until we find the module root (contains pom.xml or ffmpeg-builds)
            java.io.File dir = classLocation;
            for (int i = 0; i < 6; i++) {
                java.io.File candidate = new java.io.File(dir, "ffmpeg-builds/win64/bin");
                if (candidate.exists()) return candidate;
                dir = dir.getParentFile();
                if (dir == null) break;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private final String inputPath;

    private FFmpeg(String inputPath) {
        this.inputPath = inputPath;
    }

    /**
     * Entry point for specifying an input file for metadata probing or processing.
     * Validates that the input path is non-null and points to an existing, readable file.
     * 
     * @param path The path to the input file.
     * @return A new FFmpeg instance.
     * @throws IllegalArgumentException if path is null or file is not accessible.
     */
    public static FFmpeg input(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Input path cannot be null.");
        }
        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + path);
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("Input file is not readable: " + path);
        }
        return new FFmpeg(path);
    }

    /**
     * Probes the input file for metadata and returns a structured record.
     * 
     * @return AudioMetadata containing format, duration, bitrate, etc.
     * @throws io.github.kinsleykajiva.ffmpeg.exception.FFmpegException if file cannot be opened
     */
    public io.github.kinsleykajiva.ffmpeg.model.AudioMetadata probe() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ppFormatCtx = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment pathSegment = arena.allocateFrom(inputPath);

            if (avformat_open_input(ppFormatCtx, pathSegment, MemorySegment.NULL, MemorySegment.NULL) != 0) {
                throw new io.github.kinsleykajiva.ffmpeg.exception.ExecutionException(-1, "Could not open input file: " + inputPath);
            }

            // Reinterpret the pointer with the correct struct size
            MemorySegment pFormatCtx = ppFormatCtx.get(ValueLayout.ADDRESS, 0)
                    .reinterpret(AVFormatContext.layout().byteSize());

            try {
                if (avformat_find_stream_info(pFormatCtx, MemorySegment.NULL) < 0) {
                    throw new io.github.kinsleykajiva.ffmpeg.exception.ExecutionException(-1, "Could not find stream info for: " + inputPath);
                }

                String format = AVInputFormat.name(AVFormatContext.iformat(pFormatCtx)).getString(0);
                double duration = AVFormatContext.duration(pFormatCtx) / (double) AV_TIME_BASE();
                long bitrate = AVFormatContext.bit_rate(pFormatCtx);

                // Extract tags
                Map<String, String> tags = new HashMap<>();
                MemorySegment metadata = AVFormatContext.metadata(pFormatCtx);
                if (!metadata.equals(MemorySegment.NULL)) {
                    MemorySegment entry = MemorySegment.NULL;
                    while (true) {
                        entry = av_dict_get(metadata, arena.allocateFrom(""), entry, AV_DICT_IGNORE_SUFFIX());
                        if (entry.equals(MemorySegment.NULL)) break;
                        entry = entry.reinterpret(AVDictionaryEntry.layout().byteSize());
                        
                        String key = AVDictionaryEntry.key(entry).getString(0);
                        String value = AVDictionaryEntry.value(entry).getString(0);
                        tags.put(key, value);
                    }
                }

                // Simplified: assuming audio for now
                return new io.github.kinsleykajiva.ffmpeg.model.AudioMetadata(
                    format,
                    duration,
                    bitrate,
                    0, // Default sample rate if not found in context directly
                    "unknown", // Default channel layout
                    tags
                );
            } finally {
                avformat_close_input(ppFormatCtx);
            }
        }
    }

    /**
     * Starts building an audio job for the current input.
     * This replaces the old convert() method with a fluent builder.
     */
    public io.github.kinsleykajiva.ffmpeg.builder.AudioJobBuilder output(String outputPath) {
        return new io.github.kinsleykajiva.ffmpeg.builder.AudioJobBuilder(inputPath, outputPath);
    }

    /**
     * Starts a live streaming builder directly.
     */
    public io.github.kinsleykajiva.ffmpeg.builder.AudioJobBuilder asLiveSource() {
        return new io.github.kinsleykajiva.ffmpeg.builder.AudioJobBuilder(inputPath, null).asLiveSource();
    }

    /**
     * Starts a streaming builder targeting a specific destination.
     */
    public io.github.kinsleykajiva.ffmpeg.builder.AudioJobBuilder toStream(io.github.kinsleykajiva.ffmpeg.model.StreamDestination destination) {
        return new io.github.kinsleykajiva.ffmpeg.builder.AudioJobBuilder(inputPath, null).toStream(destination);
    }

    // Deprecated methods for backward compatibility if needed, 
    // but moving towards the new builder pattern.
}


