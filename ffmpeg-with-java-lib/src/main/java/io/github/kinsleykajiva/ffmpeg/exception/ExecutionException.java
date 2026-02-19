package io.github.kinsleykajiva.ffmpeg.exception;

/**
 * Thrown when a command execution fails with a non-zero exit code.
 */
public final class ExecutionException extends FFmpegException {
    private final int exitCode;
    private final String stderr;

    public ExecutionException(int exitCode, String stderr) {
        super("FFmpeg command failed with exit code " + exitCode + ". Stderr: " + stderr);
        this.exitCode = exitCode;
        this.stderr = stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStderr() {
        return stderr;
    }
}
