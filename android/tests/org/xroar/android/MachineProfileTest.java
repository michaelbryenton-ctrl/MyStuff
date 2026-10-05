package org.xroar.android;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Configuration regression checks; generated files are not real firmware or games. */
public final class MachineProfileTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("xroar-launch-test-");
        try {
            File roms = Files.createDirectory(root.resolve("roms")).toFile();
            File games = Files.createDirectory(root.resolve("games")).toFile();
            MachineProfile ntsc = MachineProfile.OPTIONS[MachineProfile.indexOf("cocous")];
            check(ntsc.validationError(roms).contains("Color BASIC"), "Empty library is rejected");
            File extended = image(roms, "extbas11.rom", 8192);
            File game = image(games, "Space Assault (1981) (26-3060) (Tandy).ccc", 8192);
            image(roms, game.getName(), 8192);
            check(ntsc.validationError(roms).contains("bas12.rom"),
                    "Extended BASIC plus a cartridge does not replace Color BASIC");
            boolean rejected = false;
            try {
                ntsc.arguments(roms, game);
            } catch (IllegalStateException expected) {
                rejected = true;
            }
            check(rejected, "Incomplete firmware cannot generate launch arguments");
            File basic = image(roms, "bas12.rom", 8192);
            check(ntsc.validationError(roms) == null, "Both CoCo firmware halves are ready");
            List<String> arguments = Arrays.asList(ntsc.arguments(roms, game));
            check(value(arguments, "-joy-right").equals("android-touch-0")
                    && value(arguments, "-joy-left").equals("android-touch-1"),
                    "Touch joystick configs map to the right and left ports");
            check(value(arguments, "-machine").equals("cocous"), "NTSC machine is explicit");
            check(value(arguments, "-bas").equals(basic.getAbsolutePath()), "Color BASIC path is explicit");
            check(value(arguments, "-extbas").equals(extended.getAbsolutePath()), "Extended BASIC path is explicit");
            check(value(arguments, "-run").equals(game.getAbsolutePath()),
                    "Cartridge filename with spaces is preserved as a single argument");
            MachineProfile pal = MachineProfile.OPTIONS[MachineProfile.indexOf("coco")];
            check(value(Arrays.asList(pal.arguments(roms, null)), "-machine").equals("coco"),
                    "PAL selection is distinct from NTSC");
            image(roms, "d64_1.rom", 16384);
            check(value(Arrays.asList(ntsc.arguments(roms, game)), "-machine").equals("cocous"),
                    "A Dragon ROM cannot override the selected CoCo");
            image(roms, "bas12.rom", 0);
            check(ntsc.validationError(roms).contains("too small"), "Empty firmware is rejected");
            check(basic.delete(), "Remove incomplete test firmware");
            image(roms, "Color Basic v1.2 (1982)(Tandy).ROM", 8192);
            check(ntsc.validationError(roms) == null, "Upstream names with spaces and capitals resolve");
            File combinedDirectory = Files.createDirectory(root.resolve("combined")).toFile();
            File combined = image(combinedDirectory, "coco.rom", 16384);
            check(ntsc.validationError(combinedDirectory) == null, "Combined CoCo firmware is supported");
            List<String> combinedArguments = Arrays.asList(ntsc.arguments(combinedDirectory, game));
            check(combinedArguments.contains("-no-bas") && !combinedArguments.contains("-bas"),
                    "Combined Color BASIC bank is not overwritten by a default ROM");
            check(value(combinedArguments, "-extbas").equals(combined.getAbsolutePath()),
                    "Combined firmware loads from the first bank");
            image(combinedDirectory, "coco.rom", 8192);
            check(ntsc.validationError(combinedDirectory).contains("Color BASIC"),
                    "An 8K image is not mistaken for combined firmware");
            File otherDirectory = Files.createDirectory(root.resolve("others")).toFile();
            MachineProfile coco3 = MachineProfile.OPTIONS[MachineProfile.indexOf("coco3")];
            image(otherDirectory, "coco3.rom", 32768);
            check(coco3.validationError(otherDirectory) == null, "CoCo 3 uses its own firmware");
            MachineProfile mc10 = MachineProfile.OPTIONS[MachineProfile.indexOf("mc10")];
            check(mc10.validationError(otherDirectory).contains("mc10.rom"),
                    "Other machines report their own required ROM");
            image(otherDirectory, "mc10.rom", 8192);
            check(mc10.validationError(otherDirectory) == null, "MC-10 firmware resolves");
            MachineProfile dragon = MachineProfile.OPTIONS[MachineProfile.indexOf("dragon64")];
            check(dragon.validationError(roms) == null, "Dragon 64 primary firmware resolves");
            File alternate = image(roms, "d64_2.rom", 16384);
            check(value(Arrays.asList(dragon.arguments(roms, null)), "-altbas")
                    .equals(alternate.getAbsolutePath()), "Optional Dragon 64 alternate bank resolves");
            check(MachineProfile.isCartridge(game)
                    && MachineProfile.isCartridge(new File("GAME.CCC"))
                    && !MachineProfile.isCartridge(extended), "Cartridges are distinguished from firmware");
            System.out.println("PASS: " + checks + " launch configuration checks");
        } finally {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                    Files.delete(path);
                }
            }
        }
    }

    private static File image(File directory, String name, int size) throws Exception {
        File image = new File(directory, name);
        try (RandomAccessFile file = new RandomAccessFile(image, "rw")) {
            file.setLength(size);
        }
        return image;
    }

    private static String value(List<String> args, String option) {
        int index = args.indexOf(option);
        if (index < 0) throw new AssertionError("Missing option: " + option);
        return args.get(index + 1);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
