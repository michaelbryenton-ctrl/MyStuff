package org.xroar.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Resolve firmware before SDL starts, rather than accepting XRoar's fallback machine. */
final class MachineProfile {
    private static final String[] BASIC = {
            "bas12", "bas13", "Color Basic v1.2 (1982)(Tandy)", "bas11", "bas10"
    };
    private static final String[] EXTENDED = {"extbas11", "extbas10", "coco"};
    private static final String[] DRAGON32 = {
            "d32", "dragon32", "d32rom", "Dragon Data Ltd - Dragon 32 - IC17", "dragon"
    };
    private static final String[] DRAGON64 = {
            "d64_1", "d64rom1", "Dragon Data Ltd - Dragon 64 - IC17",
            "Dragon Data Ltd - TANO IC18", "Eurohard S.A. - Dragon 200 IC18", "dragrom", "dragon"
    };
    private static final String[] DRAGON64_ALT = {
            "d64_2", "d64rom2", "Dragon Data Ltd - Dragon 64 - IC18",
            "Dragon Data Ltd - TANO IC17", "Eurohard S.A. - Dragon 200 IC17"
    };

    static final MachineProfile[] OPTIONS = {
            new MachineProfile("cocous", "Tandy CoCo (NTSC)", "-bas", BASIC, 8192,
                    EXTENDED, 8192, null),
            new MachineProfile("coco", "Tandy CoCo (PAL)", "-bas", BASIC, 8192,
                    EXTENDED, 8192, null),
            new MachineProfile("coco3", "Tandy CoCo 3 (NTSC)", "-extbas",
                    new String[]{"coco3"}, 32768, null, 0, null),
            new MachineProfile("coco3p", "Tandy CoCo 3 (PAL)", "-extbas",
                    new String[]{"coco3p"}, 32768, null, 0, null),
            new MachineProfile("dragon32", "Dragon 32", "-extbas", DRAGON32, 16384,
                    null, 0, null),
            new MachineProfile("dragon64", "Dragon 64", "-extbas", DRAGON64, 16384,
                    null, 0, DRAGON64_ALT),
            new MachineProfile("mc10", "Tandy MC-10", "-bas", new String[]{"mc10"}, 8192,
                    null, 0, null)
    };

    final String id;
    final String label;
    private final String primaryOption;
    private final String[] primaryNames;
    private final int primarySize;
    private final String[] extendedNames;
    private final int extendedSize;
    private final String[] alternateNames;

    private MachineProfile(String id, String label, String primaryOption,
            String[] primaryNames, int primarySize, String[] extendedNames,
            int extendedSize, String[] alternateNames) {
        this.id = id;
        this.label = label;
        this.primaryOption = primaryOption;
        this.primaryNames = primaryNames;
        this.primarySize = primarySize;
        this.extendedNames = extendedNames;
        this.extendedSize = extendedSize;
        this.alternateNames = alternateNames;
    }

    static int indexOf(String id) {
        for (int i = 0; i < OPTIONS.length; i++) {
            if (OPTIONS[i].id.equals(id)) return i;
        }
        return 0;
    }

    static boolean isCartridge(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".ccc");
    }

    String validationError(File directory) {
        File primary = findRom(directory, primaryNames);
        File extended = findRom(directory, extendedNames);
        // A combined CoCo image supplies both 8K banks, in Extended/Color BASIC order.
        boolean combined = extended != null && payloadSize(extended) == 16384;
        if (primary == null && !combined) {
            if (extendedNames != null) {
                return "Missing Color BASIC. Import bas12.rom (or bas13.rom, bas11.rom, "
                        + "bas10.rom). extbas11.rom is Extended BASIC; a .ccc game is not firmware.";
            }
            return "Missing firmware for " + label + ". Import " + primaryNames[0] + ".rom.";
        }
        if (primary != null && payloadSize(primary) < primarySize) {
            return primary.getName() + " is too small for " + label
                    + " firmware (needs at least " + primarySize + " bytes).";
        }
        if (extendedNames != null && extended == null) {
            return "Missing Extended Color BASIC. Import extbas11.rom or extbas10.rom.";
        }
        if (extended != null && payloadSize(extended) < extendedSize) {
            return extended.getName() + " is too small for Extended Color BASIC "
                    + "(needs at least " + extendedSize + " bytes).";
        }
        return null;
    }

    String[] arguments(File directory, File media) {
        String error = validationError(directory);
        if (error != null) throw new IllegalStateException(error);
        List<String> arguments = new ArrayList<>();
        arguments.add("-rompath");
        arguments.add(directory.getAbsolutePath());
        arguments.add("-machine");
        arguments.add(id);
        // Touch and Android gamepad input share this JNI source, selected by
        // the UI without restarting XRoar. The inactive port stays neutral.
        for (int port = 0; port < 2; port++) {
            arguments.add("-joy");
            arguments.add("android-touch-" + port);
            arguments.add("-joy-axis");
            arguments.add("0=touch:" + port);
            arguments.add("-joy-axis");
            arguments.add("1=touch:" + port);
            arguments.add("-joy-button");
            arguments.add("0=touch:" + port);
        }
        arguments.add("-joy-right");
        arguments.add("android-touch-0");
        arguments.add("-joy-left");
        arguments.add("android-touch-1");
        File primary = findRom(directory, primaryNames);
        if (primary != null) {
            addRom(arguments, primaryOption, primary);
        } else {
            // Do not overwrite the combined image's Color BASIC bank with an absent default.
            arguments.add("-no-bas");
        }
        addRom(arguments, "-extbas", findRom(directory, extendedNames));
        addRom(arguments, "-altbas", findRom(directory, alternateNames));
        if (media != null) {
            if (media.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                throw new IllegalStateException("Choose the ZIP again to extract the tape inside it.");
            }
            if (!media.isFile() || media.length() == 0) {
                throw new IllegalStateException("The selected program is missing or empty. Choose it again.");
            }
            // XRoar recognises .ccc as a cartridge and enables cartridge autorun for -run.
            arguments.add("-run");
            arguments.add(media.getAbsolutePath());
        }
        return arguments.toArray(new String[0]);
    }

    private static void addRom(List<String> arguments, String option, File rom) {
        if (rom != null) {
            arguments.add(option);
            arguments.add(rom.getAbsolutePath());
        }
    }

    private static File findRom(File directory, String[] names) {
        if (names == null) return null;
        File[] files = directory.listFiles(File::isFile);
        if (files == null) return null;
        for (String name : names) {
            for (String extension : new String[]{"", ".rom", ".bin"}) {
                for (File file : files) {
                    if (file.getName().equalsIgnoreCase(name + extension)) return file;
                }
            }
        }
        return null;
    }

    private static long payloadSize(File file) {
        // Match XRoar's treatment of optional short ROM headers.
        return file.length() - file.length() % 256;
    }
}
