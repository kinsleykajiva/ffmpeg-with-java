package io.github.kinsleykajiva.ffmpeg;

import java.io.File;

/**
 * A fluent API wrapper for FFmpeg operations.
 * Metadata probing uses ffprobe CLI (cross-platform).
 * Native Panama bindings are loaded optionally for advanced use cases.
 */
public class FFmpeg {
    static {
        loadLibraries();
    }

    private static boolean librariesLoaded = false;

    private static void loadLibraries() {
        if (librariesLoaded) return;

        // Core libs for Panama-based features (optional, non-fatal)
        String[] requiredLibs = {"avutil-60", "swresample-6", "avcodec-62", "avformat-62", "swscale-9", "avfilter-11"};
        String[] optionalLibs = {"avdevice-62"};

        File binDir = resolveFallbackBinDir();

        boolean allLoaded = true;
        for (String lib : requiredLibs) {
            if (!loadSingleLib(lib, binDir)) {
                allLoaded = false;
                break;
            }
        }

        if (!allLoaded) {
            // Non-fatal: Panama features are unavailable, but CLI-based features work fine
            System.err.println("[FFmpeg] Native libraries not loaded. Panama features unavailable.");
            System.err.println("[FFmpeg] CLI-based features (probe, transcode, stream) are fully operational.");
            return;
        }

        // Load optional libraries (best-effort)
        for (String lib : optionalLibs) {
            loadSingleLib(lib, binDir);
        }

        librariesLoaded = true;
    }

    /**
     * Programmatically sets the path to FFmpeg binaries and libraries.
     * This must be called before any FFmpeg operations.
     */
    public static void setBinPath(String path) {
        FFmpegBinary.setBinPath(path);
        // Retry loading native libraries with the new path
        loadLibraries();
    }

    private static boolean loadSingleLib(String lib, java.io.File binDir) {
        try {
            System.loadLibrary(lib);
            return true;
        } catch (UnsatisfiedLinkError e1) {
            String libFilename = FFmpegBinary.getLibraryFilename(lib);
            
            // If binDir is provided, use our flexible search
            if (binDir != null && binDir.exists()) {
                java.io.File foundLib = FFmpegBinary.findFile(binDir, libFilename);
                if (foundLib != null) {
                    try {
                        System.load(foundLib.getAbsolutePath());
                        return true;
                    } catch (UnsatisfiedLinkError e2) {
                        // failed to load found file
                    }
                }
            }

            // On Linux, also search standard system library directories
            if (FFmpegBinary.getCurrentOS() == FFmpegBinary.OS.LINUX) {
                String[] systemLibPaths = {
                    "/usr/lib/x86_64-linux-gnu",
                    "/usr/lib64",
                    "/usr/lib",
                    "/usr/local/lib",
                    "/usr/local/lib64",
                    "/usr/lib/aarch64-linux-gnu"
                };
                for (String path : systemLibPaths) {
                    java.io.File sysDir = new java.io.File(path);
                    if (sysDir.exists() && sysDir.isDirectory()) {
                        // 1. Try exact (or prefix versioned)
                        java.io.File foundLib = FFmpegBinary.findFile(sysDir, libFilename);
                        
                        // 2. Try base name fallback if exact fails
                        if (foundLib == null && libFilename.contains(".so.")) {
                            String baseName = libFilename.substring(0, libFilename.indexOf(".so") + 3);
                            foundLib = FFmpegBinary.findFile(sysDir, baseName);
                        }

                        if (foundLib != null) {
                            try {
                                System.load(foundLib.getAbsolutePath());
                                return true;
                            } catch (UnsatisfiedLinkError e3) {
                                // failed to load from system path
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static java.io.File resolveFallbackBinDir() {
        // 1. Check custom path if set
        if (io.github.kinsleykajiva.ffmpeg.FFmpegBinary.getBinPath() != null) {
            return new java.io.File(io.github.kinsleykajiva.ffmpeg.FFmpegBinary.getBinPath());
        }

        // 2. Fall back to auto-detection (searching up the tree for ffmpeg-builds)
        try {
            java.net.URL codeSource = FFmpeg.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File classLocation = new java.io.File(codeSource.toURI());
            java.io.File dir = classLocation;
            for (int i = 0; i < 6; i++) {
                java.io.File candidate = new java.io.File(dir, "ffmpeg-builds");
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
     * Probes the input file for metadata using ffprobe CLI (cross-platform).
     * Returns a structured AudioMetadata record with format, duration, bitrate,
     * sample rate, channel layout, and all tags.
     * 
     * @return AudioMetadata containing format, duration, bitrate, etc.
     * @throws io.github.kinsleykajiva.ffmpeg.exception.ExecutionException if ffprobe fails
     */
    public io.github.kinsleykajiva.ffmpeg.model.AudioMetadata probe() {
        return FFprobeJsonProber.probe(inputPath);
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


