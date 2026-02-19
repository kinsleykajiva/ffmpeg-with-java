package io.github.kinsleykajiva.ffmpeg.execution;

import io.github.kinsleykajiva.ffmpeg.exception.FFmpegException;
import io.github.kinsleykajiva.ffmpeg.model.EncodingResult;

/**
 * Callback interface for the full lifecycle of an FFmpeg job.
 */
public interface FFmpegCallback {
    void onStart(String command);
    void onProgress(double percentage, long frame, double bitrate);
    void onSuccess(EncodingResult result);
    void onError(FFmpegException exception);
}
