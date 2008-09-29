//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

/**
 * A scale9Grid/NinePatch-like container that can be dynamically sized while keeping correct
 * aspect of border images. Can be used as a bordered frame, button, or any container that needs to
 * draw constant sized edges/corners while still having a stretchable content area.
 */
public class StretchPanel extends SmartTable
{
    public StretchPanel (String styleName)
    {
        super(styleName, 0, 0);

        setup(0, 0, "NW");
        setup(0, 1, "N");
        setup(0, 2, "NE");
        setup(1, 0, "W");
        setup(1, 1, "X");
        setup(1, 2, "E");
        setup(2, 0, "SW");
        setup(2, 1, "S");
        setup(2, 2, "SE");
    }

    public StretchPanel (String styleName, Widget content)
    {
        this(styleName);
        setContent(content);
    }

    /** Sets the content widget displayed in the center patch of the grid. */
    public void setContent (Widget content)
    {
        setWidget(1, 1, content);
    }

    public Widget getContent ()
    {
        return getWidget(1, 1);
    }

    /** Internally used to prepare a patch for display. */
    protected void setup (int row, int col, String styleName)
    {
        //setWidget(row, col, new Widget());
        prepareCell(row, col);
        getFlexCellFormatter().setStyleName(row, col, styleName);
    }
}
