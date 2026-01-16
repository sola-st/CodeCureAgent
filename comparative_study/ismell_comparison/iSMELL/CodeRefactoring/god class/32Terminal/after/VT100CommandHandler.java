package org.apache.karaf.webconsole.gogo;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

class VT100CommandHandler {
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

    private void vt100_charset_update() {
        vt100_charset_is_graphical = (vt100_charset_g[vt100_charset_g_sel] == 2);
    }

    private void vt100_charset_set(int g) {
        // Invoke active character set
        vt100_charset_g_sel = g;
        vt100_charset_update();
    }

    private void vt100_charset_select(int g, int charset) {
        // Select charset
        vt100_charset_g[g] = charset;
        vt100_charset_update();
    }

    private void vt100_setmode(String p, boolean state) {
        // Set VT100 mode
        String[] ps = vt100_parse_params(p, new String[0]);
        for (String m : ps) {
            // 1 : GATM: Guarded area transfer
            // 2 : KAM: Keyboard action
            // 3 : CRM: Control representation
            if ("4".equals(m)) {
                // Insertion replacement mode
                vt100_mode_insert = state;
            // 5 : SRTM: Status reporting transfer
            // 7 : VEM: Vertical editing
            // 10 : HEM: Horizontal editing
            // 11 : PUM: Positioning nit
            // 12 : SRM: Send/receive
            // 13 : FEAM: Format effector action
            // 14 : FETM: Format effector transfer
            // 15 : MATM: Multiple area transfer
            // 16 : TTM: Transfer termination
            // 17 : SATM: Selected area transfer
            // 18 : TSM: Tabulation stop
            // 19 : EBM: Editing boundary
            } else if ("20".equals(m)) {
                // LNM: Line feed/new line
                vt100_mode_lfnewline = state;
            } else if ("?1".equals(m)) {
                // DECCKM: Cursor keys
                vt100_mode_cursorkey = state;
            // ?2 : DECANM: ANSI
            } else if ("?3".equals(m)) {
                // DECCOLM: Column
                if (vt100_mode_column_switch) {
                    if (state) {
                        width = 132;
                    } else {
                        width = 80;
                    }
                    reset_screen();
                }
            // ?4 : DECSCLM: Scrolling
            } else if ("?5".equals(m)) {
                // DECSCNM: Screen
                vt100_mode_inverse = state;
            } else if ("?6".equals(m)) {
                // DECOM: Origin
                vt100_mode_origin = state;
                if (state) {
                    cursor_set(scroll_area_y0, 0);
                } else {
                    cursor_set(0, 0);
                }
            } else if ("?7".equals(m)) {
                // DECAWM: Autowrap
                vt100_mode_autowrap = state;
            // ?8 : DECARM: Autorepeat
            // ?9 : Interlacing
            // ?18 : DECPFF: Print form feed
            // ?19 : DECPEX: Printer extent
            } else if ("?25".equals(m)) {
                // DECTCEM: Text cursor enable
                vt100_mode_cursor = state;
            // ?34 : DECRLM: Cursor direction, right to left
            // ?35 : DECHEBM: Hebrew keyboard mapping
            // ?36 : DECHEM: Hebrew encoding mode
            } else if ("?40".equals(m)) {
                // Column switch control
                vt100_mode_column_switch = state;
            // ?42 : DECNRCM: National replacement character set
            } else if ("?47".equals(m)) {
                // Alternate screen mode
                if ((state && !vt100_mode_alt_screen) || (!state && vt100_mode_alt_screen)) {
                    int[] s = screen; screen = screen2; screen2 = s;
                    Map<String, Object> map = vt100_saved; vt100_saved = vt100_saved2; vt100_saved2 = map;
                }
                vt100_mode_alt_screen = state;
            // ?57 : DECNAKB: Greek keyboard mapping
            } else if ("?67".equals(m)) {
                // DECBKM: Backarrow key
                vt100_mode_backspace = state;
            }
            // ?98 : DECARSM: auto-resize
            // ?101 : DECCANSM: Conceal answerback message
            // ?109 : DECCAPSLK: caps lock
        }
    }

