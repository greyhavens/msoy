//
// $Id$

package client.msgs;

import java.util.List;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.msoy.item.data.all.Item;

import client.shell.CShell;
import client.util.ItemThumbnail;
import client.util.MsoyUI;

/**
 * Allows a member to select an item from their inventory.
 */
public class ItemChooser extends VerticalPanel
    implements ClickListener
{
    /**
     * Creates an item chooser popup with the supplied list of items and a callback to be
     * informed when one is selected.
     */
    public ItemChooser (List images, AsyncCallback callback)
    {
        _callback = callback;
        setStyleName("itemChooser");

        // iterate over all our items and fill the popup panel
        PagedGrid grid = new PagedGrid(1, 4) {
            protected Widget createWidget (Object item) {
                return new ItemThumbnail((Item)item, ItemChooser.this);
            }
            protected String getEmptyMessage () {
                return CMsgs.cmsgs.haveNoImages();
            }
        };
    }

    // from interface ClickListener
    public void onClick (Widget sender)
    {
        _callback.onSuccess(((ItemThumbnail)(sender.getParent())).getItem());
    }

    protected AsyncCallback _callback;
}
