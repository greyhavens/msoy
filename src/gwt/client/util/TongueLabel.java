//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Image;
import com.threerings.gwt.ui.SmartTable;

/**
 * Displays a little tongue label that sticks out from the left of the page.
 */
public class TongueLabel extends SmartTable
{
    public TongueLabel (String text)
    {
        super("tongue", 0, 0);
        setText(0, 0, text, 1, "Base");
        Image line = new Image("/images/ui/grey_line.png");
        line.setWidth("100%");
        line.setHeight("1px");
        setWidget(0, 1, line, 1, "Line");
    }
}
