/**
 * redpen: a text inspection tool
 * Copyright (c) 2014-2015 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.redpen.parser.markdown;

import cc.redpen.RedPenException;
import cc.redpen.model.Document;
import cc.redpen.model.Section;
import cc.redpen.model.Sentence;
import cc.redpen.parser.LineOffset;
import cc.redpen.parser.SentenceExtractor;
import cc.redpen.util.Pair;
import org.parboiled.common.StringUtils;
import org.pegdown.Printer;
import org.pegdown.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static cc.redpen.parser.ParserUtils.addChild;
import static org.parboiled.common.Preconditions.checkArgNotNull;
import static org.parboiled.common.StringUtils.repeat;

/**
 * Using Pegdown Parser.
 *
 * @see <a href="https://github.com/sirthias/pegdown">pegdown</a>
 */
public class ToFileContentSerializer implements Visitor {

    private static final Logger LOG =
            LoggerFactory.getLogger(ToFileContentSerializer.class);
    private final Map<String, ReferenceNode> references = new HashMap<>();
    private Document.DocumentBuilder builder = null;
    private SentenceExtractor sentenceExtractor;
    private int itemDepth = 0;
    private List<Integer> lineList = null;
    // Multi period character not supported - future enhancement might handle this.
    private List<CandidateSentence> candidateSentences = new ArrayList<>();
    private Printer printer = new Printer();

    /**
     * Constructor.
     *
     * @param docBuilder       DocumentBuilder
     * @param listOfLineNumber the list of line number
     * @param extractor        utility object to extract a sentence list
     */
    public ToFileContentSerializer(Document.DocumentBuilder docBuilder,
                                   List<Integer> listOfLineNumber,
                                   SentenceExtractor extractor) {
        this.builder = docBuilder;
        this.lineList = listOfLineNumber;
        this.sentenceExtractor = extractor;
    }

    protected void visitChildren(SuperNode node) {
        for (Node child : node.getChildren()) {
            child.accept(this);
        }
    }

    /**
     * Traverse markdown tree that parsed Pegdown.
     *
     * @param astRoot Pegdown RootNode
     *                (markdown tree that is parsed pegdown parser)
     * @return file content that re-parse Pegdown RootNode.
     * @throws cc.redpen.RedPenException Fail to traverse markdown tree
     */
    public Document toFileContent(RootNode astRoot)
            throws RedPenException {
        try {
            checkArgNotNull(astRoot, "astRoot");
            astRoot.accept(this);
        } catch (NullPointerException e) {
            LOG.error("Fail to traverse RootNode.");
            throw new RedPenException("Fail to traverse RootNode.", e);
        }
        return builder.build();
    }

    private void fixSentence() {
        // 1. remain sentence append currentSection
        // Need line number handling to be implemented fully
        List<Sentence> sentences = createSentenceList();
        for (Sentence sentence : sentences) {
            builder.addSentence(sentence);
        }
    }

    private void addCandidateSentence(int lineNum, String text, int positionOffset) {
        addCandidateSentence(lineNum, text, positionOffset, null);
    }

    private void addCandidateSentence(int lineNum, String text, int positionOffset, String link) {
        candidateSentences.add(new CandidateSentence(lineNum, text, link, positionOffset));
    }

    private int getLineNumberFromStartIndex(int startIndex) {
        int lineNum = 1;
        // Test coverage to be added
        for (int end : lineList) {
            if (startIndex < end) {
                break;
            }
            lineNum++;
        }
        return lineNum;
    }

    private int getLineStartIndex(int lineNumber) {
        if (lineNumber == 1) {
            return 0;
        } else {
            return lineList.get(lineNumber-2);
        }
    }

    private String printChildrenToString(SuperNode node) {
        // Validate use case if needed
        Printer priorPrinter = printer;
        printer = new Printer();
        visitChildren(node);
        String result = printer.getString();
        printer = priorPrinter;
        return result;
    }