    private void ctrl_SO() {
        vt100_charset_set(1);
    }

    private void ctrl_SI() {
        vt100_charset_set(0);
    }

    private void esc_CSI() {
        vt100_parse_reset(State.Csi);
    }

    private void esc_DECALN() {
        fill(0, 0, height, width, 0x00fe0045);
    }

    private void esc_G0_0() {
        vt100_charset_select(0, 0);
    }
    private void esc_G0_1() {
        vt100_charset_select(0, 1);
    }
    private void esc_G0_2() {
        vt100_charset_select(0, 2);
    }
    private void esc_G0_3() {
        vt100_charset_select(0, 3);
    }
    private void esc_G0_4() {
        vt100_charset_select(0, 4);
    }

    private void esc_G1_0() {
        vt100_charset_select(1, 0);
    }
    private void esc_G1_1() {
        vt100_charset_select(1, 1);
    }
    private void esc_G1_2() {
        vt100_charset_select(1, 2);
    }
    private void esc_G1_3() {
        vt100_charset_select(1, 3);
    }
    private void esc_G1_4() {
        vt100_charset_select(1, 4);
    }

    private void esc_DECSC() {
        vt100_saved = new HashMap<String, Object>();
        vt100_saved.put("cx", cx);
        vt100_saved.put("cy", cy);
        vt100_saved.put("attr", attr);
        vt100_saved.put("vt100_charset_g_sel", vt100_charset_g_sel);
        vt100_saved.put("vt100_charset_g", vt100_charset_g);
        vt100_saved.put("vt100_mode_autowrap", vt100_mode_autowrap);
        vt100_saved.put("vt100_mode_origin", vt100_mode_origin);
    }

    private void esc_DECRC() {
        cx = (Integer) vt100_saved.get("cx");
        cy = (Integer) vt100_saved.get("cy");
        attr = (Integer) vt100_saved.get("attr");
        vt100_charset_g_sel = (Integer) vt100_saved.get("vt100_charset_g_sel");
        vt100_charset_g = (int[]) vt100_saved.get("vt100_charset_g");
        vt100_charset_update();
        vt100_mode_autowrap = (Boolean) vt100_saved.get("vt100_mode_autowrap");
        vt100_mode_origin = (Boolean) vt100_saved.get("vt100_mode_origin");
    }

    private void esc_IND() {
        ctrl_LF();
    }

    private void esc_NEL() {
        ctrl_CR();
        ctrl_LF();
    }

    private void esc_HTS() {
        csi_CTC("0");
    }

    private void esc_RI() {
        if (cy == scroll_area_y0) {
            scroll_area_down(scroll_area_y0, scroll_area_y1);
        } else {
            cursor_up();
        }
    }

    private void esc_SS2() {
        vt100_charset_is_single_shift = true;
    }

    private void esc_SS3() {
        vt100_charset_is_single_shift = true;
    }

    private void esc_DCS() {
        vt100_parse_reset(State.Str);
    }

    private void esc_SOS() {
        vt100_parse_reset(State.Str);
    }

    private void esc_DECID() {
        csi_DA("0");
    }

    private void esc_ST() {
    }

    private void esc_OSC() {
        vt100_parse_reset(State.Str);
    }

    private void esc_PM() {
        vt100_parse_reset(State.Str);
    }

    private void esc_APC() {
        vt100_parse_reset(State.Str);
    }

    private void esc_RIS() {
        reset_hard();
    }

    private void csi_ICH(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        scroll_line_right(cy, cx, ps[0]);
    }

    private void csi_CUU(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        cursor_up(Math.max(1, ps[0]));
    }

    private void csi_CUD(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        cursor_down(Math.max(1, ps[0]));
    }

    private void csi_CUF(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        cursor_right(Math.max(1, ps[0]));
    }

