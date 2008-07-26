//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.MediaDesc;

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
                Link.go(page, args);
            }
        };

        ThumbBox thumb = new ThumbBox(media, onClick);
        Widget mainWidget = thumb;

        if (remixable) {
            // add a wee icon indicating remixable
            Image remix = new Image("/images/item/remixable_icon.png");
            remix.setTitle(CShell.imsgs.remixTip());

            // arrange it next to the thumbnail
            HorizontalPanel hpan = new HorizontalPanel();
            hpan.add(thumb);
            hpan.add(WidgetUtil.makeShim(2, 2));
            hpan.add(remix);
            mainWidget = hpan;
        }

        addWidget(mainWidget, getColumns(), null);
        getFlexCellFormatter().setHorizontalAlignment(getRowCount()-1, 0, HasAlignment.ALIGN_CENTER);
        addWidget(MsoyUI.createActionLabel(name, "Name", onClick), getColumns(), null);
    }

    protected int getColumns ()
    {
        return 1;
    }
}
