cmake_minimum_required(VERSION 3.22.1)

get_filename_component(XROAR_ROOT
    "${CMAKE_CURRENT_LIST_DIR}/../../../../vendor/xroar"
    ABSOLUTE
)
set(XROAR_SRC "${XROAR_ROOT}/src")
set(PORTALIB "${XROAR_ROOT}/portalib")

# This source set follows XRoar's Automake manifest for the Dragon, CoCo 3,
# MC-10, SDL3, and null-audio backends. Platform-specific desktop UIs and
# optional host audio backends are intentionally omitted.
set(XROAR_SOURCES
    "${XROAR_SRC}/ao.c"
    "${XROAR_SRC}/auto_kbd.c"
    "${XROAR_SRC}/ay891x.c"
    "${XROAR_SRC}/becker.c"
    "${XROAR_SRC}/blockdev.c"
    "${XROAR_SRC}/breakpoint.c"
    "${XROAR_SRC}/cart.c"
    "${XROAR_SRC}/clock.c"
    "${XROAR_SRC}/colourspace.c"
    "${XROAR_SRC}/crc16.c"
    "${XROAR_SRC}/crc32.c"
    "${XROAR_SRC}/debug.c"
    "${XROAR_SRC}/dkbd.c"
    "${XROAR_SRC}/events.c"
    "${XROAR_SRC}/filter.c"
    "${XROAR_SRC}/fs.c"
    "${XROAR_SRC}/hexs19.c"
    "${XROAR_SRC}/hkbd.c"
    "${XROAR_SRC}/hkbd_joystick.c"
    "${XROAR_SRC}/joystick.c"
    "${XROAR_SRC}/keyboard.c"
    "${XROAR_SRC}/logging.c"
    "${XROAR_SRC}/machine.c"
    "${XROAR_SRC}/mc10_cart.c"
    "${XROAR_SRC}/messenger.c"
    "${XROAR_SRC}/module.c"
    "${XROAR_SRC}/mos6551.c"
    "${XROAR_SRC}/ntsc.c"
    "${XROAR_SRC}/null/ui_null.c"
    "${XROAR_SRC}/part.c"
    "${XROAR_SRC}/path.c"
    "${XROAR_SRC}/printer.c"
    "${XROAR_SRC}/ram.c"
    "${XROAR_SRC}/rom.c"
    "${XROAR_SRC}/rombank.c"
    "${XROAR_SRC}/romlist.c"
    "${XROAR_SRC}/screenshot.c"
    "${XROAR_SRC}/serialise.c"
    "${XROAR_SRC}/snapshot.c"
    "${XROAR_SRC}/sndfile_compat.c"
    "${XROAR_SRC}/sound.c"
    "${XROAR_SRC}/symtab.c"
    "${XROAR_SRC}/tape.c"
    "${XROAR_SRC}/tape_cas.c"
    "${XROAR_SRC}/tape_sndfile.c"
    "${XROAR_SRC}/ui.c"
    "${XROAR_SRC}/vdg_pal.c"
    "${XROAR_SRC}/vdg_palette.c"
    "${XROAR_SRC}/vdisk.c"
    "${XROAR_SRC}/vdrive.c"
    "${XROAR_SRC}/vo.c"
    "${XROAR_SRC}/vo_render.c"
    "${XROAR_SRC}/xconfig.c"
    "${XROAR_SRC}/xroar.c"
    "${XROAR_SRC}/main_unix.c"
    "${XROAR_SRC}/dragon/coco.c"
    "${XROAR_SRC}/dragon/immunity.c"
    "${XROAR_SRC}/dragon/deluxecoco.c"
    "${XROAR_SRC}/dragon/dragon.c"
    "${XROAR_SRC}/dragon/dragon32.c"
    "${XROAR_SRC}/dragon/dragon64.c"
    "${XROAR_SRC}/dragon/dragonpro.c"
    "${XROAR_SRC}/coco3.c"
    "${XROAR_SRC}/mc10.c"
    "${XROAR_SRC}/mc6801/mc6801.c"
    "${XROAR_SRC}/mc6809/hd6309.c"
    "${XROAR_SRC}/mc6809/mc6809.c"
    "${XROAR_SRC}/mc6821.c"
    "${XROAR_SRC}/mc6847/font-6847.c"
    "${XROAR_SRC}/mc6847/font-6847t1.c"
    "${XROAR_SRC}/mc6847/mc6847.c"
    "${XROAR_SRC}/mc6883.c"
    "${XROAR_SRC}/samx8.c"
    "${XROAR_SRC}/tcc1014/font-gime.c"
    "${XROAR_SRC}/tcc1014/tcc1014.c"
    "${XROAR_SRC}/deltados.c"
    "${XROAR_SRC}/dragondos.c"
    "${XROAR_SRC}/gmc.c"
    "${XROAR_SRC}/ide.c"
    "${XROAR_SRC}/idecart.c"
    "${XROAR_SRC}/mooh.c"
    "${XROAR_SRC}/mpi.c"
    "${XROAR_SRC}/nx32.c"
    "${XROAR_SRC}/orch90.c"
    "${XROAR_SRC}/rsdos.c"
    "${XROAR_SRC}/sn76489.c"
    "${XROAR_SRC}/spi65.c"
    "${XROAR_SRC}/spi_sdcard.c"
    "${XROAR_SRC}/wd279x.c"
    "${XROAR_SRC}/mcx128.c"
    "${XROAR_SRC}/null/ao_null.c"
    "${XROAR_SRC}/sdl3/ao_sdl3.c"
    "${XROAR_SRC}/sdl3/common.c"
    "${XROAR_SRC}/sdl3/joystick_sdl3.c"
    "${XROAR_SRC}/sdl3/keyboard_sdl3.c"
    "${XROAR_SRC}/sdl3/ui_sdl3.c"
    "${XROAR_SRC}/sdl3/vo_sdl3.c"
)

