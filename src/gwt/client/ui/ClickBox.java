//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;

/**
 * Displays a thumbnail image and a name below it (both of which are clickable) with a grey box
 * around them both in our preferred grid of clickable things style.
 */
public class ClickBox extends SmartTable
{
    /**
     * Create a {@link ClickBox} with the given media, style name, and link information. The page
     * argument may be null, in which case linking will be disabled for this box.
     */
    public ClickBox (MediaDesc media, String name, Pages page, Object... args)
    {
        super("clickBox", 0, 0);

        Widget box, label;

        if (page != null) {
            box = new ThumbBox(media, page, args);
            label = Link.createBlock(name, null, page, args);
        } else {
            box = new ThumbBox(media);
            label = MsoyUI.createLabel(name, null);
        }
        addWidget(box, getColumns());
        addLabel(label);
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
    }

    /**
     * Adds the supplied label to the click box, making it width-limited and non-wrapping.
     */
    protected void addLabel (Widget label)
    {
        label.addStyleName("Label");
        addWidget(label, getColumns());
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
