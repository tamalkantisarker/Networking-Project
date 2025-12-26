package com.securechat.common;

import com.securechat.common.util.FileTransferUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class FileTransferVerificationTest {

    @Test
    public void verifyFileChunkingAndMerger() throws IOException {
        // 1. Create a dummy file (150KB)
        File originalFile = new File("test_original.dat");
        try (FileOutputStream fos = new FileOutputStream(originalFile)) {
            byte[] data = new byte[150 * 1024]; // 150KB
            new Random().nextBytes(data);
            fos.write(data);
        }

        // 2. Split file -> Expect 3 chunks (64KB, 64KB, 22KB)
        List<byte[]> chunks = FileTransferUtil.splitFile(originalFile);
        Assertions.assertEquals(3, chunks.size(), "Should have 3 chunks for 150KB file with 64KB chunk size");
        Assertions.assertEquals(64 * 1024, chunks.get(0).length);
        Assertions.assertEquals(64 * 1024, chunks.get(1).length);

        // 3. Merge chunks
        File restoredFile = new File("test_restored.dat");
        FileTransferUtil.mergeChunks(chunks, restoredFile);

        // 4. Verify Integrity (Checksum)
        String originalHash = FileTransferUtil.calculateChecksum(originalFile);
        String restoredHash = FileTransferUtil.calculateChecksum(restoredFile);

        Assertions.assertEquals(originalHash, restoredHash, "Checksums must match after split/merge");

        // Cleanup
        originalFile.delete();
        restoredFile.delete();
    }
}
