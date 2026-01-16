package org.apache.karaf.webconsole.gogo;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.karaf.webconsole.gogo.Terminal.State;

class ScreenManager {
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

    public ScreenManager(int width, int height) {
        this.width = width;
        this.height = height;
        resetScreen();
    }

    public void resetScreen() {
        // Reset screen logic
    }

    private int[] peek(int y0, int x0, int y1, int x1) {
        int from = width * y0 + x0;
        int to = width * (y1 - 1) + x1; 
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        int[] copy = new int[newLength];
        System.arraycopy(screen, from, copy, 0,
                         Math.min(screen.length - from, newLength));
        return copy;
    }

    private void poke(int y, int x, int[] s) {
        System.arraycopy(s, 0, screen, width * y + x, s.length);
        setDirty();
    }

    private void fill(int y0, int x0, int y1, int x1, int c) {
        int d0 = width * y0 + x0;
        int d1 = width * (y1 - 1) + x1;
        if (d0 <= d1) {
            Arrays.fill(screen, width * y0 + x0,  width * (y1 - 1) + x1, c);
            setDirty();
        }
    }

    private void clear(int y0, int x0, int y1, int x1) {
        fill(y0, x0, y1, x1, attr | 0x20);
    }
 
    private void scroll_area_up(int y0, int y1) {
        scroll_area_up(y0, y1, 1);
    }

    private void scroll_area_up(int y0, int y1, int n) {
        n = Math.min(y1 - y0, n);
        poke(y0, 0, peek(y0 + n, 0, y1, width));
        clear(y1-n, 0, y1, width);
    }

    private void scroll_area_down(int y0, int y1) {
        scroll_area_down(y0, y1, 1);
    }

    private void scroll_area_down(int y0, int y1, int n) {
        n = Math.min(y1 - y0, n);
        poke(y0 + n, 0, peek(y0, 0, y1-n, width));
        clear(y0, 0, y0 + n, width);
    }

    private void scroll_area_set(int y0, int y1) {
        y0 = Math.max(0, Math.min(height - 1, y0));
        y1 = Math.max(1, Math.min(height, y1));
        if (y1 > y0) {
            scroll_area_y0 = y0;
            scroll_area_y1 = y1;
        }
    }

    private void scroll_line_right(int y, int x) {
        scroll_line_right(y, x, 1);
    }

    private void scroll_line_right(int y, int x, int n) {
        if (x < width) {
            n = Math.min(width - cx, n);
            poke(y, x + n, peek(y, x, y + 1, width - n));
            clear(y, x, y + 1, x + n);
        }
    }

    private void scroll_line_left(int y, int x) {
        scroll_line_left(y, x, 1);
    }

    private void scroll_line_left(int y, int x, int n) {
        if (x < width) {
            n = Math.min(width - cx, n);
            poke(y, x, peek(y, x + n, y + 1, width));
            clear(y, width - n, y + 1, width);
        }
    }
 
    private void reset_screen() {
        // Screen
        screen = new int[width * height];
        Arrays.fill(screen, attr | 0x0020);
        screen2 = new int[width * height];
        Arrays.fill(screen2, attr | 0x0020);
        // Scroll parameters
        scroll_area_y0 = 0;
        scroll_area_y1 = height;
        // Cursor position
        cx = 0;
        cy = 0;
        // Tab stops
        tab_stops = new ArrayList<Integer>();
        for (int i = 7; i < width; i += 8) {
            tab_stops.add(i);
        }
    }
}