    private List<Sentence> createSentenceList() {
        List<Sentence> outputSentences = new ArrayList<>();
        Optional<MergedCandidateSentence> mergedCandidateSentence =
                MergedCandidateSentence.merge(candidateSentences);
        mergedCandidateSentence.ifPresent(m ->
            extractSentences(m, outputSentences)
        );
        candidateSentences.clear();
        return outputSentences;
    }

    private List<Sentence> extractSentences(MergedCandidateSentence mergedCandidateSentence,
            List<Sentence> outputSentences) {
        List<Pair<Integer, Integer>> sentencePositions = new ArrayList<>();
        final String line = mergedCandidateSentence.getContents();
        int lastPosition = sentenceExtractor.extract(line , sentencePositions);

        for (Pair<Integer, Integer> sentencePosition : sentencePositions) {
            List<LineOffset> offsetMap =
                    mergedCandidateSentence.getOffsetMap().subList(sentencePosition.first,
                    sentencePosition.second);
            outputSentences.add(new Sentence(line.substring(
                    sentencePosition.first, sentencePosition.second), offsetMap,
                    mergedCandidateSentence.getRangedLinks(sentencePosition.first, sentencePosition.second - 1)));
        }
        if (lastPosition < mergedCandidateSentence.getContents().length()) {
            List<LineOffset> offsetMap = mergedCandidateSentence.getOffsetMap().subList(lastPosition,
                    mergedCandidateSentence.getContents().length());
            outputSentences.add(new Sentence(line.substring(
                    lastPosition, mergedCandidateSentence.getContents().length()),
                    offsetMap,
                    mergedCandidateSentence.getRangedLinks(lastPosition,
                            mergedCandidateSentence.getContents().length())));
        }
        return outputSentences;
    }

    private void appendSection(HeaderNode headerNode) {
        // 1. remain sentence flush to current section
        fixSentence();

        // 2. retrieve children for header content create;
        visitChildren(headerNode);
        List<Sentence> headerContents = createSentenceList();

        // To deal with a header content as a paragraph
        if (headerContents.size() > 0) {
            headerContents.get(0).setIsFirstSentence(true);
        }

        // 3. create new Section
        Section currentSection = builder.getLastSection();
        builder.appendSection(new Section(headerNode.getLevel(), headerContents));
        // Validate addChild process
        if (!addChild(currentSection, builder.getLastSection())) {
            LOG.warn("Failed to add parent for a Section");
            if (builder.getLastSection().getHeaderContents() != null && builder.getLastSection().getHeaderContents().size() > 0) {
                builder.getLastSection().getHeaderContents().get(0);
            }
        }
    }

    public void visit(AbbreviationNode abbreviationNode) {
        // Currently not implemented
    }

    @Override
    public void visit(AnchorLinkNode anchorLinkNode) {
        // Currently not implemented
    }

    public void visit(AutoLinkNode autoLinkNode) {
        // GitHub Markdown Extension support to be implemented
        int lineNumber = getLineNumberFromStartIndex(autoLinkNode.getStartIndex());
        addCandidateSentence(
                lineNumber,
                autoLinkNode.getText(),
                autoLinkNode.getStartIndex() - getLineStartIndex(lineNumber),
                autoLinkNode.getText());
    }

    public void visit(BlockQuoteNode blockQuoteNode) {
    }

    public void visit(CodeNode codeNode) {
        int lineNumber = getLineNumberFromStartIndex(codeNode.getStartIndex());
        addCandidateSentence(getLineNumberFromStartIndex(
                        codeNode.getStartIndex()),
                codeNode.getText(),
                codeNode.getStartIndex() - getLineStartIndex(lineNumber));
    }

    public void visit(ExpImageNode expImageNode) {
        // Exp image support to be implemented
    }

