    private void calcPageClearance(LayoutContext c) {
        if (! (c.isPrint() && getStyle().isCollapseBorders())) {
            return;
        }

        PageBox page = c.getRootLayer().getFirstPage(c, this);
        if (page == null) {
            return;
        }

        TableRowBox row = getFirstRow();
        if (row == null) {
            return;
        }

        int spill = calculateSpillFromRow(row);

        int borderTop = getAbsY() + (int)getMargin(c).top() - spill;
        int delta = page.getTop() - borderTop;

        if (delta > 0) {
            setY(getY() + delta);
            setPageClearance(delta);
            calcCanvasLocation();
            c.translate(0, delta);
        }
    }

    private int calculateSpillFromRow(TableRowBox row) {
        int spill = 0;
        for (Iterator<TableCellBox> i = row.getChildIteratorOfType(TableCellBox.class); i.hasNext(); ) {
            TableCellBox cell = i.next();
            BorderPropertySet collapsed = cell.getCollapsedPaintingBorder();
            int tmp = (int)collapsed.top() / 2;
            if (tmp > spill) {
                spill = tmp;
            }
        }
        return spill;
    }