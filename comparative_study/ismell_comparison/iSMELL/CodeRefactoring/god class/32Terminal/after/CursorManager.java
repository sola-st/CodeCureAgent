package org.apache.karaf.webconsole.gogo;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

class CursorManager {
    private int width;
    private int height;
    private int attr;
    private boolean eol;
    private int cx;
    private int cy;
    private int[] screen;
    private int[] screen2;
    private State vt100_parse_state = State.None;
    private int vt100_parse_len;
    private int vt100_lastchar;
    private int vt100_parse_func;
    private String vt100_parse_param;
    private boolean vt100_mode_autowrap;
    private boolean vt100_mode_insert;
    private boolean vt100_charset_is_single_shift;
    private boolean vt100_charset_is_graphical;
    private boolean vt100_mode_lfnewline;
    private boolean vt100_mode_origin;
    private boolean vt100_mode_inverse;
    private boolean vt100_mode_cursorkey;
    private boolean vt100_mode_cursor;
    private boolean vt100_mode_alt_screen;
    private boolean vt100_mode_backspace;
    private boolean vt100_mode_column_switch;
    private boolean vt100_keyfilter_escape;
    private int[] vt100_charset_graph = new int[] {
            0x25ca, 0x2026, 0x2022, 0x3f,
            0xb6, 0x3f, 0xb0, 0xb1,
            0x3f, 0x3f, 0x2b, 0x2b,
            0x2b, 0x2b, 0x2b, 0xaf,
            0x2014, 0x2014, 0x2014, 0x5f,
            0x2b, 0x2b, 0x2b, 0x2b,
            0x7c, 0x2264, 0x2265, 0xb6,
            0x2260, 0xa3, 0xb7, 0x7f
    };
    private int vt100_charset_g_sel;
    private int[] vt100_charset_g = { 0, 0 };
    private Map<String, Object> vt100_saved;
    private Map<String, Object> vt100_saved2;
    private int vt100_saved_cx;
    private int vt100_saved_cy;
    private String vt100_out;

    private int scroll_area_y0;
    private int scroll_area_y1;

    private List<Integer> tab_stops;

    private int utf8_char;
    private int utf8_units_count;
    private int utf8_units_received;

    private AtomicBoolean dirty = new AtomicBoolean(true);

    private int[] cursor_line_width(int next_char) {
        int wx = utf8_charwidth(next_char);
        int lx = 0;
        for (int x = 0; x < Math.min(cx, width); x++) {
            int c = peek(cy, x, cy + 1, x + 1)[0] & 0xffff;
            wx += utf8_charwidth(c);
            lx += 1;
        }
        return new int[] { wx, lx };
    }

    private void cursor_up() {
        cursor_up(1);
    }

    private void cursor_up(int n) {
        cy = Math.max(scroll_area_y0, cy - n);
        setDirty();
    }

    private void cursor_down() {
        cursor_down(1);
    }

    private void cursor_down(int n) {
        cy = Math.min(scroll_area_y1 - 1, cy + n);
        setDirty();
    }

    private void cursor_left() {
        cursor_left(1);
    }

    private void cursor_left(int n) {
        eol = false;
        cx = Math.max(0, cx - n);
        setDirty();
    }

    private void cursor_right() {
        cursor_right(1);
    }

    private void cursor_right(int n) {
        eol = cx + n >= width;
        cx = Math.min(width - 1, cx + n);
        setDirty();
    }

    private void cursor_set_x(int x) {
        eol = false;
        cx = Math.max(0, x);
        setDirty();
    }

    private void cursor_set_y(int y) {
        cy = Math.max(0, Math.min(height - 1, y));
        setDirty();
    }

    private void cursor_set(int y, int x) {
        cursor_set_x(x);
        cursor_set_y(y);
    }
}