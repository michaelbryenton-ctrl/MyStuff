package org.xroar.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;

public final class SavedTapeStoreTest {
    private static int checks;
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
    private interface Operation { void run() throws IOException; }
    private static void fails(Operation operation, String name) throws IOException {
        try { operation.run(); throw new AssertionError(name); }
        catch (IOException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        SavedTapeStore store = new SavedTapeStore(root);
        check(store.list().length == 0, "Initially empty");
        File first = store.create("../MY/PROG\\\n");
        check(first.getParentFile().equals(new File(root, "saved-tapes")), "Safe private parent");
        check(first.getName().endsWith(".cas") && !first.getName().contains("/"),
                "Sanitized cassette filename");
        File second = store.create("../MY/PROG\\\n");
        check(!first.equals(second), "Repeated labels create unique files");
        byte[] cassette = new byte[40000];
        for (int i = 0; i < cassette.length; i++) cassette[i] = (byte)i;
        Files.write(first.toPath(), cassette);
        check(store.requireFile(first.getPath()).equals(first.getCanonicalFile()), "Own file resolves");
        ByteArrayOutputStream exported = new ByteArrayOutputStream();
        check(store.copyTo(first.getPath(), exported) == cassette.length, "Full multi-buffer copy");
        check(Arrays.equals(cassette, exported.toByteArray()), "Byte-for-byte export");
        check(Arrays.equals(cassette, Files.readAllBytes(first.toPath())), "Original preserved");
        fails(() -> store.copyTo(second.getPath(), new ByteArrayOutputStream()), "Reject empty");
        File outside = new File(root, "outside.cas");
        Files.write(outside.toPath(), cassette);
        fails(() -> store.requireFile(outside.getPath()), "Reject outside path");
        fails(() -> store.requireFile(null), "Reject missing selection");
        fails(() -> store.requireFile(new File(root, "missing.cas").getPath()), "Reject missing file");
        File link = new File(first.getParentFile(), "outside-link.cas");
        Files.createSymbolicLink(link.toPath(), outside.toPath());
        fails(() -> store.requireFile(link.getPath()), "Reject symlink escaping private folder");
        check(store.list().length == 2, "List only private cassettes");
        fails(() -> store.copyTo(first.getPath(), new OutputStream() {
            @Override public void write(int value) throws IOException { throw new IOException("Disk full"); }
        }), "Report destination write failure");
        check(Arrays.equals(cassette, Files.readAllBytes(first.toPath())),
                "Failed export cannot erase source");
        File third = store.create("MY PROG");
        check(first.length() == cassette.length && third.length() == 0,
                "New recording never overwrites earlier save");
        File corruptFolder = new File(root, "blocked");
        check(corruptFolder.mkdir(), "Set up failed-folder test");
        Files.write(new File(corruptFolder, "saved-tapes").toPath(), new byte[]{1});
        fails(() -> new SavedTapeStore(corruptFolder).create("Program"), "Report storage failure");
        System.out.println("PASS: " + checks + " cassette storage, export, path and failure checks");
    }
}
