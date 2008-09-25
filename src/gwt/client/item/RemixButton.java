//
// $Id$

package client.item;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

import client.ui.MsoyUI;
import client.ui.RowPanel;

/**
 * Displays a standard control to remix an item.
 */
public class RemixButton extends RowPanel
{
    public RemixButton (String label, ClickListener onClick)
    {
        setCellSpacing(6);

        add(MsoyUI.createImageButton("remixButton", onClick));
        add(MsoyUI.createLabel(label, "remixLabel"), HasVerticalAlignment.ALIGN_MIDDLE);
    }
}
