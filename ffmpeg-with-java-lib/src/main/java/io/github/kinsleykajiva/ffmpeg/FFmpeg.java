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

    private String inputPath;
    private String outputPath;

    private FFmpeg(String inputPath) {
        this.inputPath = inputPath;
    }

    public static FFmpeg input(String path) {
        return new FFmpeg(path);
    }

    public FFmpeg output(String path) {
        this.outputPath = path;
        return this;
    }

    /**
     * Probes the input file for metadata.
     */
    public Map<String, String> probe() {
        Map<String, String> metadataMap = new HashMap<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ppFormatCtx = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment pathSegment = arena.allocateFrom(inputPath);

            if (avformat_open_input(ppFormatCtx, pathSegment, MemorySegment.NULL, MemorySegment.NULL) == 0) {
                // Reinterpret the pointer with the correct struct size
                MemorySegment pFormatCtx = ppFormatCtx.get(ValueLayout.ADDRESS, 0)
                        .reinterpret(AVFormatContext.layout().byteSize());

                if (avformat_find_stream_info(pFormatCtx, MemorySegment.NULL) >= 0) {
                    MemorySegment metadata = AVFormatContext.metadata(pFormatCtx);
                    if (!metadata.equals(MemorySegment.NULL)) {
                        MemorySegment entry = MemorySegment.NULL;
                        while (true) {
                            entry = av_dict_get(metadata, arena.allocateFrom(""), entry, AV_DICT_IGNORE_SUFFIX());
                            if (entry.equals(MemorySegment.NULL)) break;
                            entry = entry.reinterpret(AVDictionaryEntry.layout().byteSize());
                            
                            String key = AVDictionaryEntry.key(entry).getString(0);
                            String value = AVDictionaryEntry.value(entry).getString(0);
                            metadataMap.put(key, value);
                        }
                    }
                }
                avformat_close_input(ppFormatCtx);
            }
        }
        return metadataMap;
    }

    /**
     * Converts the input file to the output format using the bundled ffmpeg executable.
     * The output format is determined by the file extension of the output path.
     */
    public boolean convert() {
        if (inputPath == null || outputPath == null) {
            throw new IllegalStateException("Input and Output paths must be set.");
        }
        System.out.println("Converting " + inputPath + " to " + outputPath + "...");
        return performConversion();
    }

    /**
     * Finds the ffmpeg executable — first checks PATH, then falls back to the
     * bundled ffmpeg-builds/win64/bin directory.
     */
    private static java.io.File resolveFfmpegExe() {
        // Check PATH first
        for (String dir : System.getenv("PATH").split(java.io.File.pathSeparator)) {
            java.io.File candidate = new java.io.File(dir.trim(), "ffmpeg.exe");
            if (candidate.isFile()) return candidate;
            // Also try without .exe on non-Windows (just in case)
            candidate = new java.io.File(dir.trim(), "ffmpeg");
            if (candidate.isFile()) return candidate;
        }
        // Fall back to bundled binary
        java.io.File binDir = resolveFallbackBinDir();
        if (binDir != null) {
            java.io.File exe = new java.io.File(binDir, "ffmpeg.exe");
            if (exe.isFile()) return exe;
        }
        return null;
    }

    /**
     * Actually converts the file by invoking the ffmpeg executable.
     * Uses: ffmpeg -y -i <input> <output>
     * The -y flag overwrites the output if it already exists.
     */
    private boolean performConversion() {
        java.io.File ffmpegExe = resolveFfmpegExe();
        if (ffmpegExe == null) {
            System.err.println("ffmpeg executable not found in PATH or bundled bin directory.");
            return false;
        }

        try {
            // Ensure the output directory exists
            java.io.File outputFile = new java.io.File(outputPath);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegExe.getAbsolutePath(),
                    "-y",           // overwrite output without asking
                    "-i", inputPath,
                    outputPath
            );
            pb.redirectErrorStream(true); // merge stderr into stdout

            Process process = pb.start();

            // Capture and optionally print ffmpeg output
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Uncomment the line below to see full ffmpeg output:
                    // System.out.println("[ffmpeg] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0) {
                return true;
            } else {
                System.err.println("ffmpeg exited with code " + exitCode + ". Output file was not created or is empty.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Conversion error: " + e.getMessage());
            return false;
        }
    }
}

