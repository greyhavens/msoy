//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays contents with a footer bar at the bottom containing buttons.
 */
public class ContentFooterPanel extends FlexTable
{
    public ContentFooterPanel (Widget content, Widget footer)
    {
        setStyleName("contentFooterPanel");
        setCellSpacing(0);
        setCellPadding(0);

        setWidget(0, 0, content);
        getFlexCellFormatter().setStyleName(0, 0, "Content");
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        setWidget(1, 0, footer);
        getFlexCellFormatter().setStyleName(1, 0, "Footer");
        getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_RIGHT);
    }
}
