package org.xroar.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.CRC32;

/** Import one program safely, preferring CAS over an equivalent WAV recording. */
final class MediaArchive {
    private static final long MAX_BYTES = 32L * 1024 * 1024;

    static boolean isZip(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private static String basename(String path) throws IOException {
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*"))
            throw new IOException("Archive contains an unsafe absolute path.");
        for (String part : normalized.split("/")) {
            if (part.equals("..")) throw new IOException("Archive contains an unsafe parent path.");
        }
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (name.isEmpty() || name.length() > 120
                || !name.equals(name.replaceAll("\\p{Cntrl}", "")))
            throw new IOException("Archive contains an invalid filename.");
        return name;
    }

    private static boolean supported(String name) {
        return name.toLowerCase(Locale.ROOT).matches(
                ".*\\.(cas|c10|k7|wav|ccc|rom|bin|dsk|vdk|dmk|jvc|sn|sna|bas|asc|hex|s19)$");
    }

    static boolean isTape(File file) {
        return file.getName().toLowerCase(Locale.ROOT).matches(".*\\.(cas|c10|k7|wav)$");
    }

    static File extract(File archive, File directory) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            List<ZipEntry> programs = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (++count > 128) throw new IOException("Archive contains too many files.");
                if (entry.isDirectory()) continue;
                String name = basename(entry.getName());
                if (name.startsWith(".") || entry.getName().startsWith("__MACOSX/")) continue;
                if (supported(name)) programs.add(entry);
            }
            // A CAS and WAV with the same basename are alternate versions of
            // one cassette, not two games. Prefer the faster CAS representation.
            List<ZipEntry> choices = new ArrayList<>();
            for (ZipEntry entry : programs) {
                String name = basename(entry.getName()).toLowerCase(Locale.ROOT);
                boolean alternate = false;
                if (name.endsWith(".wav")) {
                    String cas = name.substring(0, name.length() - 4) + ".cas";
                    for (ZipEntry other : programs)
                        if (basename(other.getName()).equalsIgnoreCase(cas)) alternate = true;
                }
                if (!alternate) choices.add(entry);
            }
            if (choices.isEmpty()) throw new IOException("ZIP contains no supported program or tape.");
            if (choices.size() != 1)
                throw new IOException("ZIP contains multiple programs. Extract it and choose one game.");
            ZipEntry chosen = choices.get(0);
            if (chosen.getSize() > MAX_BYTES) throw new IOException("Archived program exceeds 32 MB.");
            if (!directory.isDirectory() && !directory.mkdirs())
                throw new IOException("Could not create media storage.");
            String name = basename(chosen.getName());
            File destination = new File(directory, name);
            int dot = name.lastIndexOf('.');
            int suffix = 2;
            while (destination.exists())
                destination = new File(directory, name.substring(0, dot) + "_" + suffix++
                        + name.substring(dot));
            File staging = File.createTempFile("xroar-tape-", ".part", directory);
            try {
                long copied = 0;
                CRC32 checksum = new CRC32();
                try (InputStream input = zip.getInputStream(chosen);
                        FileOutputStream output = new FileOutputStream(staging)) {
                    byte[] buffer = new byte[16384];
                    int length;
                    while ((length = input.read(buffer)) != -1) {
                        copied += length;
                        if (copied > MAX_BYTES) throw new IOException("Archived program exceeds 32 MB.");
                        output.write(buffer, 0, length);
                        checksum.update(buffer, 0, length);
                    }
                }
                if (copied == 0) throw new IOException("Archived program is empty.");
                if ((chosen.getSize() >= 0 && copied != chosen.getSize())
                        || (chosen.getCrc() >= 0 && checksum.getValue() != chosen.getCrc()))
                    throw new IOException("Archived program failed its integrity check.");
                if (!staging.renameTo(destination)) throw new IOException("Could not finish extracting tape.");
                return destination;
            } finally {
                staging.delete();
            }
        }
    }
}
