//
// $Id$

package client.item;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

import client.ui.MsoyUI;
import client.ui.RowPanel;

/**
 * Displays a standard control to remix or config an item.
 */
public class ConfigButton extends RowPanel
{
    public ConfigButton (boolean isConfig, String label, ClickHandler onClick)
    {
        setCellSpacing(6);

        add(MsoyUI.createImageButton(isConfig ? "configButton" : "remixButton", onClick));
        add(MsoyUI.createActionLabel(label, "configLabel", onClick),
            HasVerticalAlignment.ALIGN_MIDDLE);
    }
}
