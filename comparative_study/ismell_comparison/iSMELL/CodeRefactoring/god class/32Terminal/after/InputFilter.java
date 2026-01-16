package org.apache.karaf.webconsole.gogo;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

class InputFilter {
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
 
    private void ctrl_BS() {
        int dy = (cx - 1) / width;
        cursor_set(Math.max(scroll_area_y0, cy + dy), (cx - 1) % width);
    }

    private void ctrl_HT() {
        ctrl_HT(1);
    }

    private void ctrl_HT(int n) {
        if (n > 0 && cx >= width) {
            return;
        }
        if (n <= 0 && cx == 0) {
            return;
        }
        int ts = -1;
        for (int i = 0; i < tab_stops.size(); i++) {
            if (cx >= tab_stops.get(i)) {
                ts = i;
            }
        }
        ts += n;
        if (ts < tab_stops.size() && ts >= 0) {
            cursor_set_x(tab_stops.get(ts));
        } else {
            cursor_set_x(width - 1);
        }
    }

    private void ctrl_LF() {
        if (vt100_mode_lfnewline) {
            ctrl_CR();
        }
        if (cy == scroll_area_y1 - 1) {
            scroll_area_up(scroll_area_y0, scroll_area_y1);
        } else {
            cursor_down();
        }
    }

    private void ctrl_CR() {
        cursor_set_x(0);
    }

    private boolean dumb_write(int c) {
        if (c < 32) {
            if (c == 8) {
                ctrl_BS();
            } else if (c == 9) {
                ctrl_HT();
            } else if (c >= 10 && c <= 12) {
                ctrl_LF();
            } else if (c == 13) {
                ctrl_CR();
            }
            return true;
        }
        return false;
    }

    private void dumb_echo(int c) {
        if (eol) {
            if (vt100_mode_autowrap) {
                ctrl_CR();
                ctrl_LF();
            } else {
                cx = cursor_line_width(c)[1] - 1;
            }
        }
        if (vt100_mode_insert) {
            scroll_line_right(cy, cx);
        }
        if (vt100_charset_is_single_shift) {
            vt100_charset_is_single_shift = false;
        } else if (vt100_charset_is_graphical && ((c & 0xffe0) == 0x0060)) {
            c = vt100_charset_graph[c - 0x60];
        }
        poke(cy, cx, new int[] { attr | c });
        cursor_right();
    }
}