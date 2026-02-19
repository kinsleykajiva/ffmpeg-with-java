package io.github.kinsleykajiva.ffmpeg.builder;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.github.kinsleykajiva.ffmpeg.execution.FFmpegCallback;
import io.github.kinsleykajiva.ffmpeg.execution.FFmpegExecutor;
import io.github.kinsleykajiva.ffmpeg.execution.OnProgressListener;
import io.github.kinsleykajiva.ffmpeg.model.ChannelLayout;
import io.github.kinsleykajiva.ffmpeg.model.SampleRate;

/**
 * Fluent builder for constructing and executing FFmpeg audio jobs.
 */
public class AudioJobBuilder {
    private final String inputPath;
    private String outputPath;
    private io.github.kinsleykajiva.ffmpeg.model.StreamDestination streamDestination;
    private io.github.kinsleykajiva.ffmpeg.model.AudioCodec codec;
    private String bitrate;
    private io.github.kinsleykajiva.ffmpeg.model.SampleRate sampleRate;
    private io.github.kinsleykajiva.ffmpeg.model.ChannelLayout channelLayout;
    private final List<String> filters = new ArrayList<>();
    private OnProgressListener progressListener;
    private io.github.kinsleykajiva.ffmpeg.execution.OnStreamStatsListener statsListener;
    private io.github.kinsleykajiva.ffmpeg.model.NetworkConfig networkConfig;
    private java.nio.file.Path sdpPath;
    private boolean isLiveSource = false;
    private boolean useWallclock = false;
    private Long probeSize;
    private Long analyzeDuration;
    private final java.util.Map<String, String> metadataTags = new java.util.HashMap<>();
    private long timeoutSeconds = 0;
    private Double readRate;
    private Consumer<Path> sdpCallback;

