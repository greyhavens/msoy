//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Widget;

/**
 * Works around browser's bullshit inability to put fucking spacing between cells in CSS without
 * also putting it around the outer edge of the whole table. Yay!
 */
public class RowPanel extends FlexTable
{
    public RowPanel ()
    {
        setCellPadding(0);
        setCellSpacing(0);
    }

    @Override // from Panel
    public void add (Widget child)
    {
        add(child, HasVerticalAlignment.ALIGN_TOP);
    }

    /**
     * Adds the supplied child with the specified vertical alignment.
     */
    public void add (Widget child, HasVerticalAlignment.VerticalAlignmentConstant valign)
    {
        int col = getWidgetCount();
        setWidget(0, col, child);
        getFlexCellFormatter().setStyleName(0, col, "rowPanelCell");
        getFlexCellFormatter().setVerticalAlignment(0, col, valign);
    }

    /**
     * Returns the number of widgets added to this row panel.
     */
    public int getWidgetCount ()
    {
        return (getRowCount() > 0 ? getCellCount(0) : 0);
    }
}