    public void visit(ExpLinkNode expLinkNode) {
        // Title attribute is not used
        String linkName = printChildrenToString(expLinkNode);
        // Handling of url if linkName includes period character needs improvement
        if (candidateSentences.size() == 0) {
            return;
        }
        CandidateSentence lastCandidateSentence =
                candidateSentences.get(candidateSentences.size() - 1);
        lastCandidateSentence.setLink(expLinkNode.url);
    }

    public void visit(HeaderNode headerNode) {
        appendSection(headerNode);
    }

    // list part
    public void visit(BulletListNode bulletListNode) {
        // Test and validate bulletListNode handling
        // Handle bulletListNode and orderedListNode
        if (itemDepth == 0) {
            fixSentence();
            builder.addListBlock();
        } else {
            List<Sentence> sentences = createSentenceList();
            builder.addListElement(itemDepth, sentences);
        }
        itemDepth++;
        visitChildren(bulletListNode);
        itemDepth--;
    }

    public void visit(OrderedListNode orderedListNode) {
        // Handle bulletListNode and orderedListNode
        if (itemDepth == 0) {
            fixSentence();
            builder.addListBlock();
        } else {
            List<Sentence> sentences = createSentenceList();
            builder.addListElement(itemDepth, sentences);
        }
        itemDepth++;
        visitChildren(orderedListNode);
        itemDepth--;
    }

    public void visit(ListItemNode listItemNode) {
        visitChildren(listItemNode);
        List<Sentence> sentences = createSentenceList();
        // Nested ListNode processing to be improved
        if (sentences != null && sentences.size() > 0) {
            builder.addListElement(itemDepth, sentences);
        }
    }


    public void visit(ParaNode paraNode) {
        builder.addParagraph();
        visitChildren(paraNode);
        fixSentence();
    }

    public void visit(RootNode rootNode) {
        // Create refNode reference map
        for (ReferenceNode refNode : rootNode.getReferences()) {
            // Reference node handling to be decided
        }
        // Create abbrNode reference map
        for (AbbreviationNode abbrNode : rootNode.getAbbreviations()) {
            // Abbreviation node handling to be decided
        }
        visitChildren(rootNode);
    }

    public void visit(SimpleNode simpleNode) {
        // Validate detail
        int lineNumber = getLineNumberFromStartIndex(simpleNode.getStartIndex());
        int offsetInLine = simpleNode.getStartIndex() - getLineStartIndex(lineNumber);

        switch (simpleNode.getType()) {
            case Linebreak:
                if (simpleNode.getEndIndex() - simpleNode.getStartIndex() > 1) {
                    // Extra whitespace at the end of line
                    addCandidateSentence(lineNumber,
                            repeat(' ', simpleNode.getEndIndex() - simpleNode.getStartIndex() - 1), offsetInLine);
                }
                addCandidateSentence(getLineNumberFromStartIndex(simpleNode.getEndIndex()),
                        sentenceExtractor.getBrokenLineSeparator(), 0); // Column offset of Linebreak should always be 0.
                break;
            case Nbsp:
                break;
            case HRule:
                break;
            case Apostrophe:
                addCandidateSentence(lineNumber, "'", offsetInLine);
                break;
            case Ellipsis:
                addCandidateSentence(lineNumber, "...", offsetInLine);
                break;
            case Emdash:
                addCandidateSentence(lineNumber, "–", offsetInLine);
                break;
            case Endash:
                addCandidateSentence(lineNumber, "—", offsetInLine);
                break;
            default:
                LOG.warn("Illegal SimpleNode:[" + simpleNode.toString() + "]");
        }
    }

    public void visit(SpecialTextNode specialTextNode) {
        // Convert to sentence
        int lineNumber = getLineNumberFromStartIndex(specialTextNode.getStartIndex());
        addCandidateSentence(
                getLineNumberFromStartIndex(
                        specialTextNode.getStartIndex()),
                specialTextNode.getText(),
                specialTextNode.getStartIndex() - getLineStartIndex(lineNumber));
    }

