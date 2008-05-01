//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.Application;
import client.shell.CShell;

/**
 * Displays an item (or anything with a thumbnail image, a name and potentially extra info below)
 * with a grey box around it in our preferred grid of items style.
 */
public class ItemBox extends SmartTable
{
    public ItemBox (
        MediaDesc media, String name, final String page, final String args, boolean remixable)
    {
        super("itemBox", 0, 0);

        ClickListener onClick = new ClickListener() {
            public void onClick (Widget widget) {
                Application.go(page, args);
            }
        };

        AbsolutePanel panel = new AbsolutePanel();
        panel.setStyleName("itemThumbBox"); // sets width=100, height=60

        ThumbBox thumb = new ThumbBox(media, onClick);
        panel.add(thumb, 10, 0); // thumbnail (80px) centered in itemThumbBox (100px)

        if (remixable) {
            Image remix = new Image("/images/item/remixable_icon.png");
            remix.setTitle(CShell.imsgs.remixTip());
            remix.addClickListener(onClick); // make the remix icon also do the main action
            remix.addStyleName("actionLabel"); // act clickable..
            panel.add(remix, 70, 30); // the icon is 30x30, so only overlap as much as necessary
        }

        addWidget(panel, getColumns(), null);
        getFlexCellFormatter().setHorizontalAlignment(getRowCount()-1, 0, HasAlignment.ALIGN_CENTER);
        addWidget(MsoyUI.createActionLabel(name, "Name", onClick), getColumns(), null);
    }

    protected int getColumns ()
    {
        return 1;
    }
}
