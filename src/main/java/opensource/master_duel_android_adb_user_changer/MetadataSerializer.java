package opensource.master_duel_android_adb_user_changer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MetadataSerializer {
    public byte[] serialize(Properties properties) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            properties.store(outputStream, null);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize metadata properties.", ex);
        }

        String text = outputStream.toString(StandardCharsets.ISO_8859_1);
        String normalized = removeLeadingComments(text);
        return normalized.getBytes(StandardCharsets.ISO_8859_1);
    }

    private String removeLeadingComments(String text) {
        String[] lines = text.split("\\R", -1);
        List<String> kept = new ArrayList<>();
        boolean skipping = true;
        for (String line : lines) {
            if (skipping && (line.startsWith("#") || line.startsWith("!"))) {
                continue;
            }
            skipping = false;
            kept.add(line);
        }
        return String.join("\n", kept);
    }
}
