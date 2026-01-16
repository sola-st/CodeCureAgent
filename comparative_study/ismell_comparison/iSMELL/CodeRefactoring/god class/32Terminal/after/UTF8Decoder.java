package org.apache.karaf.webconsole.gogo;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

class UTF8Decoder {
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

    private String utf8_decode(String d) {
        StringBuilder o = new StringBuilder();
        for (char c : d.toCharArray()) {
            if (utf8_units_count != utf8_units_received) {
                utf8_units_received++;
                if ((c & 0xc0) == 0x80) {
                    utf8_char = (utf8_char << 6) | (c & 0x3f);
                    if (utf8_units_count == utf8_units_received) {
                        if (utf8_char < 0x10000) {
                            o.append((char) utf8_char);
                        }
                        utf8_units_count = utf8_units_received = 0;
                    }
                } else {
                    o.append('?');
                    while (utf8_units_received-- > 0) {
                        o.append('?');
                    }
                    utf8_units_count = 0;
                }
            } else {
                if ((c & 0x80) == 0x00) {
                    o.append(c);
                } else if ((c & 0xe0) == 0xc0) {
                    utf8_units_count = 1;
                    utf8_char = c & 0x1f;
                } else if ((c & 0xf0) == 0xe0) {
                    utf8_units_count = 2;
                    utf8_char = c & 0x0f;
                } else if ((c & 0xf8) == 0xf0) {
                    utf8_units_count = 3;
                    utf8_char = c & 0x07;
                } else {
                    o.append('?');
                }

            }
        }
        return o.toString();
    }
 
    private int utf8_charwidth(int c) {
        if (c >= 0x2e80) {
            return 2;
        } else {
            return 1;
        }
    }
}