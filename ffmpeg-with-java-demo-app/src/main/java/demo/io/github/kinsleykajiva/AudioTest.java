package demo.io.github.kinsleykajiva;

import io.github.kinsleykajiva.ffmpeg.*;
import static io.github.kinsleykajiva.ffmpeg.ffmpeg_includes_h.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AudioTest {
    public static void main(String[] args) {
        // Path to the test file
        String filePath = "media-files/song.mp3";
        Path path = Paths.get(filePath).toAbsolutePath();
        System.out.println("Processing file: " + path);

        try (Arena arena = Arena.ofConfined()) {
            // Allocate a pointer for AVFormatContext
            MemorySegment ppFormatCtx = arena.allocate(ValueLayout.ADDRESS);

            // Convert path to C string
            MemorySegment pathSegment = arena.allocateFrom(path.toString());

            // Open input file
            int ret = avformat_open_input(ppFormatCtx, pathSegment, MemorySegment.NULL, MemorySegment.NULL);
            if (ret < 0) {
                System.err.println("Could not open file: " + path);
                return;
            }

            // Get the pointer values from the double pointer
            MemorySegment pFormatCtx = ppFormatCtx.get(ValueLayout.ADDRESS, 0);

            // Retrieve stream information
            if (avformat_find_stream_info(pFormatCtx, MemorySegment.NULL) < 0) {
                System.err.println("Could not find stream information");
                avformat_close_input(ppFormatCtx);
                return;
            }

            // Read Basic Info
            long duration = AVFormatContext.duration(pFormatCtx);
            long bitRate = AVFormatContext.bit_rate(pFormatCtx);
            
            System.out.println("--- Audio Info ---");
            System.out.println("Duration: " + (duration / 1_000_000) + " seconds");
            System.out.println("Bitrate: " + (bitRate / 1000) + " kbps");

            // Extract Metadata
            MemorySegment metadata = AVFormatContext.metadata(pFormatCtx);
            if (!metadata.equals(MemorySegment.NULL)) {
                System.out.println("--- Metadata ---");
                MemorySegment entry = MemorySegment.NULL;
                
                // Iterate through metadata dictionary
                while (true) {
                    entry = av_dict_get(metadata, arena.allocateFrom(""), entry, AV_DICT_IGNORE_SUFFIX());
                    if (entry.equals(MemorySegment.NULL)) {
                        break;
                    }
                    String key = AVDictionaryEntry.key(entry).getString(0);
                    String value = AVDictionaryEntry.value(entry).getString(0);
                    System.out.println(key + ": " + value);
                }
            } else {
                System.out.println("No metadata found.");
            }

            // Cleanup
            avformat_close_input(ppFormatCtx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
