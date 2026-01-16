class ExpressionReader {
    private HsqlArrayList exprList;
    private short[] parseList;
    private int start;
    private int count;
    private boolean isOption;

    public ExpressionReader(HsqlArrayList exprList, short[] parseList, int start, int count, boolean isOption) {
        this.exprList = exprList;
        this.parseList = parseList;
        this.start = start;
        this.count = count;
        this.isOption = isOption;
    }

    public void readExpression() {
        for (int i = start; i < start + count; i++) {
            int exprType = parseList[i];
            switch (exprType) {
                case Tokens.QUESTION:
                    handleQuestion();
                    continue;
                case Tokens.X_POS_INTEGER:
                    handlePosInteger();
                    continue;
                case Tokens.X_OPTION:
                    handleOption(i);
                    i += parseList[++i] - 1;
                    continue;
                case Tokens.X_REPEAT:
                    handleRepeat(i);
                    i += parseList[++i] - 1;
                    continue;
                case Tokens.X_KEYSET:
                    handleKeySet(i);
                    i += parseList[++i];
                    continue;
                case Tokens.OPENBRACKET:
                case Tokens.CLOSEBRACKET:
                case Tokens.COMMA:
                default:
                    handleDefault(exprType);
                    continue;
            }
        }
    }

    private void handleQuestion() {
        // Implementation for handling Question case
        Expression e = null;

        e = XreadAllTypesCommonValueExpression(false);

        exprList.add(e);
    }

    private void handlePosInteger() {
        // Implementation for handling PosInteger case
        Expression e     = null;
        Integer    value = readIntegerObject();

        if (value.intValue() < 0) {
            throw Error.error(ErrorCode.X_42592);
        }

        e = new ExpressionValue(value, Type.SQL_INTEGER);

        exprList.add(e);
    }

    private void handleOption(int i) {
        // Implementation for handling Option case
        int expressionCount  = exprList.size();
        int position         = getPosition();
        int elementCount     = parseList[i++];
        int initialExprIndex = exprList.size();

        try {
            readExpression(exprList, parseList, i, elementCount,
                    true);
        } catch (HsqlException ex) {
            ex.setLevel(compileContext.subqueryDepth);

            if (lastError == null
                    || lastError.getLevel() < ex.getLevel()) {
                lastError = ex;
            }

            rewind(position);
            exprList.setSize(expressionCount);

            for (int j = i; j < i + elementCount; j++) {
                if (parseList[j] == Tokens.QUESTION
                        || parseList[j] == Tokens.X_KEYSET
                        || parseList[j] == Tokens.X_POS_INTEGER) {
                    exprList.add(null);
                }
            }
        }

        if (initialExprIndex == exprList.size()) {
            exprList.add(null);
        }
    }

    private void handleRepeat(int i) {
        // Implementation for handling Repeat case
        int elementCount = parseList[i++];
        int parseIndex   = i;

        while (true) {
            int initialExprIndex = exprList.size();

            readExpression(exprList, parseList, parseIndex,
                    elementCount, true);

            if (exprList.size() == initialExprIndex) {
                break;
            }
        }
    }

    private void handleKeySet(int i) {
        // Implementation for handling KeySet case
        int        elementCount = parseList[++i];
        Expression e            = null;

        if (ArrayUtil.find(parseList, token.tokenType, i
                + 1, elementCount) == -1) {
            if (!isOption) {
                throw unexpectedToken();
            }
        } else {
            e = new ExpressionValue(
                    ValuePool.getInt(token.tokenType),
                    Type.SQL_INTEGER);

            read();
        }

        exprList.add(e);
    }

    private void handleDefault(int exprType) {
        // Implementation for handling Default case
        if (token.tokenType != exprType) {
            throw unexpectedToken();
        }

        read();
    }
}

    void readExpression(HsqlArrayList exprList, short[] parseList, int start, int count, boolean isOption) {
        ExpressionReader reader = new ExpressionReader(exprList, parseList, start, count, isOption);
        reader.readExpression();
    }