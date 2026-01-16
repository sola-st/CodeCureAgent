package org.apache.karaf.webconsole.gogo;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

class TerminalConfiguration {
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

    private void reset_hard() {
        // Attribute mask: 0x0XFB0000
        //	X:	Bit 0 - Underlined
        //		Bit 1 - Negative
        //		Bit 2 - Concealed
        //	F:	Foreground
        //	B:	Background
        attr = 0x00fe0000;
        // UTF-8 decoder
        utf8_units_count = 0;
        utf8_units_received = 0;
        utf8_char = 0;
        // Key filter
        vt100_keyfilter_escape = false;
        // Last char
        vt100_lastchar = 0;
        // Control sequences
        vt100_parse_len = 0;
        vt100_parse_state = State.None;
        vt100_parse_func = 0;
        vt100_parse_param = "";
        // Buffers
        vt100_out = "";
        // Invoke other resets
        reset_screen();
        reset_soft();
    }

    private void reset_soft() {
        // Attribute mask: 0x0XFB0000
        //	X:	Bit 0 - Underlined
        //		Bit 1 - Negative
        //		Bit 2 - Concealed
        //	F:	Foreground
        //	B:	Background
        attr = 0x00fe0000;
        // Scroll parameters
        scroll_area_y0 = 0;
        scroll_area_y1 = height;
        // Character sets
        vt100_charset_is_single_shift = false;
        vt100_charset_is_graphical = false;
        vt100_charset_g_sel = 0;
        vt100_charset_g = new int[] { 0, 0 };
        // Modes
        vt100_mode_insert = false;
        vt100_mode_lfnewline = false;
        vt100_mode_cursorkey = false;
        vt100_mode_column_switch = false;
        vt100_mode_inverse = false;
        vt100_mode_origin = false;
        vt100_mode_autowrap = true;
        vt100_mode_cursor = true;
        vt100_mode_alt_screen = false;
        vt100_mode_backspace = false;
        // Init DECSC state
        esc_DECSC();
        vt100_saved2 = vt100_saved;
        esc_DECSC();
    }
}