    public AudioJobBuilder(String inputPath, String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    /**
     * Enables low-latency real-time flags (-re, -fflags nobuffer).
     */
    public AudioJobBuilder asLiveSource() {
        this.isLiveSource = true;
        this.useWallclock = false; // safer default for file-to-rtp
        if (this.networkConfig == null) {
            this.networkConfig = io.github.kinsleykajiva.ffmpeg.model.NetworkConfig.defaultLowLatency();
        }
        return this;
    }

    /**
     * Sets the read rate speed (e.g., 1.0 for real-time).
     * Uses -readrate flag.
     */
    public AudioJobBuilder withReadRate(double rate) {
        this.readRate = rate;
        return this;
    }

    /**
     * Sets probe size (probesize) and analyze duration (analyzeduration) 
     * to near-zero for instant startup.
     */
    public AudioJobBuilder withInstantStartup() {
        this.probeSize = 32L;
        this.analyzeDuration = 0L;
        return this;
    }

    public AudioJobBuilder withWallclock(boolean useWallclock) {
        this.useWallclock = useWallclock;
        return this;
    }

    /**
     * Sets the destination as a network stream (e.g., rtp://127.0.0.1:5004).
     */
    public AudioJobBuilder toStream(io.github.kinsleykajiva.ffmpeg.model.StreamDestination destination) {
        this.streamDestination = destination;
        this.outputPath = null; // Use destination instead of file
        return this;
    }

    /**
     * Auto-generate and export an SDP file at the given path.
     */
    public AudioJobBuilder saveSdpTo(java.nio.file.Path path) {
        this.sdpPath = path;
        return this;
    }

    /**
     * Configures network-specific parameters like TTL and buffer sizes.
     */
    public AudioJobBuilder withNetworkConfig(io.github.kinsleykajiva.ffmpeg.model.NetworkConfig config) {
        this.networkConfig = config;
        return this;
    }

    /**
     * Sets a listener for real-time streaming stats (bitrate, speed).
     */
    public AudioJobBuilder onStreamStats(io.github.kinsleykajiva.ffmpeg.execution.OnStreamStatsListener listener) {
        this.statsListener = listener;
        return this;
    }

    public AudioJobBuilder withCodec(io.github.kinsleykajiva.ffmpeg.model.AudioCodec codec) {
        this.codec = codec;
        return this;
    }

    public AudioJobBuilder withBitrate(String bitrate) {
        if (bitrate != null && !bitrate.matches("(?i)\\d+[kM]?")) {
            throw new IllegalArgumentException("Invalid bitrate format: " + bitrate + ". Expected format like '128k' or '1M'.");
        }
        this.bitrate = bitrate;
        return this;
    }

    public AudioJobBuilder withSampleRate(SampleRate rate) {
        this.sampleRate = rate;
        return this;
    }

    public AudioJobBuilder withChannels(ChannelLayout layout) {
        this.channelLayout = layout;
        return this;
    }

    public AudioJobBuilder addFilter(String filter) {
        this.filters.add(filter);
        return this;
    }

    public AudioJobBuilder onProgress(OnProgressListener listener) {
        this.progressListener = listener;
        return this;
    }

    public AudioJobBuilder timeout(long seconds) {
        this.timeoutSeconds = seconds;
        return this;
    }

    /**
     * Callback triggered when the SDP file is successfully created.
     */
    public AudioJobBuilder onSdpCreated(Consumer<java.nio.file.Path> callback) {
        this.sdpCallback = callback;
        return this;
    }

    /**
     * Sets a metadata tag (e.g., title, artist, album).
     */
    public AudioJobBuilder withMetadata(String key, String value) {
        this.metadataTags.put(key, value);
        return this;
    }

    /**
     * Executes the job synchronously.
     */
    public io.github.kinsleykajiva.ffmpeg.model.EncodingResult execute() {
        validate();
        io.github.kinsleykajiva.ffmpeg.model.EncodingResult result = FFmpegExecutor.execute(buildCommand(), progressListener, statsListener, timeoutSeconds);
        if (sdpPath != null && sdpCallback != null && java.nio.file.Files.exists(sdpPath)) {
            sdpCallback.accept(sdpPath);
        }
        return result;
    }

    /**
     * Executes the job asynchronously.
     */
    public CompletableFuture<io.github.kinsleykajiva.ffmpeg.model.EncodingResult> executeAsync() {
        validate();
        CompletableFuture<io.github.kinsleykajiva.ffmpeg.model.EncodingResult> future = FFmpegExecutor.executeAsync(buildCommand(), progressListener, statsListener, timeoutSeconds);
        
        // If an SDP callback is registered, we should check for the file shortly after start
        if (sdpPath != null && sdpCallback != null) {
            CompletableFuture.runAsync(() -> {
                // Poll for SDP file existence for up to 5 seconds
                for (int i = 0; i < 50; i++) {
                    if (java.nio.file.Files.exists(sdpPath)) {
                        sdpCallback.accept(sdpPath);
                        break;
                    }
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                }
            });
        }
        
        return future;
    }

    /**
     * Executes with a full lifecycle callback.
     */
    public void execute(FFmpegCallback callback) {
        validate();
        List<String> cmd = buildCommand();
        callback.onStart(String.join(" ", cmd));
        
        executeAsync()
            .thenAccept(callback::onSuccess)
            .exceptionally(ex -> {
                if (ex.getCause() instanceof io.github.kinsleykajiva.ffmpeg.exception.FFmpegException fe) {
                    callback.onError(fe);
                } else {
                    callback.onError(new io.github.kinsleykajiva.ffmpeg.exception.ExecutionException(-1, ex.getMessage()));
                }
                return null;
            });
    }

    private void validate() {
        if (inputPath == null) throw new IllegalArgumentException("Input source is not set.");
        
        if (outputPath == null && streamDestination == null) {
            throw new IllegalArgumentException("No output specified. Use .output(path) or .toStream(destination).");
        }

        if (outputPath != null) {
            File output = new File(outputPath);
            File parent = output.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        }
    }

    private List<String> buildCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(io.github.kinsleykajiva.ffmpeg.FFmpegBinary.getFfmpeg().getAbsolutePath());
        cmd.add("-hide_banner");
        cmd.add("-loglevel");
        cmd.add("warning");
        
        // Input Flags
        if (probeSize != null) {
            cmd.add("-probesize");
            cmd.add(String.valueOf(probeSize));
        }
        if (analyzeDuration != null) {
            cmd.add("-analyzeduration");
            cmd.add(String.valueOf(analyzeDuration));
        }
        
        if (isLiveSource) {
            if (readRate != null) {
                cmd.add("-readrate");
                cmd.add(String.valueOf(readRate));
            } else {
                cmd.add("-re"); // Read input at native frame rate
            }

            if (useWallclock) {
                cmd.add("-use_wallclock_as_timestamps");
                cmd.add("1");
            }
            if (networkConfig != null && networkConfig.noBuffer()) {
                cmd.add("-fflags");
                cmd.add("nobuffer");
            }
        }

        cmd.add("-y"); // Overwrite
        cmd.add("-stats"); // Periodically print progress
        cmd.add("-i");
        cmd.add(inputPath);

        // Encoding Parameters
        if (codec != null) {
            cmd.add("-c:a");
            cmd.add(codec.getCodecName());
        }

        if (bitrate != null) {
            cmd.add("-b:a");
            cmd.add(bitrate);
        }

        if (sampleRate != null) {
            cmd.add("-ar");
            cmd.add(String.valueOf(sampleRate.getRate()));
        }

        if (channelLayout != null) {
            cmd.add("-ac");
            cmd.add(channelLayout.getChannels());
        }

        if (!filters.isEmpty()) {
            cmd.add("-af");
            cmd.add(String.join(",", filters));
        }

        // Network/Stream Flags
        if (networkConfig != null) {
            if (networkConfig.ttl() > 0) {
                cmd.add("-ttl");
                cmd.add(String.valueOf(networkConfig.ttl()));
            }
            if (networkConfig.bufferSize() > 0) {
                cmd.add("-rtbufsize");
                cmd.add(String.valueOf(networkConfig.bufferSize()));
            }
        }

        if (sdpPath != null) {
            cmd.add("-sdp_file");
            cmd.add(sdpPath.toString());
        }

        // Metadata Tags
        for (java.util.Map.Entry<String, String> entry : metadataTags.entrySet()) {
            cmd.add("-metadata");
            cmd.add(entry.getKey() + "=" + entry.getValue());
        }

        // Output
        if (streamDestination != null) {
            String uri = streamDestination.toString();
            if (networkConfig != null && networkConfig.rtcpport() != null) {
                // For RTP, we add rtcpport as a query param or part of the URI logic
                // FFmpeg expects: rtp://host:port?rtcpport=...
                uri += (uri.contains("?") ? "&" : "?") + "rtcpport=" + networkConfig.rtcpport();
            }
            // FFmpeg cannot infer the muxer from rtp:// alone — must specify -f rtp explicitly
            if (uri.startsWith("rtp://")) {
                cmd.add("-f");
                cmd.add("rtp");
            }
            cmd.add(uri);
        } else {
            cmd.add(outputPath);
        }

        return cmd;
    }
}
