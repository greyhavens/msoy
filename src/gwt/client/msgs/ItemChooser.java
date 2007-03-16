//
// $Id$

package client.msgs;

import java.util.List;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;

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
        if (images.size() > 0) {
            HorizontalPanel itemPanel = new HorizontalPanel();
            ScrollPanel chooser = new ScrollPanel(itemPanel);
            add(chooser);

            for (Iterator iter = images.iterator(); iter.hasNext(); ) {
                itemPanel.add(new ItemThumbnail((Item) iter.next(), this));
            }

        } else {
            add(MsoyUI.createLabel(CShell.cmsgs.haveNoImages(), "Title"));
        }
    }

    // from interface ClickListener
    public void onClick (Widget sender)
    {
        _callback.onSuccess(((ItemThumbnail)(sender.getParent())).getItem());
    }

    protected AsyncCallback _callback;
}
