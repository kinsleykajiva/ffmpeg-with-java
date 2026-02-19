# Modern FFmpeg Java (Java 25+)

A production-grade, type-safe, and fluent Java wrapper for FFmpeg and FFprobe. Designed for modern high-performance media applications, it leverages **Java 25 Records**, **Sealed Classes**, and **Project Panama** for native efficiency and developer ergonomics.

---

## 🚀 Key Features

### 1. Modern Java 25 Architecture
*   **Records**: Immutable data carriers for `AudioMetadata` and `EncodingResult`. No more parsing raw strings or maps.
*   **Sealed Exceptions**: A structured hierarchy (`FFmpegException`) that allows granular error handling for missing binaries, network congestion, or execution timeouts.
*   **Enums**: Type-safe constants for `AudioCodec` (Opus, AAC, MP3), `SampleRate`, and `ChannelLayout`.

---

## 🧠 Memory & Performance

This library is built with high-performance media requirements in mind, utilizing **Project Panama (JEP 454)** for ultra-efficient native interactions.

### Off-Heap Memory Safety
Unlike traditional JNI-based wrappers, we use Java's **Foreign Function & Memory API** to handle native memory:
- **Deterministic Resource Management**: We use `Arena.ofConfined()` for all native calls. This ensures that memory used during metadata probing is released **immediately** after the call, preventing the "memory creep" common in media processing.
- **Zero-Copy Probing**: When extracting metadata, we map native FFmpeg structs directly to Java `MemorySegment` objects, avoiding expensive byte-array copying.
- **Off-heap Efficiency**: Sensitive data or large buffers are maintained off-heap, keeping the JVM Garbage Collector (GC) lean and responsive for your application logic.

### Process Isolation
While metadata probing happens in-process via Panama for speed, heavy transcoding is handled via **Process Isolation**:
- **Stability**: Crashing FFmpeg binaries (e.g., due to corrupt input) won't crash your JVM.
- **Memory Capping**: Native memory used by the `ffmpeg` process is separate from the Java heap, making it easier to manage resource limits in containerized environments (Docker/K8s).

---
### 2. Fluent Builder API
Construct complex media jobs using a clean, chained interface that eliminates configuration errors.
```java
FFmpeg.input("song.mp3")
    .output("processed.opus")
    .withCodec(AudioCodec.LIBOPUS)
    .withBitrate("128k")
    .addFilter(AudioFilters.volume(1.2))
    .executeAsync();
```

### 3. Production-Grade Real-Time Streaming
Go beyond file processing with dedicated low-latency streaming support:
*   **Protocols**: Native support for `RTP`, `SRT`, `UDP`, and `RTSP`.
*   **Low-Latency Tuning**: `.asLiveSource()` enables `-re` and `nobuffer` flags.
*   **Instant Startup**: `.withInstantStartup()` sets `probesize` and `analyzeduration` to near-zero.
*   **SDP Auto-Generation**: Automatically export `.sdp` files for RTP receivers.

### 4. Real-Time Monitoring & Health
*   **Throughput Stats**: Track `bitrate` and processing `speed` in real-time.
*   **Congestion Detection**: Easily identify when network speed falls below 1.0x (real-time).
*   **Progress Tracking**: Visual feedback on processed frames and bitrate.

### 5. Defensive Validation
Catch errors before they reach FFmpeg:
*   **Path Validation**: Ensures input files exist and are readable.
*   **Bitrate Regex**: Validates formats like `128k` or `1M`.
*   **Volume Range**: Prevents audio clipping by enforcing the 0.0 to 10.0 range.

---

## 🛠 Usage Examples

### Metadata Probing (Panama Powered)
```java
AudioMetadata metadata = FFmpeg.input("input.mp3").probe();
System.out.println("Format: " + metadata.format());
System.out.println("Duration: " + metadata.durationSeconds() + "s");
```

### Low-Latency RTP Streaming
```java
FFmpeg.input("live_input.wav")
    .asLiveSource()
    .withCodec(AudioCodec.LIBOPUS)
    .toStream(StreamDestination.rtp("192.168.1.50", 5004))
    .saveSdpTo(Path.of("stream.sdp"))
    .onStreamStats((br, speed, dropped) -> {
        System.out.printf("Health: %d bps | Speed %.2fx\n", br, speed);
    })
    .executeAsync();
```

---

## ⚖ Advantages & Downsides

### ✅ Advantages
1.  **Type Safety**: Drastically reduces "magic string" errors common in other wrappers.
2.  **Modern Concurrency**: Full support for `CompletableFuture` (async) and callbacks.
3.  **Deterministic Memory**: Project Panama `Arena` usage ensures zero memory leaks in native space.
4.  **Low Latency**: Highly tuned for real-time media engines (Project Panama overhead is negligible).
5.  **GC Efficiency**: Off-heap data handling keeps Java heap usage low and stable.
6.  **No Boilerplate**: Binary resolution, error parsing, and progress monitoring are handled out-of-the-box.
7.  **Robustness**: Sealed exceptions force developers to handle specific failure modes.

### ❌ Downsides
1.  **Feature Scope**: Currently focused on **Audio**. Video support is scheduled for a future release.
2.  **Native Dependency**: Requires `ffmpeg.exe` and `ffprobe.exe` to be present (though it can auto-locate them).
3.  **Preview Feature**: Uses Java's Project Panama (Foreign Function API), which requires `--enable-preview` and native access flags.

---

## 🏗 Project Structure

*   `FFmpeg.java`: Unified entry point.
*   `AudioJobBuilder.java`: Fluent configuration engine.
*   `FFmpegExecutor.java`: Low-level process and stream management.
*   `FFmpegBinary.java`: Smart executable resolver.
*   `exception/`: Sealed exception hierarchy.
*   `model/`: Type-safe records and enums.

### 🏗 Demo Examples

We provide modular demo classes in the `demo` package for easy exploration:
- **`MetadataDemo`**: Shows Project Panama-based probing of MP3/WAV info.
- **`ComprehensiveMetadataDemo`**: Demonstrates reading and **setting** (writing) audio tags like Title, Artist, and Album.
- **`TranscodingDemo`**: Demonstrates the fluent builder with filters and async execution.
- **`StreamingDemo`**: Showcases RTP streaming with real-time stats monitoring.
- **`ValidationDemo`**: Highlights the safety checks for paths, bitrates, and volume.

---

### 🚦 Getting Started

### Prerequisites
*   Java 22+ (Java 25 recommended)
*   Maven

### Running the Demos
Ensure you use the following JVM flags for native access:
```bash
java --enable-native-access=ALL-UNNAMED --enable-preview -classpath ... demo.AudioTest
```
