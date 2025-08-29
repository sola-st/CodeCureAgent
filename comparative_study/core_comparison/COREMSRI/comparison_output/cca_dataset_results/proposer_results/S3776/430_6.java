/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.extend.FSDOMMutator;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.pdfboxout.PdfBoxSlowOutputDevice.Metadata;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.CacheStore;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.ViewportBox;
import com.openhtmltopdf.render.displaylist.DisplayListCollector;
import com.openhtmltopdf.render.displaylist.DisplayListContainer;
import com.openhtmltopdf.render.displaylist.DisplayListPainter;
import com.openhtmltopdf.render.displaylist.DisplayListContainer.DisplayListPageContainer;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;
import com.openhtmltopdf.util.Configuration;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.ThreadCtx;
import com.openhtmltopdf.util.XRLog;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.schema.XMPSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class PdfBoxRenderer implements Closeable, PageSupplier {
    // See discussion of units at top of PdfBoxOutputDevice.
    private static final float DEFAULT_DOTS_PER_POINT = 20f * 4f / 3f;
    private static final int DEFAULT_DOTS_PER_PIXEL = 20;
    private static final int DEFAULT_PDF_POINTS_PER_INCH = 72;

    private final SharedContext _sharedContext;
    private final PdfBoxOutputDevice _outputDevice;
    private final List<FSDOMMutator> _domMutators;

    private Document _doc;
    private BlockBox _root;

    private final float _dotsPerPoint;

    private PDDocument _pdfDoc;
    
    private PDEncryption _pdfEncryption;

    private String _producer;

    // Usually 1.7
    private float _pdfVersion;

    private PdfAConformance _pdfAConformance;
    private boolean _pdfUaConformance;

    private byte[] _colorProfile;

    private boolean _testMode;

    private PDFCreationListener _listener;
    
    private OutputStream _os;
    private SVGDrawer _svgImpl;
    private SVGDrawer _mathmlImpl;
    
    private BidiSplitterFactory _splitterFactory;
    private byte _defaultTextDirection = BidiSplitter.LTR;
    private BidiReorderer _reorderer;
    private final boolean _useFastMode;

    private PageSupplier _pageSupplier;

    private final Closeable diagnosticConsumer;
    
    /**
     * This method is constantly changing as options are added to the builder.
     */
    PdfBoxRenderer(BaseDocument doc, UnicodeImplementation unicode,
            PageDimensions pageSize, PdfRendererBuilderState state, Closeable diagnosticConsumer) {

        this.diagnosticConsumer = diagnosticConsumer;

        _pdfDoc = state.pddocument != null ? state.pddocument : new PDDocument();
        _pdfDoc.setVersion(state._pdfVersion);

        _producer = state._producer;

        _pageSupplier = state._pageSupplier != null ? state._pageSupplier : this;

        _svgImpl = state._svgImpl;
        _mathmlImpl = state._mathmlImpl;

        _pdfAConformance = state._pdfAConformance;
        _pdfUaConformance = state._pdfUaConform;
        _colorProfile = state._colorProfile;

        _dotsPerPoint = DEFAULT_DOTS_PER_POINT;
        _testMode = state._testMode;
        _useFastMode = state._useFastRenderer;
        _outputDevice = state._useFastRenderer ? 
                new PdfBoxFastOutputDevice(DEFAULT_DOTS_PER_POINT, _testMode,
                        state._pdfUaConform || state._pdfAConformance.getConformanceValue().equals("A"),
                        state._pdfAConformance != PdfAConformance.NONE) : 
                new PdfBoxSlowOutputDevice(DEFAULT_DOTS_PER_POINT, _testMode);
        _outputDevice.setWriter(_pdfDoc);
        _outputDevice.setStartPageNo(_pdfDoc.getNumberOfPages());
        
        PdfBoxUserAgent userAgent = new PdfBoxUserAgent(_outputDevice);

        if (_svgImpl != null) {
            _svgImpl.withUserAgent(userAgent);
        }

        userAgent.setProtocolsStreamFactory(state._streamFactoryMap);
        
        if (state._resolver != null) {
            userAgent.setUriResolver(state._resolver);
        }
        
        _sharedContext = new SharedContext();
        _sharedContext.registerWithThread();
        
        _sharedContext._preferredTransformerFactoryImplementationClass = state._preferredTransformerFactoryImplementationClass;
        _sharedContext._preferredDocumentBuilderFactoryImplementationClass = state._preferredDocumentBuilderFactoryImplementationClass;
        
        _sharedContext.setUserAgentCallback(userAgent);
        _sharedContext.setCss(new StyleReference(userAgent));
        userAgent.setSharedContext(_sharedContext);
        _outputDevice.setSharedContext(_sharedContext);

        PdfBoxFontResolver fontResolver = new PdfBoxFontResolver(_sharedContext, _pdfDoc, state._caches.get(CacheStore.PDF_FONT_METRICS), state._pdfAConformance, state._pdfUaConform);
        _sharedContext.setFontResolver(fontResolver);

        PdfBoxReplacedElementFactory replacedElementFactory = new PdfBoxReplacedElementFactory(_outputDevice, state._svgImpl, state._objectDrawerFactory, state._mathmlImpl);
        _sharedContext.setReplacedElementFactory(replacedElementFactory);

        _sharedContext.setTextRenderer(new PdfBoxTextRenderer());
        _sharedContext.setDPI(DEFAULT_PDF_POINTS_PER_INCH * _dotsPerPoint);
        _sharedContext.setDotsPerPixel(DEFAULT_DOTS_PER_PIXEL);
        _sharedContext.setPrint(true);
        _sharedContext.setInteractive(false);
        
        this.getSharedContext().setDefaultPageSize(pageSize.w, pageSize.h, pageSize.isSizeInches);
        
        if (state._replacementText != null) {
            this.getSharedContext().setReplacementText(state._replacementText);
        }
        
        if (unicode.splitterFactory != null) {
            this._splitterFactory = unicode.splitterFactory;
        }
        
        if (unicode.reorderer != null) {
            this._reorderer = unicode.reorderer;
            this._outputDevice.setBidiReorderer(_reorderer);
        }
        
        if (unicode.lineBreaker != null) {
            _sharedContext.setLineBreaker(unicode.lineBreaker);
        }
        
        if (unicode.charBreaker != null) {
            _sharedContext.setCharacterBreaker(unicode.charBreaker);
        }
        
        if (unicode.toLowerTransformer != null) {
            _sharedContext.setUnicodeToLowerTransformer(unicode.toLowerTransformer);
        }
        
        if (unicode.toUpperTransformer != null) {
            _sharedContext.setUnicodeToUpperTransformer(unicode.toUpperTransformer);
        }
        
        if (unicode.toTitleTransformer != null) {
            _sharedContext.setUnicodeToTitleTransformer(unicode.toTitleTransformer);
        }
        
        this._defaultTextDirection = unicode.textDirection ? BidiSplitter.RTL : BidiSplitter.LTR;

        this._domMutators = state._domMutators;

        if (doc.html != null) {
            this.setDocumentFromStringP(doc.html, doc.baseUri);
        }
        else if (doc.document != null) {
            this.setDocumentP(doc.document, doc.baseUri);
        }
        else if (doc.uri != null) {
            this.setDocumentP(doc.uri);
        }
        else if (doc.file != null) {
            try {
                this.setDocumentP(doc.file);
            } catch (IOException e) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_PROBLEM_TRYING_TO_READ_INPUT_XHTML_FILE, e);
                throw new RuntimeException("File IO problem", e);
            }
        }
        
        this._os = state._os;
    }

    public Document getDocument() {
        return _doc;
    }
    
    /**
     * Returns the PDDocument or null if it has been closed.
     */
    public PDDocument getPdfDocument() {
        return _pdfDoc;
    }
    
    /**
     * Get the PDF-BOX font resolver. Can be used to add fonts in code.
     * @return
     */
    public PdfBoxFontResolver getFontResolver() {
        return (PdfBoxFontResolver) _sharedContext.getFontResolver();
    }

    private Document loadDocument(String uri) {
        return _sharedContext.getUserAgentCallback().getXMLResource(uri).getDocument();
    }

    private void setDocumentP(String uri) {
        setDocumentP(loadDocument(uri), uri);
    }

    private void setDocumentP(Document doc, String url) {
        setDocumentP(doc, url, new XhtmlNamespaceHandler());
    }
    
    private void setDocumentP(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        setDocumentP(loadDocument(file.toURI().toURL().toExternalForm()), (parent == null ? "" : parent.toURI().toURL().toExternalForm()));
    }
    
    private void setDocumentFromStringP(String content, String baseUrl) {
        InputSource is = new InputSource(new BufferedReader(new StringReader(content)));
        Document dom = XMLResource.load(is).getDocument();
```java
    private void writePDFFast(List<PageBox> pages, RenderingContext c, Rectangle2D firstPageSize, PDDocument doc) throws IOException {
        _outputDevice.setRoot(_root);
        _outputDevice.start(_doc);

        PDPage page = _pageSupplier.requestPage(doc, (float) firstPageSize.getWidth(), (float) firstPageSize.getHeight(), 0, -1);
        PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, !_testMode);

        _outputDevice.initializePage(cs, page, (float) firstPageSize.getHeight());
        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);

        int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);
        firePreWrite(pageCount); // opportunity to adjust meta data
        setDidValues(doc); // set PDF header fields from meta data

        if (_pdfUaConformance) {
            addPdfUaXMPSchema(doc);
        } else if (_pdfAConformance != PdfAConformance.NONE) {
            addPdfASchema(doc, _pdfAConformance.getPart(), _pdfAConformance.getConformanceValue());
        }

        int pdfPageIndex = 0;
        int pageCountMinusOne = pageCount - 1;

        for (int i = 0; i < pageCount; i++) {
            PageBox currentPage = pages.get(i);
            currentPage.setBasePagePdfPageIndex(pdfPageIndex);
            DisplayListPageContainer pageOperations = new DisplayListCollector(_root.getLayer().getPages()).collectRoot(c, _root.getLayer()).getPageInstructions(i);
            c.setPage(i, currentPage);
            c.setShadowPageNumber(-1);
            paintPageFast(c, currentPage, pageOperations, 0);
            _outputDevice.finishPage();
            pdfPageIndex++;

            if (!pageOperations.shadowPages().isEmpty()) {
                currentPage.setShadowPageCount(pageOperations.shadowPages().size());

                int shadowPageIndex = 0;
                int pageContentWidth = currentPage.getContentWidth(c);
                int translateX = pageContentWidth * (currentPage.getCutOffPageDirection() == IdentValue.LTR ? 1 : -1);

                for (DisplayListPageContainer shadowPage : pageOperations.shadowPages()) {
                    PDPage shadowPdPage =
                            _pageSupplier.requestPage(
                                    doc,
                                    (float) currentPage.getWidth(c) / _dotsPerPoint,
                                    (float) currentPage.getHeight(c) / _dotsPerPoint, i, shadowPageIndex);

                    PDPageContentStream shadowCs = new PDPageContentStream(doc, shadowPdPage, AppendMode.APPEND, !_testMode);

                    _outputDevice.initializePage(shadowCs, shadowPdPage, (float) currentPage.getHeight(c) / _dotsPerPoint);
                    c.setShadowPageNumber(shadowPageIndex);
                    paintPageFast(c, currentPage, shadowPage, -translateX);
                    _outputDevice.finishPage();
                    translateX += (pageContentWidth * (currentPage.getCutOffPageDirection() == IdentValue.LTR ? 1 : -1));

                    pdfPageIndex++;
                    shadowPageIndex++;
                }
            }

            if (i < pageCountMinusOne) {
                PageBox nextPage = pages.get(i + 1);

                Rectangle2D nextPageSize = new Rectangle2D.Float(0, 0,
                        nextPage.getWidth(c) / _dotsPerPoint,
                        nextPage.getHeight(c) / _dotsPerPoint);

                PDPage pageNext =
                        _pageSupplier.requestPage(doc, (float) nextPageSize.getWidth(), (float) nextPageSize.getHeight(), i + 1, -1);

                PDPageContentStream csNext = new PDPageContentStream(doc, pageNext, AppendMode.APPEND, !_testMode);
                _outputDevice.initializePage(csNext, pageNext, (float) nextPageSize.getHeight());
            }
        }

        _outputDevice.finish(c, _root);
    }

            uaProp.setTextPropertyValue("description",
                    "Indicates, which part of ISO 14289 standard is followed");
            uaSchema.addUnqualifiedSequenceValue("property", uaProp);
            xmp.getPDFExtensionSchema().addBagValue("schemas", uaSchema);
            xmp.getPDFExtensionSchema().setPrefix("pdfuaid");
            xmp.getPDFExtensionSchema().setTextPropertyValue("part", "1");
            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(xmp, baos, true);
            PDMetadata metadata = new PDMetadata(doc);
            metadata.importXMPMetadata(baos.toByteArray());
            doc.getDocumentCatalog().setMetadata(metadata);
        } catch (IOException|TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private void addPdfASchema(PDDocument document, int part, String conformance) {
        PDDocumentInformation information = document.getDocumentInformation();
        XMPMetadata metadata = XMPMetadata.createXMPMetadata();

        try {
            String title = information.getTitle();
            String author = information.getAuthor();
            String subject = information.getSubject();
            String keywords = information.getKeywords();
            String producer = information.getProducer();
            
            // NOTE: The XMP metadata MUST match up with the document information dictionary
            // to be a valid PDF/A document.
            
            PDFAIdentificationSchema pdfaid = metadata.createAndAddPFAIdentificationSchema();
            pdfaid.setConformance(conformance);
            pdfaid.setPart(part);

            AdobePDFSchema pdfSchema = metadata.createAndAddAdobePDFSchema();
            if (keywords != null) {
                pdfSchema.setKeywords(keywords);
            }
            if (producer != null) {
                pdfSchema.setProducer(producer);
            }
            
            XMPBasicSchema xmpBasicSchema = metadata.createAndAddXMPBasicSchema();
            xmpBasicSchema.setCreateDate(information.getCreationDate());
            
            DublinCoreSchema dc = metadata.createAndAddDublinCoreSchema();
            if (author != null) {
                dc.addCreator(author);
            }
            if (title != null) {
                dc.setTitle(title);
            }
            if (subject != null) {
                dc.setDescription(subject);
            }
            
            PDMetadata metadataStream = new PDMetadata(document);
            PDMarkInfo markInfo = new PDMarkInfo();
            markInfo.setMarked(true);

            // add to catalog
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            catalog.setMetadata(metadataStream);
            catalog.setMarkInfo(markInfo);
            
            String lang = _doc.getDocumentElement().getAttribute("lang");
            catalog.setLanguage(!lang.isEmpty() ? lang : "EN-US");
            catalog.setViewerPreferences(new PDViewerPreferences(new COSDictionary()));
            catalog.getViewerPreferences().setDisplayDocTitle(true);

            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(metadata, baos, true);
            metadataStream.importXMPMetadata( baos.toByteArray() );

            if (_colorProfile != null) {
                ByteArrayInputStream colorProfile = new ByteArrayInputStream(_colorProfile);
                PDOutputIntent oi = new PDOutputIntent(document, colorProfile);
                oi.setInfo("sRGB IEC61966-2.1");
                oi.setOutputCondition("sRGB IEC61966-2.1");
                oi.setOutputConditionIdentifier("sRGB IEC61966-2.1");
                oi.setRegistryName("http://www.color.org");
                catalog.addOutputIntent(oi);
            }
        } catch (BadFieldValueException | IOException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    // Sets the document information dictionary values from html metadata
    private void setDidValues(PDDocument doc) {
        PDDocumentInformation info = new PDDocumentInformation();

        info.setCreationDate(Calendar.getInstance());

        if (_producer == null) {
            info.setProducer("openhtmltopdf.com");
        } else {
            info.setProducer(_producer);
        }

        for (Metadata metadata : _outputDevice.getMetadata()) {
        	String name = metadata.getName();
			if (name.isEmpty())
				continue;
        	String content = metadata.getContent();
        	if( content == null )
        	    continue;
            if( name.equals("title"))
                info.setTitle(content);
            else if( name.equals("author"))
                info.setAuthor(content);
            else if(name.equals("subject"))
                info.setSubject(content);
            else if(name.equals("keywords"))
                info.setKeywords(content);
            else
                info.setCustomMetadataValue(name,content);
        }

        doc.setDocumentInformation(info);
    }
    
    private void paintPageFast(RenderingContext c, PageBox page, DisplayListPageContainer pageOperations, int additionalTranslateX) {
        page.paintBackground(c, 0, Layer.PAGED_MODE_PRINT);
        
        c.setInPageMargins(true);
        page.paintMarginAreas(c, 0, Layer.PAGED_MODE_PRINT);
        c.setInPageMargins(false);
        
        page.paintBorder(c, 0, Layer.PAGED_MODE_PRINT);

        Rectangle content = page.getPrintClippingBounds(c);
        _outputDevice.pushClip(content);

        int top = -page.getPaintingTop() + page.getMarginBorderPadding(c, CalculatedStyle.TOP);

        int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);

        int translateX = left + additionalTranslateX;
        
        _outputDevice.translate(translateX, top);
        DisplayListPainter painter = new DisplayListPainter();
        painter.paint(c, pageOperations);
        _outputDevice.translate(-translateX, -top);

        _outputDevice.popClip();
    }

    private void paintPage(RenderingContext c, PageBox page) {
        // TODO: provideMetadataToPage(_pdfDoc, page);

        page.paintBackground(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintMarginAreas(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintBorder(c, 0, Layer.PAGED_MODE_PRINT);

        Shape working = _outputDevice.getClip();

        Rectangle content = page.getPrintClippingBounds(c);
        _outputDevice.clip(content);

        int top = -page.getPaintingTop() + page.getMarginBorderPadding(c, CalculatedStyle.TOP);

        int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);

        _outputDevice.translate(left, top);
        _root.getLayer().paint(c);
        _outputDevice.translate(-left, -top);

        _outputDevice.setClip(working);
    }
/* TODO : Metadata
    private void provideMetadataToPage(PdfWriter writer, PageBox page) throws IOException {
        byte[] metadata = null;
        if (page.getMetadata() != null) {
            try {
                String metadataBody = stringfyMetadata(page.getMetadata());
                if (metadataBody != null) {
                    metadata = createXPacket(stringfyMetadata(page.getMetadata())).getBytes("UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                // Can't happen
                throw new RuntimeException(e);
            }
        }

        if (metadata != null) {
            writer.setPageXmpMetadata(metadata);
        }
    }
*/
    private String stringfyMetadata(Element element) {
        Element target = getFirstChildElement(element);
        if (target == null) {
            return null;
        }

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter output = new StringWriter();
            transformer.transform(new DOMSource(target), new StreamResult(output));

            return output.toString();
        } catch (TransformerException e) {
            // Things must be in pretty bad shape to get here so
            // rethrow as runtime exception
            throw new RuntimeException(e);
        }
    }

    private static Element getFirstChildElement(Element element) {
        Node n = element.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) n;
            }
            n = n.getNextSibling();
        }
        return null;
    }

    private String createXPacket(String metadata) {
        StringBuilder result = new StringBuilder(metadata.length() + 50);
        result.append("<?xpacket begin='\uFEFF' id='W5M0MpCehiHzreSzNTczkc9d'?>\n");
        result.append(metadata);
        result.append("\n<?xpacket end='r'?>");

        return result.toString();
    }

    public PdfBoxOutputDevice getOutputDevice() {
        return _outputDevice;
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    /**
     * @deprecated unused and untested.
     * @param writer
     * @throws IOException
     */
    @Deprecated
    public void exportText(Writer writer) throws IOException {
        RenderingContext c = newRenderingContext();
        c.setPageCount(_root.getLayer().getPages().size());
        _root.exportText(c, writer);
    }

    public BlockBox getRootBox() {
        return _root;
    }

    public float getDotsPerPoint() {
        return _dotsPerPoint;
    }

    public List<PagePosition> findPagePositionsByID(Pattern pattern) {
        return _outputDevice.findPagePositionsByID(newLayoutContext(), pattern);
    }

    private static final class NullUserInterface implements UserInterface {
        public boolean isHover(Element e) {
            return false;
        }

        public boolean isActive(Element e) {
            return false;
        }

        public boolean isFocus(Element e) {
            return false;
        }
    }

    public PDFCreationListener getListener() {
        return _listener;
    }

    public void setListener(PDFCreationListener listener) {
        _listener = listener;
    }
    
    /**
     * @deprecated Use close instead.
     */
    @Deprecated
    public void cleanup() {
        _outputDevice.close();
        _sharedContext.removeFromThread();
        try {
            diagnosticConsumer.close();
        } catch (IOException e) {
        }
        ThreadCtx.cleanup();

        // Close all still open font files
        ((PdfBoxFontResolver)getSharedContext().getFontResolver()).close();

        if (_svgImpl != null) {
            try {
                _svgImpl.close();
            } catch (IOException e) {
            }
        }
        
        if (_mathmlImpl != null) {
            try {
                _mathmlImpl.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Cleanup thread resources. MUST be called after finishing with the renderer.
     */
    @Override
    public void close() {
        this.cleanup();
    }
    
    @Override
    public PDPage requestPage(PDDocument doc, float pageWidth, float pageHeight, int pageNumber, int shadowPageNumber) {
    	PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
    	doc.addPage(page);
    	return page;
    }
}