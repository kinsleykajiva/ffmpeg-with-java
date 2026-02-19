package io.github.kinsleykajiva.ffmpeg;

import io.github.kinsleykajiva.ffmpeg.exception.BinaryNotFoundException;
import java.io.File;
import java.net.URL;

/**
 * Utility to locate and validate FFmpeg and FFprobe binaries.
 */
public class FFmpegBinary {

    private static File ffmpegExe;
    private static File ffprobeExe;
    private static String customBinPath;

    private static final String[] REQUIRED_DLLS = {
        "avutil-60.dll", "swresample-6.dll", "avcodec-62.dll", 
        "avformat-62.dll", "swscale-9.dll", "avfilter-11.dll"
    };

    /**
     * Set a global directory path where FFmpeg binaries and DLLs are located.
     * This path is validated immediately for all required files.
     */
    public static void setBinPath(String path) {
        if (path == null) throw new IllegalArgumentException("Bin path cannot be null.");
        File dir = new File(path);
        validate(dir);
        customBinPath = path;
        // Reset cached file objects to force re-resolve from new path
        ffmpegExe = null;
        ffprobeExe = null;
    }

    public static String getBinPath() {
        return customBinPath;
    }

    public static File getFfmpeg() {
        if (ffmpegExe == null) {
            ffmpegExe = resolveBinary("ffmpeg.exe", "ffmpeg");
        }
        return ffmpegExe;
    }

    public static File getFfprobe() {
        if (ffprobeExe == null) {
            ffprobeExe = resolveBinary("ffprobe.exe", "ffprobe");
        }
        return ffprobeExe;
    }

    private static File resolveBinary(String exeName, String name) {
        // 1. Check custom path if set
        if (customBinPath != null) {
            File candidate = new File(customBinPath, exeName);
            if (candidate.isFile() && candidate.canExecute()) return candidate;
        }

        // 2. Check PATH
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                File candidate = new File(dir.trim(), exeName);
                if (candidate.isFile() && candidate.canExecute()) return candidate;
                
                // For Linux/Mac
                if (!exeName.equals(name)) {
                   candidate = new File(dir.trim(), name);
                   if (candidate.isFile() && candidate.canExecute()) return candidate;
                }
            }
        }

        // 3. Fall back to bundled binary
        File binDir = resolveBundledBinDir();
        if (binDir != null) {
            File exe = new File(binDir, exeName);
            if (exe.isFile() && exe.canExecute()) return exe;
        }

        throw new BinaryNotFoundException(name, (customBinPath != null ? customBinPath : "PATH or bundled bin directory"));
    }

    private static void validate(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            throw new BinaryNotFoundException("bin-directory", dir.getAbsolutePath());
        }

        java.util.List<String> missing = new java.util.ArrayList<>();
        
        // Check executables
        if (!new File(dir, "ffmpeg.exe").exists() && !new File(dir, "ffmpeg").exists()) missing.add("ffmpeg");
        if (!new File(dir, "ffprobe.exe").exists() && !new File(dir, "ffprobe").exists()) missing.add("ffprobe");

        // Check DLLs (Windows specific for now as requested)
        for (String dll : REQUIRED_DLLS) {
            if (!new File(dir, dll).exists()) {
                missing.add(dll);
            }
        }

        if (!missing.isEmpty()) {
            throw new io.github.kinsleykajiva.ffmpeg.exception.BinaryNotFoundException(
                "FFmpeg Components", 
                "Missing files in " + dir.getAbsolutePath() + ": " + String.join(", ", missing)
            );
        }
    }

    private static File resolveBundledBinDir() {
        if (customBinPath != null) return new File(customBinPath);
        try {
            URL codeSource = FFmpegBinary.class.getProtectionDomain().getCodeSource().getLocation();
            File classLocation = new File(codeSource.toURI());
            
            File dir = classLocation;
            for (int i = 0; i < 6; i++) {
                File candidate = new File(dir, "ffmpeg-builds/win64/bin");
                if (candidate.exists()) return candidate;
                dir = dir.getParentFile();
                if (dir == null) break;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