set(PORTALIB_SOURCES
    "${PORTALIB}/c-ctype.c"
    "${PORTALIB}/c-strcasecmp.c"
    "${PORTALIB}/c-strncasecmp.c"
    "${PORTALIB}/delegate.c"
    "${PORTALIB}/intfuncs.c"
    "${PORTALIB}/pl-estrcpy.c"
    "${PORTALIB}/sds.c"
    "${PORTALIB}/sdsx.c"
    "${PORTALIB}/slist.c"
    "${PORTALIB}/strnlen.c"
    "${PORTALIB}/strsep.c"
    "${PORTALIB}/xmalloc.c"
)

foreach(source IN LISTS XROAR_SOURCES PORTALIB_SOURCES)
    if(NOT EXISTS "${source}")
        message(FATAL_ERROR "Missing XRoar source file: ${source}")
    endif()
endforeach()

add_library(main SHARED ${XROAR_SOURCES} ${PORTALIB_SOURCES})
target_include_directories(main PRIVATE
    "${XROAR_ROOT}"
    "${XROAR_SRC}"
    "${PORTALIB}"
)
target_compile_features(main PRIVATE c_std_11)
target_compile_definitions(main PRIVATE
    HAVE_SDL3=1
    UI_SDL3=1
    WANT_UI_SDL=1
    HAVE_SIGNAL_H=1
    HAVE_NULL_AUDIO=1
    HAVE_REGEX_H=1
    HAVE_STRNLEN=1
    HAVE_STRSEP=1
    LOGGING=1
    ENABLE_SNAPSHOT=1
    WANT_BECKER=1
    WANT_TRAPS=1
    WANT_MACHINE_ARCH_DRAGON=1
    WANT_MACHINE_ARCH_COCO3=1
    WANT_MACHINE_ARCH_MC10=1
    WANT_PART_MC6801=1
    WANT_PART_MC6809=1
    WANT_PART_MC6821=1
    WANT_PART_MC6847=1
    WANT_PART_MC6883=1
    WANT_PART_TCC1014=1
    WANT_CART_ARCH_DRAGON=1
    WANT_CART_ARCH_MC10=1
    PACKAGE_VERSION=\"1.13.0.dev\"
    PACKAGE_TEXT=\"XRoar 1.13.0.dev\"
    PACKAGE_YEAR=\"2026\"
)
target_link_libraries(main PRIVATE SDL3::SDL3 m)
