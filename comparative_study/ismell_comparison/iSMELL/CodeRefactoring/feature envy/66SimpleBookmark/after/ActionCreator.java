package com.itextpdf.text.pdf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import com.itextpdf.text.error_messages.MessageLocalization;
import com.itextpdf.text.xml.simpleparser.IanaEncodings;
import com.itextpdf.text.xml.simpleparser.SimpleXMLDocHandler;
import com.itextpdf.text.xml.simpleparser.SimpleXMLParser;

public class ActionCreator {
    private PdfWriter writer;

    public ActionCreator(PdfWriter writer) {
        this.writer = writer;
    }

    public void createAction(PdfDictionary outline, HashMap<String, Object> map, boolean namedAsNames) {
        String action = (String) map.get("Action");
        if ("GoTo".equals(action)) {
            createGoToAction(outline, map, namedAsNames);
        } else if ("GoToR".equals(action)) {
            createGoToRAction(outline, map);
        } else if ("URI".equals(action)) {
            createURIAction(outline, map);
        } else if ("Launch".equals(action)) {
            createLaunchAction(outline, map);
        }
    }

    private void createGoToAction(PdfDictionary outline, HashMap<String, Object> map, boolean namedAsNames) {
        String p;
        if ((p = (String)map.get("Named")) != null) {
            if (namedAsNames)
                outline.put(PdfName.DEST, new PdfName(p));
            else
                outline.put(PdfName.DEST, new PdfString(p, null));
        }
        else if ((p = (String)map.get("Page")) != null) {
            PdfArray ar = new PdfArray();
            StringTokenizer tk = new StringTokenizer(p);
            int n = Integer.parseInt(tk.nextToken());
            ar.add(writer.getPageReference(n));
            if (!tk.hasMoreTokens()) {
                ar.add(PdfName.XYZ);
                ar.add(new float[]{0, 10000, 0});
            }
            else {
                String fn = tk.nextToken();
                if (fn.startsWith("/"))
                    fn = fn.substring(1);
                ar.add(new PdfName(fn));
                for (int k = 0; k < 4 && tk.hasMoreTokens(); ++k) {
                    fn = tk.nextToken();
                    if (fn.equals("null"))
                        ar.add(PdfNull.PDFNULL);
                    else
                        ar.add(new PdfNumber(fn));
                }
            }
            outline.put(PdfName.DEST, ar);
        }
    }

    private void createGoToRAction(PdfDictionary outline, HashMap<String, Object> map) {
        String p;
        PdfDictionary dic = new PdfDictionary();
        if ((p = (String)map.get("Named")) != null)
            dic.put(PdfName.D, new PdfString(p, null));
        else if ((p = (String)map.get("NamedN")) != null)
            dic.put(PdfName.D, new PdfName(p));
        else if ((p = (String)map.get("Page")) != null){
            PdfArray ar = new PdfArray();
            StringTokenizer tk = new StringTokenizer(p);
            ar.add(new PdfNumber(tk.nextToken()));
            if (!tk.hasMoreTokens()) {
                ar.add(PdfName.XYZ);
                ar.add(new float[]{0, 10000, 0});
            }
            else {
                String fn = tk.nextToken();
                if (fn.startsWith("/"))
                    fn = fn.substring(1);
                ar.add(new PdfName(fn));
                for (int k = 0; k < 4 && tk.hasMoreTokens(); ++k) {
                    fn = tk.nextToken();
                    if (fn.equals("null"))
                        ar.add(PdfNull.PDFNULL);
                    else
                        ar.add(new PdfNumber(fn));
                }
            }
            dic.put(PdfName.D, ar);
        }
        String file = (String)map.get("File");
        if (dic.size() > 0 && file != null) {
            dic.put(PdfName.S,  PdfName.GOTOR);
            dic.put(PdfName.F, new PdfString(file));
            String nw = (String)map.get("NewWindow");
            if (nw != null) {
                if (nw.equals("true"))
                    dic.put(PdfName.NEWWINDOW, PdfBoolean.PDFTRUE);
                else if (nw.equals("false"))
                    dic.put(PdfName.NEWWINDOW, PdfBoolean.PDFFALSE);
            }
            outline.put(PdfName.A, dic);
        }
    }

    private void createURIAction(PdfDictionary outline, HashMap<String, Object> map) {
        String uri = (String)map.get("URI");
        if (uri != null) {
            PdfDictionary dic = new PdfDictionary();
            dic.put(PdfName.S, PdfName.URI);
            dic.put(PdfName.URI, new PdfString(uri));
            outline.put(PdfName.A, dic);
        }
    }

    private void createLaunchAction(PdfDictionary outline, HashMap<String, Object> map) {
        String file = (String)map.get("File");
        if (file != null) {
            PdfDictionary dic = new PdfDictionary();
            dic.put(PdfName.S, PdfName.LAUNCH);
            dic.put(PdfName.F, new PdfString(file));
            outline.put(PdfName.A, dic);
        }
    }
}