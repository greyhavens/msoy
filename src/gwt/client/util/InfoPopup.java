//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a popup informational message.
 */
public class InfoPopup extends BorderedPopup
{
    public InfoPopup (String message)
    {
        super(true);
        setWidget(MsoyUI.createLabel(message, "infoPopup"));
    }

    public InfoPopup (Widget contents)
    {
        super(true);
        contents.addStyleName("infoPopup");
        setWidget(contents);
    }

    public void showNear (Widget parent)
    {
        _centerOnShow = false;
        setPopupPosition(parent.getAbsoluteLeft(),
                         parent.getAbsoluteTop() + parent.getOffsetHeight());
        show();
    }
}
