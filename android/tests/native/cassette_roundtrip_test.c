/* Exercise the real upstream CAS encoder/decoder, not a mock tape writer. */
#include <assert.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "logging.h"
#include "tape.h"

struct logging logging;
static bool write_error;
void android_tape_note_write_error(void) { write_error = true; }
struct tape *tape_new(void) { return calloc(1, sizeof(struct tape)); }
void tape_free(struct tape *t) { free(t); }

static uint8_t source[2048];
static size_t source_size;
static void block(uint8_t type, const uint8_t *data, size_t length) {
    assert(length <= 255);
    for (int i = 0; i < 128; i++) source[source_size++] = 0x55;
    source[source_size++] = 0x3c;
    source[source_size++] = type;
    source[source_size++] = (uint8_t)length;
    uint8_t checksum = type + length;
    for (size_t i = 0; i < length; i++) {
        source[source_size++] = data[i];
        checksum += data[i];
    }
    source[source_size++] = checksum;
    source[source_size++] = 0x55;
}
static bool contains(const uint8_t *bytes, size_t length, const uint8_t *wanted, size_t size) {
    for (size_t i = 0; i + size <= length; i++)
        if (memcmp(bytes + i, wanted, size) == 0) return true;
    return false;
}
static void record(struct tape *output) {
    for (size_t i = 0; i < source_size; i++) {
        for (int bit = 0; bit < 8; bit++) {
            int ticks = (source[i] & (1 << bit)) ? TAPE_BIT1_LENGTH / 2 : TAPE_BIT0_LENGTH / 2;
            assert(tape_sample_out(output, 0, ticks) == 0);
            assert(tape_sample_out(output, 255, ticks) == 0);
        }
    }
}
int main(int argc, char **argv) {
    assert(argc == 2);
    const uint8_t header[15] = {'T','E','S','T',' ',' ',' ',' ',0,0xff,0xff,0,0,0,0};
    const uint8_t program[] = "10 PRINT \"SAVED\"\r";
    block(0, header, sizeof(header));
    block(1, program, sizeof(program) - 1);
    block(0xff, NULL, 0);
    struct tape *output = tape_cas_open(argv[1], "wb");
    assert(output);
    record(output);
    tape_close(output); /* Real motor-off, data commit, CUE write and fclose. */
    assert(!write_error);
    FILE *file = fopen(argv[1], "rb");
    assert(file);
    uint8_t saved[8192];
    size_t size = fread(saved, 1, sizeof(saved), file);
    assert(fclose(file) == 0 && size > 0);
    assert(contains(saved, size, header, sizeof(header)));
    assert(contains(saved, size, program, sizeof(program) - 1));

    struct tape *input = tape_cas_open(argv[1], "rb");
    assert(input);
    uint8_t decoded[8192] = {0};
    size_t decoded_size = 0;
    int bits = 0, byte = 0, width, sense;
    while ((sense = input->module->pulse_in(input, &width)) != -1) {
        if (sense != 1) continue;
        /* CAS playback returns two equal-width half waves per bit. */
        byte |= (width * 2 < TAPE_AV_BIT_LENGTH ? 1 : 0) << bits++;
        if (bits == 8) {
            assert(decoded_size < sizeof(decoded));
            decoded[decoded_size++] = (uint8_t)byte;
            bits = byte = 0;
        }
    }
    tape_close(input);
    assert(contains(decoded, decoded_size, header, sizeof(header)));
    assert(contains(decoded, decoded_size, program, sizeof(program) - 1));

    output = tape_cas_open("/dev/full", "wb");
    if (output) {
        record(output);
        tape_close(output);
        assert(write_error); /* A failed flush must reach the Android Finish action. */
    }
    puts("PASS: real CAS waveform recording, close/flush, program round-trip and write-error reporting");
}
