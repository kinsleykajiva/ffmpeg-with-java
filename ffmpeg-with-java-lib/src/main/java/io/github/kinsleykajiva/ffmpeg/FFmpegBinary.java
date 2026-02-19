package io.github.kinsleykajiva.ffmpeg;

import java.io.File;
import java.net.URL;

import io.github.kinsleykajiva.ffmpeg.exception.BinaryNotFoundException;

/**
 * Utility to locate and validate FFmpeg and FFprobe binaries.
 */
public class FFmpegBinary {

    private static File ffmpegExe;
    private static File ffprobeExe;
    private static String customBinPath;

    public enum OS {
        WINDOWS(".exe", ".dll", "", ""),
        LINUX("", ".so", "lib", "."),
        MACOS("", ".dylib", "lib", ".");

        private final String exeExtension;
        private final String libExtension;
        private final String libPrefix;
        private final String libVersionSeparator;

        OS(String exeExtension, String libExtension, String libPrefix, String libVersionSeparator) {
            this.exeExtension = exeExtension;
            this.libExtension = libExtension;
            this.libPrefix = libPrefix;
            this.libVersionSeparator = libVersionSeparator;
        }

        public String getExeExtension() { return exeExtension; }
        public String getLibExtension() { return libExtension; }
        public String getLibPrefix() { return libPrefix; }
        public String getLibVersionSeparator() { return libVersionSeparator; }
    }

    private static final OS CURRENT_OS = detectOS();

