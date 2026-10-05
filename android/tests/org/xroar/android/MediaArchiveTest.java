package org.xroar.android;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class MediaArchiveTest {
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    static File zip(File directory, String label, String... names) throws Exception {
        File file = new File(directory, label + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            for (String name : names) {
                out.putNextEntry(new ZipEntry(name));
                out.write(new byte[]{1, 2, 3, 4});
                out.closeEntry();
            }
        }
        return file;
    }
    static void rejected(File archive, File target) throws Exception {
        try {
            MediaArchive.extract(archive, target);
            throw new AssertionError("Unsafe or ambiguous archive accepted");
        } catch (IOException expected) { }
    }
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("xroar-media-test").toFile();
        File media = new File(root, "media");
        try {
            File archive = zip(root, "single", "folder/Game.cas", "folder/Game.wav", "README.txt");
            File selected = MediaArchive.extract(archive, media);
            check(selected.getName().equals("Game.cas"), "CAS preferred over matching WAV");
            check(selected.length() == 4 && archive.isFile(), "Extracted intact; original retained");
            check(MediaArchive.extract(archive, media).getName().equals("Game_2.cas"),
                    "Existing tape is not overwritten");
            check(MediaArchive.extract(zip(root, "audio", "Game.wav"), media)
                    .getName().equals("Game.wav"), "Standalone WAV supported");
            rejected(zip(root, "traversal", "../escape.cas"), media);
            rejected(zip(root, "absolute", "/escape.cas"), media);
            rejected(zip(root, "ambiguous", "GameA.cas", "GameB.cas"), media);
            rejected(zip(root, "unsupported", "README.txt"), media);
            check(!new File(root, "escape.cas").exists(), "No ZIP path escapes storage");
            if (args.length > 0) {
                File canyon = MediaArchive.extract(new File(args[0]), media);
                check(canyon.getName().equals("Canyon Climber (Tandy).cas"),
                        "Actual Canyon Climber archive selects the CAS image");
                byte[] bytes = Files.readAllBytes(canyon.toPath());
                check(bytes.length == 8769, "Actual cassette bytes retained");
                boolean machineCode = false;
                for (int i = 0; i + 18 < bytes.length; i++) {
                    if ((bytes[i] & 255) == 0x3c && bytes[i + 1] == 0
                            && (bytes[i + 2] & 255) >= 15 && bytes[i + 11] == 2) {
                        machineCode = true;
                        break;
                    }
                }
                check(machineCode, "Cassette filename block requests machine-code loading");
                File roms = new File(root, "roms");
                check(roms.mkdir(), "Create synthetic firmware fixtures");
                Files.write(new File(roms, "bas12.rom").toPath(), new byte[8192]);
                Files.write(new File(roms, "extbas11.rom").toPath(), new byte[8192]);
                String[] launch = MachineProfile.OPTIONS[0].arguments(roms, canyon);
                int run = Arrays.asList(launch).indexOf("-run");
                check(run >= 0 && launch[run + 1].equals(canyon.getAbsolutePath()),
                        "Autorun receives the CAS, not the ZIP or WAV");
                System.out.println("PASS: Canyon Climber CAS extracted and queued for native CLOADM autorun");
            }
            for (int[] size : new int[][]{{402, 450}, {850, 240}, {402, 180}, {120, 90}}) {
                MonitorGeometry tv = new MonitorGeometry(size[0], size[1]);
                check(tv.left >= 0 && tv.top >= 0 && tv.left + tv.width <= size[0]
                        && tv.top + tv.height <= size[1], "Cabinet fits available display region");
                check(tv.screenLeft >= tv.left && tv.screenTop >= tv.top
                        && tv.screenLeft + tv.screenWidth <= tv.left + tv.width
                        && tv.screenTop + tv.screenHeight <= tv.top + tv.height,
                        "Live screen stays within cabinet");
                check(Math.abs(tv.screenWidth * 3 - tv.screenHeight * 4) <= 4,
                        "CRT opening remains 4:3 within pixel rounding");
            }
            System.out.println("PASS: Archive safety, CAS/WAV choice and bezel screen geometry");
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> paths = Files.walk(root.toPath())) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.delete(path); } catch (IOException e) { throw new RuntimeException(e); }
                });
            }
        }
    }
}
