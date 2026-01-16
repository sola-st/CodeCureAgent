package org.apache.karaf.webconsole.gogo;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

class VT100Parser {
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

    private String[] vt100_parse_params(String p, String[] defaults) {
        String prefix = "";
        if (p.length() > 0) {
            if (p.charAt(0) >= '<' && p.charAt(0) <= '?') {
                prefix = "" + p.charAt(0);
                p = p.substring(1);
            }
        }
        String[] ps = p.split(";");
        int n = Math.max(ps.length, defaults.length);
        String[] values = new String[n];
        for (int i = 0; i < n; i++) {
            String value = null;
            if (i < ps.length && ps[i].length() > 0) {
                value = prefix + ps[i];
            }
            if (value == null && i < defaults.length) {
                value = defaults[i];
            }
            if (value == null) {
                value = "";
            }
            values[i] = value;
        }
        return values;
    }

    private int[] vt100_parse_params(String p, int[] defaults) {
        String prefix = "";
        p = p == null ? "" : p;
        if (p.length() > 0) {
            if (p.charAt(0) >= '<' && p.charAt(0) <= '?') {
                prefix = p.substring(0, 1);
                p = p.substring(1);
            }
        }
        String[] ps = p.split(";");
        int n = Math.max(ps.length, defaults.length);
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            Integer value = null;
            if (i < ps.length) {
                String v = prefix + ps[i];
                try {
                    value = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                }
            }
            if (value == null && i < defaults.length) {
                value = defaults[i];
            }
            if (value == null) {
                value = 0;
            }
            values[i] = value;
        }
        return values;
    }
    
    private void vt100_parse_reset() {
        vt100_parse_reset(State.None);
    }

    private void vt100_parse_reset(State state) {
        vt100_parse_state = state;
        vt100_parse_len = 0;
        vt100_parse_func = 0;
        vt100_parse_param = "";
    }

    private void vt100_parse_process() {
        if (vt100_parse_state == State.Esc) {
            switch (vt100_parse_func) {
                case 0x0036: /* DECBI */ break;
                case 0x0037: esc_DECSC(); break;
                case 0x0038: esc_DECRC(); break;
                case 0x0042: /* BPH */ break;
                case 0x0043: /* NBH */ break;
                case 0x0044: esc_IND(); break;
                case 0x0045: esc_NEL(); break;
                case 0x0046: /* SSA */ esc_NEL(); break;
                case 0x0048: esc_HTS(); break;
                case 0x0049: /* HTJ */ break;
                case 0x004A: /* VTS */ break;
                case 0x004B: /* PLD */ break;
                case 0x004C: /* PLU */ break;
                case 0x004D: esc_RI(); break;
                case 0x004E: esc_SS2(); break;
                case 0x004F: esc_SS3(); break;
                case 0x0050: esc_DCS(); break;
                case 0x0051: /* PU1 */ break;
                case 0x0052: /* PU2 */ break;
                case 0x0053: /* STS */ break;
                case 0x0054: /* CCH */ break;
                case 0x0055: /* MW */ break;
                case 0x0056: /* SPA */ break;
                case 0x0057: /* ESA */ break;
                case 0x0058: esc_SOS(); break;
                case 0x005A: /* SCI */ break;
                case 0x005B: esc_CSI(); break;
                case 0x005C: esc_ST(); break;
                case 0x005D: esc_OSC(); break;
                case 0x005E: esc_PM(); break;
                case 0x005F: esc_APC(); break;
                case 0x0060: /* DMI */ break;
                case 0x0061: /* INT */ break;
                case 0x0062: /* EMI */ break;
                case 0x0063: esc_RIS(); break;
                case 0x0064: /* CMD */ break;
                case 0x006C: /* RM */ break;
                case 0x006E: /* LS2 */ break;
                case 0x006F: /* LS3 */ break;
                case 0x007C: /* LS3R */ break;
                case 0x007D: /* LS2R */ break;
                case 0x007E: /* LS1R */ break;
                case 0x2338: esc_DECALN(); break;
                case 0x2841: esc_G0_0(); break;
                case 0x2842: esc_G0_1(); break;
                case 0x2830: esc_G0_2(); break;
                case 0x2831: esc_G0_3(); break;
                case 0x2832: esc_G0_4(); break;
                case 0x2930: esc_G1_2(); break;
                case 0x2931: esc_G1_3(); break;
                case 0x2932: esc_G1_4(); break;
                case 0x2941: esc_G1_0(); break;
                case 0x2942: esc_G1_1(); break;
            }
            if (vt100_parse_state == State.Esc) {
                vt100_parse_reset();
            }
        } else {
            switch (vt100_parse_func) {
                case 0x0040: csi_ICH(vt100_parse_param); break;
                case 0x0041: csi_CUU(vt100_parse_param); break;
                case 0x0042: csi_CUD(vt100_parse_param); break;
                case 0x0043: csi_CUF(vt100_parse_param); break;
                case 0x0044: csi_CUB(vt100_parse_param); break;
                case 0x0045: csi_CNL(vt100_parse_param); break;
                case 0x0046: csi_CPL(vt100_parse_param); break;
                case 0x0047: csi_CHA(vt100_parse_param); break;
                case 0x0048: csi_CUP(vt100_parse_param); break;
                case 0x0049: csi_CHT(vt100_parse_param); break;
                case 0x004A: csi_ED(vt100_parse_param); break;
                case 0x004B: csi_EL(vt100_parse_param); break;
                case 0x004C: csi_IL(vt100_parse_param); break;
                case 0x004D: csi_DL(vt100_parse_param); break;
                case 0x004E: /* EF */ break;
                case 0x004F: /* EA */ break;
                case 0x0050: csi_DCH(vt100_parse_param); break;
                case 0x0051: /* SEE */ break;
                case 0x0052: /* CPR */ break;
                case 0x0053: csi_SU(vt100_parse_param); break;
                case 0x0054: csi_SD(vt100_parse_param); break;
                case 0x0055: /* NP */ break;
                case 0x0056: /* PP */ break;
                case 0x0057: csi_CTC(vt100_parse_param); break;
                case 0x0058: csi_ECH(vt100_parse_param); break;
                case 0x0059: /* CVT */ break;
                case 0x005A: csi_CBT(vt100_parse_param); break;
                case 0x005B: /* SRS */ break;
                case 0x005C: /* PTX */ break;
                case 0x005D: /* SDS */ break;
                case 0x005E: /* SIMD */ break;
                case 0x0060: csi_HPA(vt100_parse_param); break;
                case 0x0061: csi_HPR(vt100_parse_param); break;
                case 0x0062: csi_REP(vt100_parse_param); break;
                case 0x0063: csi_DA(vt100_parse_param); break;
                case 0x0064: csi_VPA(vt100_parse_param); break;
                case 0x0065: csi_VPR(vt100_parse_param); break;
                case 0x0066: csi_HVP(vt100_parse_param); break;
                case 0x0067: csi_TBC(vt100_parse_param); break;
                case 0x0068: csi_SM(vt100_parse_param); break;
                case 0x0069: /* MC */ break;
                case 0x006A: /* HPB */ break;
                case 0x006B: /* VPB */ break;
                case 0x006C: csi_RM(vt100_parse_param); break;
                case 0x006D: csi_SGR(vt100_parse_param); break;
                case 0x006E: csi_DSR(vt100_parse_param); break;
                case 0x006F: /* DAQ */ break;
                case 0x0072: csi_DECSTBM(vt100_parse_param); break;
                case 0x0073: csi_SCP(vt100_parse_param); break;
                case 0x0075: csi_RCP(vt100_parse_param); break;
                case 0x0078: csi_DECREQTPARM(vt100_parse_param); break;
                case 0x2040: /* SL */ break;
                case 0x2041: /* SR */ break;
                case 0x2042: /* GSM */ break;
                case 0x2043: /* GSS */ break;
                case 0x2044: /* FNT */ break;
                case 0x2045: /* TSS */ break;
                case 0x2046: /* JFY */ break;
                case 0x2047: /* SPI */ break;
                case 0x2048: /* QUAD */ break;
                case 0x2049: /* SSU */ break;
                case 0x204A: /* PFS */ break;
                case 0x204B: /* SHS */ break;
                case 0x204C: /* SVS */ break;
                case 0x204D: /* IGS */ break;
                case 0x204E: /* deprecated: HTSA */ break;
                case 0x204F: /* IDCS */ break;
                case 0x2050: /* PPA */ break;
                case 0x2051: /* PPR */ break;
                case 0x2052: /* PPB */ break;
                case 0x2053: /* SPD */ break;
                case 0x2054: /* DTA */ break;
                case 0x2055: /* SLH */ break;
                case 0x2056: /* SLL */ break;
                case 0x2057: /* FNK */ break;
                case 0x2058: /* SPQR */ break;
                case 0x2059: /* SEF */ break;
                case 0x205A: /* PEC */ break;
                case 0x205B: /* SSW */ break;
                case 0x205C: /* SACS */ break;
                case 0x205D: /* SAPV */ break;
                case 0x205E: /* STAB */ break;
                case 0x205F: /* GCC */ break;
                case 0x2060: /* TAPE */ break;
                case 0x2061: /* TALE */ break;
                case 0x2062: /* TAC */ break;
                case 0x2063: /* TCC */ break;
                case 0x2064: /* TSR */ break;
                case 0x2065: /* SCO */ break;
                case 0x2066: /* SRCS */ break;
                case 0x2067: /* SCS */ break;
                case 0x2068: /* SLS */ break;
                case 0x2069: /* SPH */ break;
                case 0x206A: /* SPL */ break;
                case 0x206B: /* SCP */ break;
                case 0x2170: csi_DECSTR(vt100_parse_param); break;
                case 0x2472: /* DECCARA */ break;
                case 0x2477: /* DECRQPSR */ break;
            }
            if (vt100_parse_state == State.Csi) {
                vt100_parse_reset();
            }
        }
    }
 
    private boolean vt100_write(int c) {
        if (c < 32) {
            if (c == 27) {
                vt100_parse_reset(State.Esc);
                return true;
            } else if (c == 14) {
                ctrl_SO();
            } else if (c == 15) {
                ctrl_SI();
            }
        } else if ((c & 0xffe0) == 0x0080) {
            vt100_parse_reset(State.Esc);
            vt100_parse_func = (char)(c - 0x0040);
            vt100_parse_process();
            return true;
        }
        if (vt100_parse_state != State.None) {
            if (vt100_parse_state == State.Str) {
                if (c >= 32) {
                    return true;
                }
                vt100_parse_reset();
            } else {
                if (c < 32) {
                    if (c == 24 || c == 26) {
                        vt100_parse_reset();
                        return true;
                    }
                } else {
                    vt100_parse_len += 1;
                    if (vt100_parse_len > 32) {
                        vt100_parse_reset();
                    } else {
                        int msb = c & 0xf0;
                        if (msb == 0x20) {
                            vt100_parse_func <<= 8;
                            vt100_parse_func += (char) c;
                        } else if (msb == 0x30 && vt100_parse_state == State.Csi) {
                            vt100_parse_param += new String(new char[] { (char) c } );
                        } else {
                            vt100_parse_func <<= 8;
                            vt100_parse_func += (char) c;
                            vt100_parse_process();
                        }
                        return true;
                    }
                }
            }
        }
        vt100_lastchar = c;
        return false;
    }
}