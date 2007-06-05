//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.ItemUtil;
import client.util.FlashClients;
import client.util.FlashEvents;
import client.util.MediaUtil;
import client.util.MsoyUI;
import client.util.events.AvatarChangedEvent;
import client.util.events.AvatarChangeListener;
import client.util.events.FlashEventListener;

/**
 * Displays a thumbnail version of an item.
 */
public class ItemContainer extends FlexTable
{
    public ItemContainer (ItemPanel panel, Item item)
    {
        setCellPadding(0);
        setCellSpacing(0);
        _panel = panel;
        setStyleName("itemContainer");
        setItem(item);
    }

    public void setItem (Item item)
    {
        if (item == null) {
            return;
        }
        _item = item;

        // clear out our old UI, and we'll create it anew
        clear();

        Widget mview = MediaUtil.createMediaView(
            item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
        if (mview instanceof Image) {
            ((Image)mview).addClickListener(_clicker);
            mview.addStyleName("actionLabel");
        }
        setWidget(0, 0, mview);
        getFlexCellFormatter().setStyleName(0, 0, "Preview");
        getFlexCellFormatter().setColSpan(0, 0, 2);

        Label label = new Label(ItemUtil.getName(item, true));
        label.setStyleName("ThumbText");
        label.addClickListener(_clicker);
        setWidget(1, 1, label);

        if (FlashClients.clientExists()) {
            if (_item instanceof Avatar) {
                setWidget(1, 0, generateActionLabel(FlashClients.getAvatarId() == _item.itemId));
                clearListener();
                FlashEvents.addListener(_listener = new AvatarChangeListener() {
                    public void avatarChanged (AvatarChangedEvent event) {
                        if (event.getAvatarId() == _item.itemId) {
                            setWidget(1, 0, generateActionLabel(true));
                        } else if (event.getOldAvatarId() == _item.itemId) {
                            setWidget(1, 0, generateActionLabel(false));
                        }
                    }
                });
            }
        }
    }

    // @Override // from Panel
    public void clear ()
    {
        super.clear();
        clearListener();
    }

    // @Override // from Panel
    protected void onDetach ()
    {
        super.onDetach();
        clearListener();
    }

    protected void clearListener ()
    {
        if (_listener != null) {
            FlashEvents.removeListener(_listener);
            _listener = null;
        }
    }

    protected Widget generateActionLabel (final boolean active)
    {
        if (_item instanceof Avatar) {
            return MsoyUI.createActionLabel("", "Avatar" + (active ? "Active" : "Inactive"), 
                new ClickListener () {
                    public void onClick (Widget sender) {
                        FlashClients.useAvatar(active ? 0 : _item.itemId,
                            active ? 0 : ((Avatar) _item).scale);
                    }
                }
            );
        } else {
            return null;
        }
    }

    protected ClickListener _clicker = new ClickListener() {
        public void onClick (Widget sender) {
            CInventory.log("Getting info on " + _item.itemId);
            _panel.requestShowDetail(_item.itemId);
        }
    };

    protected ItemPanel _panel;
    protected Item _item;

    protected FlashEventListener _listener;
}
