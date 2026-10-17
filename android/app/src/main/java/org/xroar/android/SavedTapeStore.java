package org.xroar.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/** Private cassette storage; recordings never overwrite an imported or saved file. */
final class SavedTapeStore {
    private final File directory;

    SavedTapeStore(File filesDirectory) {
        directory = new File(filesDirectory, "saved-tapes");
    }

    File create(String label) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IOException("Could not create the saved-cassette folder.");
        String name = label == null ? "" : label.trim();
        name = name.replaceAll("[^a-zA-Z0-9 _-]", "_").trim();
        if (name.isEmpty()) name = "Program";
        if (name.length() > 48) name = name.substring(0, 48);
        File file = new File(directory, name + "-" + UUID.randomUUID() + ".cas");
        if (!file.createNewFile()) throw new IOException("Could not create a new cassette.");
        return file;
    }

    File requireFile(String path) throws IOException {
        if (path == null) throw new IOException("No cassette was selected.");
        File file = new File(path).getCanonicalFile();
        if (!directory.getCanonicalFile().equals(file.getParentFile())
                || !file.getName().endsWith(".cas") || !file.isFile())
            throw new IOException("The saved cassette is missing or outside app storage.");
        return file;
    }

    File[] list() {
        File[] files = directory.listFiles(file -> {
            try { return requireFile(file.getPath()).equals(file.getCanonicalFile()); }
            catch (IOException error) { return false; }
        });
        if (files == null) return new File[0];
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return files;
    }

    long copyTo(String path, OutputStream output) throws IOException {
        File source = requireFile(path);
        long expected = source.length();
        if (expected == 0) throw new IOException("The cassette is empty. Nothing was exported.");
        long count = 0;
        try (FileInputStream input = new FileInputStream(source)) {
            byte[] buffer = new byte[16384];
            for (int size; (size = input.read(buffer)) != -1;) {
                output.write(buffer, 0, size);
                count += size;
            }
        }
        if (count != expected || source.length() != expected)
            throw new IOException("The cassette changed during export. Try again after finishing it.");
        output.flush();
        return count;
    }
}
