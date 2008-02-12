//
// $Id$

package client.shop;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.SubItem;

import client.item.ItemActivator;
import client.util.FlashClients;
import client.util.ItemUtil;
import client.util.MediaUtil;

/**
 * Displays feedback after an item has been purchased, allowing it to be worn, used, etc.
 */
public class BoughtItemPanel extends FlexTable
{
    public BoughtItemPanel (Item item)
    {
        setStyleName("boughtItem");
        setCellSpacing(0);
        setCellPadding(5);

        int row = 0;
        setText(row, 0, CShop.msgs.bipTitle());
        getFlexCellFormatter().setColSpan(row++, 0, 2);

        int prow = row;
        setWidget(row, 0, MediaUtil.createMediaView(item.getThumbnailMedia(),
                                                    MediaDesc.THUMBNAIL_SIZE, null));
        getFlexCellFormatter().setStyleName(row, 0, "Preview");

        setText(row, 1, ItemUtil.getName(item, true));
        getFlexCellFormatter().setStyleName(row++, 1, "ThumbText");

        if (FlashClients.clientExists() && !(item instanceof SubItem)) {
            setWidget(row++, 0, new ItemActivator(item));
            setText(row++, 0, getUsageMessage(item.getType()));
            getFlexCellFormatter().setRowSpan(prow, 0, 3);
        }

        setWidget(row++, 1, new Button("Back to catalog", new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        }));
    }

    protected String getUsageMessage (byte itemType)
    {
        if (itemType == Item.AVATAR) {
            return "Click the icon above to wear your new avatar now!";
        } else if (itemType == Item.DECOR) {
            return "Click the icon above to use your new decor now!";
        } else if (itemType == Item.AUDIO) {
            return "Click the icon above to use your new song as the soundtrack for this room!";
        } else if (itemType == Item.PET) {
            return "Click the icon above to add your new pet to this room!";
        } else {
            return "Click the icon above to add your new item to this room!";
        }
    }
}
