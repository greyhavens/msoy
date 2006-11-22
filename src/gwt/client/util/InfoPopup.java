//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a popup informational message.
 */
public class InfoPopup extends PopupPanel
{
    public InfoPopup (String message)
    {
        super(true);
        setStyleName("infoPopup");
        setWidget(new Label(message));
    }

    public void showNear (Widget parent)
    {
        setPopupPosition(parent.getAbsoluteLeft(),
                         parent.getAbsoluteTop() + parent.getOffsetHeight());
        show();
    }
}
