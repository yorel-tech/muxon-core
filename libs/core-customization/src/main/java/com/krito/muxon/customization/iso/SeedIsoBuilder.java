package com.krito.muxon.customization.iso;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

/**
 * Builds a seed ISO9660 image from a map of filename → file content strings.
 *
 * <p>Implementation: shells out to {@code genisoimage} or {@code xorriso}, whichever is
 * available on the worker host. Both tools are standard packages on Ubuntu/Debian/RHEL.
 *
 * <p>Security: the seed ISO is written with mode {@code 0600} in the parent directory,
 * which must already be {@code 0700}. Both constraints are enforced here.
 */
public class SeedIsoBuilder {

    private static final long MAX_PAYLOAD_BYTES = 1024L * 1024L; // 1 MiB sanity limit

    private final String isoToolCommand;

    /**
     * Create a builder that auto-detects the first available ISO tool.
     */
    public SeedIsoBuilder() {
        this.isoToolCommand = detectIsoTool();
    }

    /**
     * Create a builder with an explicit tool command (useful for testing).
     */
    public SeedIsoBuilder(String isoToolCommand) {
        this.isoToolCommand = isoToolCommand;
    }

    /**
     * Write the seed ISO to {@code outputPath}.
     *
     * @param files       Map of filename → UTF-8 content.
     * @param volumeLabel ISO9660 volume label (e.g. {@code CIDATA}, {@code UNATTEND}).
     * @param outputPath  Absolute path of the ISO to create.
     * @throws IOException if the tool is unavailable or returns non-zero.
     */
    public void build(Map<String, String> files, String volumeLabel, Path outputPath) throws IOException {
        // 1. Write file content into a temp staging directory
        Path stagingDir = Files.createTempDirectory("muxon-seed-");
        try {
            long totalBytes = 0;
            for (Map.Entry<String, String> entry : files.entrySet()) {
                byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
                totalBytes += bytes.length;
                if (totalBytes > MAX_PAYLOAD_BYTES) {
                    throw new IOException("Seed ISO payload exceeds " + MAX_PAYLOAD_BYTES + " bytes limit");
                }
                Path dest = stagingDir.resolve(entry.getKey());
                Files.writeString(dest, entry.getValue(), StandardCharsets.UTF_8);
            }

            // 2. Ensure parent dir and output path have correct permissions
            Path parentDir = outputPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
                try {
                    Files.setPosixFilePermissions(parentDir, Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE));
                } catch (UnsupportedOperationException ignored) {
                    // Non-POSIX filesystem (e.g. Windows tests)
                }
            }

            // 3. Build ISO
            List<String> cmd = buildCommand(volumeLabel, stagingDir, outputPath);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode;
            try {
                exitCode = proc.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("ISO build interrupted", e);
            }
            if (exitCode != 0) {
                throw new IOException("ISO build failed (exit=" + exitCode + "): " + output);
            }

            // 4. Lock down the output file
            try {
                Files.setPosixFilePermissions(outputPath, Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException ignored) {}

        } finally {
            // 5. Clean up staging directory
            deleteRecursive(stagingDir);
        }
    }

    private List<String> buildCommand(String volumeLabel, Path stagingDir, Path outputPath) {
        if ("genisoimage".equals(isoToolCommand) || isoToolCommand.endsWith("/genisoimage")) {
            return List.of(
                    isoToolCommand,
                    "-output", outputPath.toString(),
                    "-volid", volumeLabel,
                    "-joliet",
                    "-rock",
                    "-quiet",
                    stagingDir.toString());
        }
        // xorriso (default / fallback)
        return List.of(
                isoToolCommand,
                "-as", "mkisofs",
                "-output", outputPath.toString(),
                "-volid", volumeLabel,
                "-joliet",
                "-rock",
                "-quiet",
                stagingDir.toString());
    }

    private static String detectIsoTool() {
        for (String tool : List.of("genisoimage", "xorriso")) {
            try {
                ProcessBuilder pb = new ProcessBuilder("which", tool);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                if (p.waitFor() == 0 && !out.isEmpty()) {
                    return out;
                }
            } catch (Exception ignored) {}
        }
        // Return a placeholder; build() will fail with a clear error at runtime.
        return "xorriso";
    }

    private static void deleteRecursive(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var stream = Files.list(path)) {
                    stream.forEach(SeedIsoBuilder::deleteRecursive);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException ignored) {}
    }
}
