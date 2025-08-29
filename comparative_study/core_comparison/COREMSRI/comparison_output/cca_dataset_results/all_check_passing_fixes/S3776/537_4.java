    private static String getNodeTextContents(Node n) {
        if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
            return getWholeText((Text) n);
        } else {
            if (n.hasChildNodes()) {
                NodeList children = n.getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    //Skip any non-text nodes, like comments or entities
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                        return getWholeText((Text) child);
                    }
                }

                return "";
            } else {
                return "";
            }
        }
    }

    private static String getWholeText(Text txtNode) {
        String content = txtNode.getWholeText(); //This concatenates any adjacent text/cdata/entity nodes
        if (content == null)
            return "";
        else
            return content;
    }
