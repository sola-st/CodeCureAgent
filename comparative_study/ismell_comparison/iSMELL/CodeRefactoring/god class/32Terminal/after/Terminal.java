/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Based on http://antony.lesuisse.org/software/ajaxterm/
 *  Public Domain License
 */

/**
 * See http://www.ecma-international.org/publications/standards/Ecma-048.htm
 *       and http://vt100.net/docs/vt510-rm/
 */

 package org.apache.karaf.webconsole.gogo;

 import java.util.Arrays;
 import java.util.Map;
 import java.util.HashMap;
 import java.util.List;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 public class Terminal {
 
     enum State {
         None,
         Esc,
         Str,
         Csi,
     }
 
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

    private ScreenManager screenManager;
    private CursorManager cursorManager;
    private UTF8Decoder utf8Decoder;
    private VT100Parser vt100Parser;
    private VT100CommandHandler vt100CommandHandler;
    private InputFilter inputFilter;
    private TerminalConfiguration configuration;
 
     public Terminal() {
         this(80, 24);
         this.screenManager = new ScreenManager(width, height);
        this.cursorManager = new CursorManager();
        this.utf8Decoder = new UTF8Decoder();
        this.vt100Parser = new VT100Parser();
        this.vt100CommandHandler = new VT100CommandHandler();
        this.inputFilter = new InputFilter();
        this.configuration = new TerminalConfiguration();
     }
 
     public Terminal(int width, int height) {
         this.width = width;
         this.height = height;
         reset_hard();
         this.screenManager = new ScreenManager(width, height);
        this.cursorManager = new CursorManager();
        this.utf8Decoder = new UTF8Decoder();
        this.vt100Parser = new VT100Parser();
        this.vt100CommandHandler = new VT100CommandHandler();
        this.inputFilter = new InputFilter();
        this.configuration = new TerminalConfiguration();
     }
     
     //
     // Dirty
     //
 
     private synchronized void setDirty() {
         dirty.set(true);
         notifyAll();
     }
 
     //
     // External interface
     //
 
     public synchronized boolean setSize(int w, int h) {
         if (w < 2 || w > 256 || h < 2 || h > 256) {
             return false;
         }
         this.width = w;
         this.height = h;
         reset_screen();
         return true;
     }
 
     public synchronized String read() {
         String d = vt100_out;
         vt100_out = "";
         return d;
     }
 
     public synchronized String pipe(String d) {
         String o = "";
         for (char c : d.toCharArray()) {
             if (vt100_keyfilter_escape) {
                 vt100_keyfilter_escape = false;
                 if (vt100_mode_cursorkey) {
                     switch (c) {
                         case '~': o += "~"; break;
                         case 'A': o += "\u001bOA"; break;
                         case 'B': o += "\u001bOB"; break;
                         case 'C': o += "\u001bOC"; break;
                         case 'D': o += "\u001bOD"; break;
                         case 'F': o += "\u001bOF"; break;
                         case 'H': o += "\u001bOH"; break;
                         case '1': o += "\u001b[5~"; break;
                         case '2': o += "\u001b[6~"; break;
                         case '3': o += "\u001b[2~"; break;
                         case '4': o += "\u001b[3~"; break;
                         case 'a': o += "\u001bOP"; break;
                         case 'b': o += "\u001bOQ"; break;
                         case 'c': o += "\u001bOR"; break;
                         case 'd': o += "\u001bOS"; break;
                         case 'e': o += "\u001b[15~"; break;
                         case 'f': o += "\u001b[17~"; break;
                         case 'g': o += "\u001b[18~"; break;
                         case 'h': o += "\u001b[19~"; break;
                         case 'i': o += "\u001b[20~"; break;
                         case 'j': o += "\u001b[21~"; break;
                         case 'k': o += "\u001b[23~"; break;
                         case 'l': o += "\u001b[24~"; break;
                     }
                 } else {
                     switch (c) {
                         case '~': o += "~"; break;
                         case 'A': o += "\u001b[A"; break;
                         case 'B': o += "\u001b[B"; break;
                         case 'C': o += "\u001b[C"; break;
                         case 'D': o += "\u001b[D"; break;
                         case 'F': o += "\u001b[F"; break;
                         case 'H': o += "\u001b[H"; break;
                         case '1': o += "\u001b[5~"; break;
                         case '2': o += "\u001b[6~"; break;
                         case '3': o += "\u001b[2~"; break;
                         case '4': o += "\u001b[3~"; break;
                         case 'a': o += "\u001bOP"; break;
                         case 'b': o += "\u001bOQ"; break;
                         case 'c': o += "\u001bOR"; break;
                         case 'd': o += "\u001bOS"; break;
                         case 'e': o += "\u001b[15~"; break;
                         case 'f': o += "\u001b[17~"; break;
                         case 'g': o += "\u001b[18~"; break;
                         case 'h': o += "\u001b[19~"; break;
                         case 'i': o += "\u001b[20~"; break;
                         case 'j': o += "\u001b[21~"; break;
                         case 'k': o += "\u001b[23~"; break;
                         case 'l': o += "\u001b[24~"; break;
                     }
                 }
             } else if (c == '~') {
                 vt100_keyfilter_escape = true;
             } else if (c == 127) {
                 if (vt100_mode_backspace) {
                     o += (char) 8;
                 } else {
                     o += (char) 127;
                 }
             } else {
                 o += c;
                 if (vt100_mode_lfnewline && c == 13) {
                     o += (char) 10;
                 }
             }
         }
         return o;
     }
 
     public synchronized boolean write(String d) {
         d = utf8_decode(d);
         for (int c : d.toCharArray()) {
             if (vt100_write(c)) {
                 continue;
             }
             if (dumb_write(c)) {
                 continue;
             }
             if (c <= 0xffff) {
                 dumb_echo(c);
             }
         }
         return true;
     }
 
     public synchronized String dump(long timeout, boolean forceDump) throws InterruptedException {
         if (!dirty.get() && timeout > 0) {
             wait(timeout);
         }
         if (dirty.compareAndSet(true, false) || forceDump) {
             StringBuilder sb = new StringBuilder();
             int prev_attr = -1;
             int cx = Math.min(this.cx, width - 1);
             int cy = this.cy;
             sb.append("<div><pre class='term'>");
             for (int y = 0; y < height; y++) {
                 int wx = 0;
                 for (int x = 0; x < width; x++) {
                     int d = screen[y * width + x];
                     int c = d & 0xffff;
                     int a = d >> 16;
                     if (cy == y && cx == x && vt100_mode_cursor) {
                         a = a & 0xfff0 | 0x000c;
                     }
                     if (a != prev_attr) {
                         if (prev_attr != -1) {
                             sb.append("</span>");
                         }
                         int bg = a & 0x000f;
                         int fg = (a & 0x00f0) >> 4;
                         boolean inv = (a & 0x0200) != 0;
                         boolean inv2 = vt100_mode_inverse;
                         if (inv && !inv2 || inv2 && !inv) {
                             int i = fg; fg = bg; bg = i;
                         }
                         if ((a & 0x0400) != 0) {
                             fg = 0x0c;
                         }
                         String ul;
                         if ((a & 0x0100) != 0) {
                             ul = " ul";
                         } else {
                             ul = "";
                         }
                         String b;
                         if ((a & 0x0800) != 0) {
                             b = " b";
                         } else {
                             b = "";
                         }
                         sb.append("<span class='f").append(fg).append(" b").append(bg).append(ul).append(b).append("'>");
                         prev_attr = a;
                     }
                     switch (c) {
                         case '&': sb.append("&amp;"); break;
                         case '<': sb.append("&lt;"); break;
                         case '>': sb.append("&gt;"); break;
                         default:
                             wx += utf8_charwidth(c);
                             if (wx <= width) {
                                 sb.append((char) c);
                             }
                             break;
                     }
                 }
                 sb.append("\n");
             }
             sb.append("</span></pre></div>");
             return sb.toString();
         }
         return null;
     }
 
     public String toString() {
         StringBuilder sb = new StringBuilder();
         for (int y = 0; y < height; y++) {
             for (int x = 0; x < width; x++) {
                 sb.append((char) (screen[y * width + x] & 0xffff));
             }
             sb.append("\n");
         }
         return sb.toString();
     }
 }