    private static OS detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return OS.WINDOWS;
        if (os.contains("mac")) return OS.MACOS;
        return OS.LINUX;
    }

    private static final String[] REQUIRED_LIBS = {
        "avutil-60", "swresample-6", "avcodec-62", 
        "avformat-62", "swscale-9", "avfilter-11"
    };

    /**
     * Standard system library directories on Linux (Ubuntu/Debian/Fedora/Arch).
     * Used as fallback when libraries are not found in the setBinPath directory.
     */
    private static final String[] LINUX_LIB_SEARCH_PATHS = {
        "/usr/lib/x86_64-linux-gnu",
        "/usr/lib/i386-linux-gnu",
        "/usr/lib/aarch64-linux-gnu",
        "/usr/lib/arm-linux-gnueabihf",
        "/usr/lib64",
        "/usr/lib",
        "/usr/local/lib",
        "/usr/local/lib64"
    };

    /**
     * Resolves a library base name (e.g., "avutil-60") to a platform-specific filename.
     * Examples: 
     * - Windows: avutil-60.dll
     * - Linux:   libavutil.so.60
     * - MacOS:   libavutil.60.dylib
     */
    public static String getLibraryFilename(String baseName) {
        String[] parts = baseName.split("-");
        String name = parts[0];
        String version = parts.length > 1 ? parts[1] : "";

        return switch (CURRENT_OS) {
            case WINDOWS -> baseName + ".dll";
            case LINUX -> "lib" + name + ".so" + (version.isEmpty() ? "" : "." + version);
            case MACOS -> "lib" + name + (version.isEmpty() ? "" : "." + version) + ".dylib";
        };
    }

    /**
     * Set a global directory path where FFmpeg binaries and DLLs are located.
     * This path is validated immediately for all required files.
     */
    public static void setBinPath(String path) {
        if (path == null) throw new IllegalArgumentException("Bin path cannot be null.");
        File dir = new File(path);
        validate(dir);
        customBinPath = path;
        // Reset cached file objects
        ffmpegExe = null;
        ffprobeExe = null;
    }

    public static String getBinPath() {
        return customBinPath;
    }

    public static OS getCurrentOS() {
        return CURRENT_OS;
    }

    public static File getFfmpeg() {
        if (ffmpegExe == null) {
            ffmpegExe = resolveBinary("ffmpeg" + CURRENT_OS.getExeExtension(), "ffmpeg");
        }
        return ffmpegExe;
    }

    public static File getFfprobe() {
        if (ffprobeExe == null) {
            ffprobeExe = resolveBinary("ffprobe" + CURRENT_OS.getExeExtension(), "ffprobe");
        }
        return ffprobeExe;
    }

    /**
     * Performs a shallow recursive search for a file within a root directory.
     * Max depth 2: Search root, immediate subdirs (bin, lib, etc.).
     * Skips zero-byte files (often broken symlinks on Windows-hosted Linux builds).
     */
    public static File findFile(File root, String targetName) {
        return findFileRecursive(root, targetName, 0, 2);
    }

    private static File findFileRecursive(File dir, String targetName, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory()) return null;

        // 1. Check immediate directory for exact match (must be > 0 bytes)
        File candidate = new File(dir, targetName);
        if (candidate.isFile() && candidate.length() > 0) return candidate;

        // 2. If it's a library, try versioned variants (e.g., name.so.1.2.3)
        if (targetName.contains(".so") || targetName.contains(".dylib") || targetName.contains(".dll")) {
            File[] matches = dir.listFiles((d, name) -> name.startsWith(targetName) && name.length() > targetName.length());
            if (matches != null && matches.length > 0) {
                // Return the first file that has content
                for (File match : matches) {
                    if (match.isFile() && match.length() > 0) return match;
                }
            }
        }

        // 3. Check subdirectories
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    // Skip hidden or uninformative directories
                    if (child.getName().startsWith(".")) continue;
                    
                    File found = findFileRecursive(child, targetName, currentDepth + 1, maxDepth);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static File resolveBinary(String exeName, String name) {
        // 1. Check custom path if set
        if (customBinPath != null) {
            File found = findFile(new File(customBinPath), exeName);
            if (found != null) return found;
        }

        // 2. Check PATH
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                File candidate = new File(dir.trim(), exeName);
                if (candidate.isFile()) return candidate;
            }
        }

        // 3. Fall back to bundled binary
        File binDir = resolveBundledBinDir();
        if (binDir != null) {
            File found = findFile(binDir, exeName);
            if (found != null) return found;
        }

        throw new BinaryNotFoundException(name, (customBinPath != null ? customBinPath : "PATH or bundled bin directory"));
    }

    private static void validate(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            throw new BinaryNotFoundException("bin-directory", dir.getAbsolutePath());
        }

        java.util.List<String> missing = new java.util.ArrayList<>();
        
        // Check executables
        String exeExt = CURRENT_OS.getExeExtension();
        if (findFile(dir, "ffmpeg" + exeExt) == null) missing.add("ffmpeg" + exeExt);
        if (findFile(dir, "ffprobe" + exeExt) == null) missing.add("ffprobe" + exeExt);

        // Check Libraries
        for (String lib : REQUIRED_LIBS) {
            String libFilename = getLibraryFilename(lib);
            if (findFile(dir, libFilename) == null) {
                // On Linux system installs, libraries may be in /usr/lib instead of /usr/bin
                if (CURRENT_OS != OS.LINUX || !findInSystemLibPaths(libFilename)) {
                    missing.add(libFilename);
                }
            }
        }

        if (!missing.isEmpty()) {
            throw new io.github.kinsleykajiva.ffmpeg.exception.BinaryNotFoundException(
                "FFmpeg Components (" + CURRENT_OS.name() + ")", 
                "Missing files in " + dir.getAbsolutePath() + " (recursive search): " + String.join(", ", missing)
            );
        }
    }

    /**
     * Searches standard Linux system library directories for a given library file.
     * If the exact filename (e.g. libavutil.so.60) is not found, it attempts to 
     * find any version of the library by searching for its base name (e.g. libavutil.so).
     */
    private static boolean findInSystemLibPaths(String filename) {
        for (String path : LINUX_LIB_SEARCH_PATHS) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                // 1. Try exact match (or prefix match for versions via findFile's fuzzy logic)
                if (findFile(dir, filename) != null) return true;
                
                // 2. Try base name fallback (e.g. search for libavutil.so if looking for libavutil.so.60)
                if (filename.contains(".so.")) {
                    String baseName = filename.substring(0, filename.indexOf(".so") + 3);
                    if (findFile(dir, baseName) != null) return true;
                }
            }
        }
        return false;
    }

    private static File resolveBundledBinDir() {
        try {
            URL codeSource = FFmpegBinary.class.getProtectionDomain().getCodeSource().getLocation();
            File classLocation = new File(codeSource.toURI());
            
            // For dev environments, look for the 'ffmpeg-builds' folder in parent directories
            File dir = classLocation;
            for (int i = 0; i < 6; i++) {
                File candidate = new File(dir, "ffmpeg-builds");
                if (candidate.exists()) return candidate;
                dir = dir.getParentFile();
                if (dir == null) break;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
