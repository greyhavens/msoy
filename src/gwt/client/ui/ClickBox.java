//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;

import client.util.Link;

/**
 * Displays a thumbnail image and a name below it (both of which are clickable) with a grey box
 * around them both in our preferred grid of clickable things style.
 */
public class ClickBox extends SmartTable
{
    public ClickBox (MediaDesc media, String name, String page, String args)
    {
        super("clickBox", 0, 0);

        ClickListener onClick = Link.createListener(page, args);
        addWidget(new ThumbBox(media, onClick), getColumns(), null);
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        addLabel(MsoyUI.createActionLabel(name, "Name", onClick));
    }

    /**
     * Adds the supplied label to the click box, making it width-limited and non-wrapping.
     */
    protected void addLabel (Label label)
    {
        label.addStyleName("Label");
        addWidget(label, getColumns(), null);
    }

    /**
     * If a derived class wants to add a row with more than one column, it can override this method
     * and all of the standard elements of the clickbox will be added with this colspan.
     */
    protected int getColumns ()
    {
        return 1;
    }
}
