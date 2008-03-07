//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

import client.item.ItemActivator;
import client.shell.Application;
import client.shell.Page;
import client.util.FlashClients;
import client.util.ThumbBox;

/**
 * Displays feedback after an item has been purchased, allowing it to be worn, used, etc.
 */
public class BoughtItemPanel extends SmartTable
{
    public BoughtItemPanel (Item item)
    {
        super("boughtItem", 0, 5);

        setWidget(0, 0, new ThumbBox(item.getThumbnailMedia(), null), 1, "Preview");
        setText(0, 1, item.name, 1, "ThumbText");
        getFlexCellFormatter().setRowSpan(0, 0, 3);

        if (FlashClients.clientExists() && !(item instanceof SubItem)) {
            setWidget(1, 0, new ItemActivator(item));
            setText(2, 0, getUsageMessage(item.getType()));
        } else {
            String type = CShop.dmsgs.getString("itemType" + item.getType());
            setText(1, 0, CShop.msgs.bipViewStuff(type));
            String ptype = CShop.dmsgs.getString("pItemType" + item.getType());
            setWidget(2, 0, Application.createLink(CShop.msgs.bipGoNow(ptype),
                                                   Page.STUFF, ""+item.getType()));
        }
    }

    protected String getUsageMessage (byte itemType)
    {
        if (itemType == Item.AVATAR) {
            return CShop.msgs.bipAvatarUsage();
        } else if (itemType == Item.DECOR) {
            return CShop.msgs.bipDecorUsage();
        } else if (itemType == Item.AUDIO) {
            return CShop.msgs.bipAudioUsage();
        } else if (itemType == Item.PET) {
            return CShop.msgs.bipPetUsage();
        } else {
            return CShop.msgs.bipOtherUsage();
        }
    }
}
