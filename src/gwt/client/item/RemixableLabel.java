//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.InlineLabel;

import client.ui.MsoyUI;

/**
 * Displays a standard note indicating that an item is remixable.
 */
public class RemixableLabel extends FlowPanel
{
    public RemixableLabel ()
    {
        setStyleName("remixableLabel");
        add(MsoyUI.createImage("/images/item/remixable_icon.png", "inline"));
        add(new InlineLabel(_imsgs.remixTip(), true, true, false));
    }

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
}
