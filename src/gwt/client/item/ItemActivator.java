//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;

import client.item.ItemMessages;
import client.ui.MsoyUI;
import client.util.events.ItemUsageEvent;
import client.util.events.ItemUsageListener;
import client.util.events.FlashEvents;

import client.util.FlashClients;

/**
 * Displays an interface for activating an item (wearing an avatar, adding furni to a room, etc.).
 * This should only be added if the Flash client is known to exist: {@link
 * FlashClients#clientExists}.
 */
public class ItemActivator extends FlowPanel
    implements ItemUsageListener
{
    public ItemActivator (Item item, boolean bigAss)
    {
        setStyleName("itemActivator");
        _bigAss = bigAss;
        setItem(item);
    }

    public void setItem (Item item)
    {
        _item = item;
        update();
    }

    // from ItemUsageListener
    public void itemUsageChanged (ItemUsageEvent event)
    {
        if ((_item != null) && (_item.getType() == event.getItemType()) &&
                (_item.itemId == event.getItemId())) {
            update();
        }
    }

    @Override // from Panel
    protected void onAttach ()
    {
        super.onAttach();

        FlashEvents.addListener(this);
        update();
    }

    @Override // from Panel
    protected void onDetach ()
    {
        super.onDetach();

        FlashEvents.removeListener(this);
    }

    protected void update ()
    {
        // TODO: do this in one place?
        boolean hasClient = FlashClients.clientExists();
        boolean isUsed = _item.isUsed();
        boolean usedHere;
        switch (_item.used) {
        default:
            usedHere = false;
            break;

        case Item.USED_AS_FURNITURE:
        case Item.USED_AS_PET:
        case Item.USED_AS_BACKGROUND:
            // TODO: getSceneId out so it's retrieved in one place and shared?
            usedHere = hasClient && (_item.location == FlashClients.getSceneId());
            break;
        }

        clear();

        String suff = isUsed ? "used.png" : "unused.png";
        String tip = null;
        String path;
        ClickListener onClick = null;

        if (!hasClient) {
            tip = isUsed ? _imsgs.inUse() : _imsgs.notInUse();
        }

        byte type = _item.getType();
        final boolean fUsedHere = usedHere;
        if (type == Item.AVATAR) {
            if (hasClient) {
                tip = usedHere ? _imsgs.removeAvatar() : _imsgs.wearAvatar();
                onClick = new ClickListener () {
                    public void onClick (Widget sender) {
                        if (fUsedHere) {
                            FlashClients.useAvatar(0, 0);
                        } else {
                            FlashClients.useAvatar(_item.itemId, ((Avatar) _item).scale);
                            // Frame.closeContent();
                        }
                    }
                };
            }
            path = "/images/ui/checkbox_avatar_" + suff;

        } else {
            if (hasClient) {
                if (usedHere) {
                    tip = _imsgs.removeFromRoom();
                    suff = "usedhere.png";
                } else if (isUsed) {
                    tip = _imsgs.moveToRoom();
                } else {
                    tip = _imsgs.addToRoom();
                }
                onClick = new ClickListener () {
                    public void onClick (Widget sender) {
                        if (fUsedHere) {
                            FlashClients.clearItem(_item.getType(), _item.itemId);
                        } else {
                            FlashClients.useItem(_item.getType(), _item.itemId);
                            // Frame.closeContent();
                        }
                    }
                };
            }
            path = "/images/ui/checkbox_room_" + suff;
        }

        if (_bigAss) {
            add(MsoyUI.createButton(MsoyUI.LONG_THIN, tip, onClick));
        } else {
            add(MsoyUI.createActionImage(path, onClick));
            add(MsoyUI.createLabel(tip, "Tip"));
        }
    }

    protected boolean _bigAss;
    protected Item _item;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
}
