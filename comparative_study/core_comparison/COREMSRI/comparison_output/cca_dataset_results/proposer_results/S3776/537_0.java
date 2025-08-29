```java
    private static String getNodeTextContents(Node n) {
        if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
            Text txtNode = (Text) n;
            String content = txtNode.getWholeText(); //This concatenates any adjacent text/cdata/entity nodes
            if (content == null)
                return "";
            else
                return content;
        } else {
            if (n.hasChildNodes()) {
                NodeList children = n.getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    //Skip any non-text nodes, like comments or entities
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                        Text txtNode = (Text) child;
                        String content = txtNode.getWholeText(); //This concatenates any adjacent text/cdata/entity nodes
                        if (content == null)
                            return "";
                        else
                            return content;
                    }
                }

                return "";
            } else {
                return "";
            }
        }
    }
```
I extracted the inner logic of the else branch in getNodeTextContents into a separate private method to reduce cognitive complexity:

```java
    private static String getNodeTextFromChildren(Node n) {
        if (!n.hasChildNodes()) {
            return "";
        }
        NodeList children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                Text txtNode = (Text) child;
                String content = txtNode.getWholeText();
                if (content == null) {
                    return "";
                } else {
                    return content;
                }
            }
        }
        return "";
    }
```

Then the original method becomes:

```java
    private static String getNodeTextContents(Node n) {
        if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
            Text txtNode = (Text) n;
            String content = txtNode.getWholeText(); //This concatenates any adjacent text/cdata/entity nodes
            if (content == null)
                return "";
            else
                return content;
        } else {
            return getNodeTextFromChildren(n);
        }
    }
```

This reduces the cognitive complexity of getNodeTextContents by moving the nested if and loop into a separate method. The functionality remains unchanged.
