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
        // 1. Check PATH
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

        // 2. Fall back to bundled binary
        File binDir = resolveBundledBinDir();
        if (binDir != null) {
            File exe = new File(binDir, exeName);
            if (exe.isFile() && exe.canExecute()) return exe;
        }

        throw new BinaryNotFoundException(name, "PATH or bundled bin directory");
    }

    private static File resolveBundledBinDir() {
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
