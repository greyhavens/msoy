//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a row of widget cells with a border around each cell.
 */
public class BorderedHorizontalPanel extends FlexTable
{
    public BorderedHorizontalPanel ()
    {
        setStyleName("borderedHP");
        setCellPadding(0);
        setCellSpacing(0);
    }

    public void add (Widget widget)
    {
        add(widget, null);
    }

    public void add (Widget widget, String styleName)
    {
        int widgets = (getRowCount() == 0) ? 0 : getCellCount(0)/3;
        int wrow = 2, wcol = 3 * widgets + 2;

        FlexCellFormatter fmt = getFlexCellFormatter();
        if (widgets == 0) {
            fmt.setStyleName(wrow-2, wcol-2, "northWestOut");
            fmt.setStyleName(wrow-1, wcol-2, "westOut");
            fmt.setStyleName(wrow, wcol-2, "westOut");
            fmt.setStyleName(wrow+1, wcol-2, "westOut");
            fmt.setStyleName(wrow+2, wcol-2, "southWestOut");
        }

        fmt.setStyleName(wrow-2, wcol-1, "northOut");
        fmt.setStyleName(wrow-1, wcol-1, "northWestIn");
        fmt.setStyleName(wrow, wcol-1, "westIn");
        fmt.setStyleName(wrow+1, wcol-1, "southWestIn");
        fmt.setStyleName(wrow+2, wcol-1, "southOut");

        fmt.setStyleName(wrow-2, wcol, "northOut");
        fmt.setStyleName(wrow-1, wcol, "northIn");
        setWidget(wrow, wcol, widget);
        if (styleName != null) {
            fmt.setStyleName(wrow, wcol, styleName);
        }
        fmt.setStyleName(wrow+1, wcol, "southIn");
        fmt.setStyleName(wrow+2, wcol, "southOut");

        fmt.setStyleName(wrow-2, wcol+1, "northOut");
        fmt.setStyleName(wrow-1, wcol+1, "northEastIn");
        fmt.setStyleName(wrow, wcol+1, "eastIn");
        fmt.setStyleName(wrow+1, wcol+1, "southEastIn");
        fmt.setStyleName(wrow+2, wcol+1, "southOut");

        fmt.setStyleName(wrow-2, wcol+2, "northEastOut");
        fmt.setStyleName(wrow-1, wcol+2, "eastOut");
        fmt.setStyleName(wrow, wcol+2, "eastOut");
        fmt.setStyleName(wrow+1, wcol+2, "eastOut");
        fmt.setStyleName(wrow+2, wcol+2, "southEastOut");
    }

    public void setHorizontalAlignment (
        int index, HasHorizontalAlignment.HorizontalAlignmentConstant alignHoriz)
    {
        int wrow = 2, wcol = 3 * index + 2;
        getFlexCellFormatter().setHorizontalAlignment(wrow, wcol, alignHoriz);
    }

    public void setVerticalAlignment (
        int index, HasVerticalAlignment.VerticalAlignmentConstant alignVert)
    {
        int wrow = 2, wcol = 3 * index + 2;
        getFlexCellFormatter().setVerticalAlignment(wrow, wcol, alignVert);
    }

    public void setStyleName (int index, String styleName)
    {
        int wrow = 2, wcol = 3 * index + 2;
        getFlexCellFormatter().setStyleName(wrow, wcol, styleName);
    }
}