    private void csi_CUB(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        cursor_left(Math.max(1, ps[0]));
    }

    private void csi_CNL(String p) {
        csi_CUD(p);
        ctrl_CR();
    }

    private void csi_CPL(String p) {
        csi_CUU(p);
        ctrl_CR();
    }

    private void csi_CHA(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        cursor_set_x(ps[0] - 1);
    }

    private void csi_CUP(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1, 1 });
        if (vt100_mode_origin) {
            cursor_set(scroll_area_y0 + ps[0] - 1, ps[1] - 1);
        } else {
            cursor_set(ps[0] - 1, ps[1] - 1);
        }
    }

    private void csi_CHT(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        ctrl_HT(Math.max(1, ps[0]));
    }

    private void csi_ED(String p) {
        String[] ps = vt100_parse_params(p, new String[] { "0" });
        if ("0".equals(ps[0])) {
            clear(cy, cx, height, width);
        } else if ("1".equals(ps[0])) {
            clear(0, 0, cy + 1, cx + 1);
        } else if ("2".equals(ps[0])) {
            clear(0, 0, height, width);
        }
    }

    private void csi_EL(String p) {
        String[] ps = vt100_parse_params(p, new String[] { "0" });
        if ("0".equals(ps[0])) {
            clear(cy, cx, cy + 1, width);
        } else if ("1".equals(ps[0])) {
            clear(cy, 0, cy + 1, cx + 1);
        } else if ("2".equals(ps[0])) {
            clear(cy, 0, cy + 1, width);
        }
    }

    private void csi_IL(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        if (cy >= scroll_area_y0 && cy < scroll_area_y1) {
            scroll_area_down(cy, scroll_area_y1, Math.max(1, ps[0]));
        }
    }

    private void csi_DL(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        if (cy >= scroll_area_y0 && cy < scroll_area_y1) {
            scroll_area_up(cy, scroll_area_y1, Math.max(1, ps[0]));
        }
    }

    private void csi_DCH(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        scroll_line_left(cy, cx, Math.max(1, ps[0]));
    }

    private void csi_SU(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        scroll_area_up(scroll_area_y0, scroll_area_y1, Math.max(1, ps[0]));
    }

    private void csi_SD(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        scroll_area_down(scroll_area_y0, scroll_area_y1, Math.max(1, ps[0]));
    }

    private void csi_CTC(String p) {
        String[] ps = vt100_parse_params(p, new String[] { "0" });
        for (String m : ps) {
            if ("0".equals(m)) {
                if (tab_stops.indexOf(cx) < 0) {
                    tab_stops.add(cx);
                    Collections.sort(tab_stops);
                }
            } else if ("2".equals(m)) {
                tab_stops.remove(Integer.valueOf(cx));
            } else if ("5".equals(m)) {
                tab_stops = new ArrayList<Integer>();
            }
        }
    }

    private void csi_ECH(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        int n = Math.min(width - cx, Math.max(1, ps[0]));
        clear(cy, cx, cy + 1, cx + n);
    }

    private void csi_CBT(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        ctrl_HT(1 - Math.max(1, ps[0]));
    }

    private void csi_HPA(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        cursor_set_x(ps[0] - 1);
    }

    private void csi_HPR(String p) {
        csi_CUF(p);
    }

    private void csi_REP(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        if (vt100_lastchar < 32) {
            return;
        }
        int n = Math.min(2000, Math.max(1, ps[0]));
        while (n-- > 0) {
            dumb_echo(vt100_lastchar);
        }
        vt100_lastchar = 0;
    }

    private void csi_DA(String p) {
        String[] ps = vt100_parse_params(p, new String[] { "0" });
        if ("0".equals(ps[0])) {
            vt100_out = "\u001b[?1;2c";
        } else if (">0".equals(ps[0]) || ">".equals(ps[0])) {
            vt100_out = "\u001b[>0;184;0c";
        }
    }

    private void csi_VPA(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1 });
        cursor_set_y(ps[0] - 1);
    }

    private void csi_VPR(String p) {
        csi_CUD(p);
    }

    private void csi_HVP(String p) {
        csi_CUP(p);
    }

    private void csi_TBC(String p) {
        String[] ps = vt100_parse_params(p, new String[] { "0" });
        if ("0".equals(ps[0])) {
            csi_CTC("2");
        } else if ("3".equals(ps[0])) {
            csi_CTC("5");
        }
    }

    private void csi_SM(String p) {
        vt100_setmode(p, true);
    }

    private void csi_RM(String p) {
        vt100_setmode(p, false);
    }

    private void csi_SGR(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 0 });
        for (int m : ps) {
            if (m == 0) {
                attr = 0x00fe0000;
            } else if (m == 1) {
                attr |= 0x08000000;
            } else if (m == 4) {
                attr |= 0x01000000;
            } else if (m == 7) {
                attr |= 0x02000000;
            } else if (m == 8) {
                attr |= 0x04000000;
            } else if (m == 24) {
                attr &= 0x7eff0000;
            } else if (m == 27) {
                attr &= 0x7dff0000;
            } else if (m == 28) {
                attr &= 0x7bff0000;
            } else if (m >= 30 && m <= 37) {
                attr = (attr & 0x7f0f0000) | ((m - 30) << 20);
            } else if (m == 39) {
                attr = (attr & 0x7f0f0000) | 0x00f00000;
            } else if (m >= 40 && m <= 47) {
                attr = (attr & 0x7ff00000) | ((m - 40) << 16);
            } else if (m == 49) {
                attr = (attr & 0x7ff00000) | 0x000e0000;
            }
        }
    }

    private void csi_DSR(String p) {
        String[] ps = vt100_parse_params(p, new String[] { "0" });
        if ("5".equals(ps[0])) {
            vt100_out = "\u001b[0n";
        } else if ("6".equals(ps[0])) {
            vt100_out = "\u001b[" + (cy + 1) + ";" + (cx + 1) + "R";
        } else if ("7".equals(ps[0])) {
            vt100_out = "gogo-term";
        } else if ("8".equals(ps[0])) {
            vt100_out = "1.0-SNAPSHOT";
        } else if ("?6".equals(ps[0])) {
            vt100_out = "\u001b[" + (cy + 1) + ";" + (cx + 1) + ";0R";
        } else if ("?15".equals(ps[0])) {
            vt100_out = "\u001b[?13n";
        } else if ("?25".equals(ps[0])) {
            vt100_out = "\u001b[?20n";
        } else if ("?26".equals(ps[0])) {
            vt100_out = "\u001b[?27;1n";
        } else if ("?53".equals(ps[0])) {
            vt100_out = "\u001b[?53n";
        }
        // ?75 : Data Integrity report
        // ?62 : Macro Space report
        // ?63 : Memory Checksum report
    }

    private void csi_DECSTBM(String p) {
        int[] ps = vt100_parse_params(p, new int[] { 1, height });
        scroll_area_set(ps[0] - 1, ps[1]);
        if (vt100_mode_origin) {
            cursor_set(scroll_area_y0, 0);
        } else {
            cursor_set(0, 0);
        }
    }

    private void csi_SCP(String p) {
        vt100_saved_cx = cx;
        vt100_saved_cy = cy;
    }

    private void csi_RCP(String p) {
        cx = vt100_saved_cx;
        cy = vt100_saved_cy;
    }

    private void csi_DECREQTPARM(String p) {
        String[] ps = vt100_parse_params(p, new String[0]);
        if ("0".equals(ps[0])) {
            vt100_out = "\u001b[2;1;1;112;112;1;0x";
        } else if ("1".equals(ps[0])) {
            vt100_out = "\u001b[3;1;1;112;112;1;0x";
        }
    }

    private void csi_DECSTR(String p) {
        reset_soft();
    }
}