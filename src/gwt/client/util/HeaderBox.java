//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a nicely titled box. Used on the profile and elsewhere.
 */
public class HeaderBox extends FlexTable
{
    public HeaderBox ()
    {
        setStyleName("headerBox");
        setCellPadding(0);
        setCellSpacing(0);

        Grid header = new Grid(1, 3);
        header.setCellPadding(0);
        header.setCellSpacing(0);
        header.setStyleName("BoxHeader");
        header.getCellFormatter().setStyleName(0, 0, "Left");
        header.setWidget(0, 1, _title = new Label("Title"));
        header.getCellFormatter().setStyleName(0, 1, "Title");
        header.getCellFormatter().setStyleName(0, 2, "Right");
        setWidget(0, 0, header);
        
        getCellFormatter().setStyleName(1, 0, "Content");
    }

    public void setTitle (String title)
    {
        _title.setText(title);
    }

    public void setContent (Widget content)
    {
        setWidget(1, 0, content);
    }

    protected Label _title;
}
