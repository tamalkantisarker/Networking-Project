package com.securechat.common.util;

import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileTransferUtil {

    public static final int CHUNK_SIZE = 64 * 1024; // 64 KB chunks

    // Split file into chunks
    public static List<byte[]> splitFile(File file) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) > 0) {
                if (bytesRead < CHUNK_SIZE) {
                    chunks.add(Arrays.copyOf(buffer, bytesRead));
                } else {
                    chunks.add(buffer.clone()); // Clone to avoid reference issues
                }
            }
        }
        return chunks;
    }

    // Merge chunks into a file
    public static void mergeChunks(List<byte[]> chunks, File destination) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destination))) {
            for (byte[] chunk : chunks) {
                bos.write(chunk);
            }
        }
    }

    // Append single chunk (for streaming)
    public static void appendChunk(byte[] chunk, File destination) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(destination, true)) {
            fos.write(chunk);
        }
    }

    public static String calculateChecksum(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = new FileInputStream(file)) {
                byte[] byteArray = new byte[1024];
                int bytesCount;
                while ((bytesCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Checksum calculation failed", e);
        }
    }

    public static void skipFully(InputStream is, long n) throws IOException {
        long remaining = n;
        while (remaining > 0) {
            long skipped = is.skip(remaining);
            if (skipped <= 0) {
                // Check if we reached EOF
                if (is.read() == -1) {
                    throw new EOFException("End of stream reached before skipping " + n + " bytes");
                }
                remaining--; // We read one byte, so decrement remaining
            } else {
                remaining -= skipped;
            }
        }
    }
}