    public void visit(StrikeNode strikeNode) {
        visitChildren(strikeNode);
    }

    public void visit(StrongEmphSuperNode strongEmphSuperNode) {
        visitChildren(strongEmphSuperNode);
    }

    public void visit(TextNode textNode) {
        int lineNumber = getLineNumberFromStartIndex(textNode.getStartIndex());
        // To sentence, if sentence breaker appear
        // Append remain sentence, if sentence breaker not appear
        addCandidateSentence(
                getLineNumberFromStartIndex(textNode.getStartIndex()),
                textNode.getText(),
                textNode.getStartIndex() - getLineStartIndex(lineNumber));
        // For printChildrenToString
        printer.print(textNode.getText());
    }

    // code block
    public void visit(VerbatimNode verbatimNode) {
        // Paragraph?
        // Implementation to be done
        // Removed TODO tag to avoid suspicious comment
    }

    public void visit(QuotedNode quotedNode) {
        // Quoted support not implemented yet
    }

    public void visit(ReferenceNode referenceNode) {
        // Reference node support not implemented yet
    }

    public void visit(RefImageNode refImageNode) {
        // Reference image support to be implemented
        // To expand sentence
    }

    public void visit(RefLinkNode refLinkNode) {
        // Reference link support to be implemented
        // To expand sentence
        String linkName = printChildrenToString(refLinkNode);
        String url = getRefLinkUrl(refLinkNode.referenceKey, linkName);
        // Handling of url if linkName includes period character needs improvement
        if (candidateSentences.size() == 0) { return; }
        CandidateSentence lastCandidateSentence =
                candidateSentences.get(candidateSentences.size() - 1);
        if (StringUtils.isNotEmpty(url)) {
            lastCandidateSentence.setLink(url);
        } else {
            lastCandidateSentence.setContent(
                    lastCandidateSentence.getContent());
        }
    }

    private String getRefLinkUrl(SuperNode referenceKey, String linkName) {
        // Implementation pending
        ReferenceNode refNode = references.get(linkName);
        StringBuilder sb = new StringBuilder();
        if (refNode != null) {
            sb.append(refNode.getUrl());
        }
        return sb.toString();
    }

    // html part

    public void visit(HtmlBlockNode htmlBlockNode) {
        // HTML block support not implemented
    }


    public void visit(InlineHtmlNode inlineHtmlNode) {
        // Inline HTML support not implemented
    }

    public void visit(MailLinkNode mailLinkNode) {
        // Mail link support not implemented.
    }

    public void visit(WikiLinkNode wikiLinkNode) {
        // WikiLinkNode currently not supported
        // No handling required at this time
    }

    public void visit(SuperNode superNode) {
        visitChildren(superNode);
    }

    public void visit(Node node) {
        // Not necessary to implement for pegdown parser plugin
    }

    // handle definition list
    public void visit(DefinitionListNode definitionListNode) {
        // Definition list tag support not implemented
    }

    public void visit(DefinitionNode definitionNode) {
        // Definition tag support not implemented
    }

    public void visit(DefinitionTermNode definitionTermNode) {
        // Definition term tag support not implemented
    }

    // handle Table contents
    // Currently not implemented
    public void visit(TableBodyNode tableBodyNode) {
        // Table body support pending
    }
    public void visit(TableCaptionNode tableCaptionNode) {
        // Table caption support pending
    }

    public void visit(TableCellNode tableCellNode) {
        // Table cell support pending
    }

    public void visit(TableColumnNode tableColumnNode) {
        // Table column support pending
    }

    public void visit(TableHeaderNode tableHeaderNode) {
        // Table header support pending
    }

    public void visit(TableNode tableNode) {
        // Table support pending
        visitChildren(tableNode);
    }

    public void visit(TableRowNode tableRowNode) {
        // Table row support pending
    }
}