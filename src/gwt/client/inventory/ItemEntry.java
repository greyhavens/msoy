//
// $Id$

package client.inventory;

import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.events.AvatarChangeListener;
import client.util.events.AvatarChangedEvent;
import client.util.events.BackgroundChangeListener;
import client.util.events.BackgroundChangedEvent;
import client.util.events.FlashEventListener;
import client.util.events.FlashEvents;
import client.util.events.FurniChangeListener;
import client.util.events.FurniChangedEvent;
import client.util.events.PetEvent;
import client.util.events.PetListener;

import client.util.FlashClients;
import client.util.ItemUtil;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays a thumbnail version of an item.
 */
public class ItemEntry extends FlexTable
{
    public ItemEntry (ItemPanel panel, Item item, List itemList)
    {
        setCellPadding(0);
        setCellSpacing(0);
        _panel = panel;
        _itemList = itemList;
        setStyleName("itemEntry");
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

        ClickListener clicker = new ClickListener() {
            public void onClick (Widget sender) {
                _panel.requestShowDetail(_item.getIdent());
            }
        };
        Widget mview = MediaUtil.createMediaView(
            item.getThumbnailMedia(), MediaDesc.HALF_THUMBNAIL_SIZE);
        if (mview instanceof Image) {
            ((Image)mview).addClickListener(clicker);
            mview.addStyleName("actionLabel");
        }
        setWidget(0, 0, mview);
        getFlexCellFormatter().setStyleName(0, 0, "Preview");

        setWidget(0, 1, MsoyUI.createActionLabel(ItemUtil.getName(item, true), clicker));
        getFlexCellFormatter().setStyleName(0, 1, "ThumbText");
        if (_item.itemId > 0) { // if this item is an original, style it slightly differently
            getFlexCellFormatter().addStyleName(0, 1, "OriginalThumbText");
        }

        setupListener();
    }

    // @Override // from Panel
    public void clear ()
    {
        super.clear();
        clearListener();
    }

    // @Override // from Panel
    protected void onAttach ()
    {
        super.onAttach();
        setupListener();
    }

    // @Override // from Panel
    protected void onDetach ()
    {
        super.onDetach();
        clearListener();
    }

    protected void setupListener ()
    {
        if (_listener != null || !FlashClients.clientExists()) {
            return;
        }

        byte type = _item.getType();
        final ItemIdent ident = new ItemIdent(type, _item.itemId);

        if (type == Item.AVATAR) {
            updateActionLabel(FlashClients.getAvatarId() == _item.itemId);
            FlashEvents.addListener(_listener = new AvatarChangeListener() {
                public void avatarChanged (AvatarChangedEvent event) {
                    if (event.getAvatarId() == _item.itemId) {
                        updateActionLabel(true);
                    } else if (event.getOldAvatarId() == _item.itemId) {
                        updateActionLabel(false);
                    }
                }
            });

        } else if (type == Item.DECOR || type == Item.AUDIO) {
            updateActionLabel(FlashClients.getSceneItemId(_item.getType()) == _item.itemId);
            FlashEvents.addListener(_listener = new BackgroundChangeListener() {
                public void backgroundChanged (BackgroundChangedEvent event) {
                    if (event.getType() == _item.getType()) {
                        if (event.getBackgroundId() == _item.itemId) {
                            updateActionLabel(true);
                        } else if (event.getOldBackgroundId() == _item.itemId) {
                            updateActionLabel(false);
                        }
                    }
                }
            });

        } else if (type == Item.FURNITURE || type == Item.GAME || type == Item.PHOTO ||
                   type == Item.VIDEO || type == Item.TOY) {
            updateActionLabel(_itemList.contains(ident));
            FlashEvents.addListener(_listener = new FurniChangeListener() {
                public void furniChanged (FurniChangedEvent event) {
                    if (event.getAddedFurni().contains(ident)) {
                        updateActionLabel(true);
                    } else if (event.getRemovedFurni().contains(ident)) {
                        updateActionLabel(false);
                    }
                }
            });

        } else if (type == Item.PET) {
            updateActionLabel(_itemList.contains(ident));
            FlashEvents.addListener(_listener = new PetListener() {
                public void petUpdated (PetEvent event) {
                    if (event.getPetId() == _item.itemId) {
                        updateActionLabel(event.addedToRoom());
                    }
                }
            });
        }
    }

    protected void updateActionLabel (final boolean active)
    {
        Label label;
        String tip;
        byte type = _item.getType();
        if (type == Item.AVATAR) {
            String style = "Avatar" + (active ? "Active" : "Inactive");
            label = MsoyUI.createActionLabel("", style, new ClickListener () {
                public void onClick (Widget sender) {
                    FlashClients.useAvatar(active ? 0 : _item.itemId,
                                           active ? 0 : ((Avatar) _item).scale);
                }
            });
            tip = active ? CInventory.msgs.removeAvatar() : CInventory.msgs.wearAvatar();

        } else if (type == Item.DECOR || type == Item.AUDIO) {
            String style = "Room" + (active ? "Active" : "Inactive");
            label = MsoyUI.createActionLabel("", style, new ClickListener () {
                public void onClick (Widget sender) {
                    FlashClients.useItem(active ? 0 : _item.itemId, _item.getType());
                }
            });
            tip = active ? CInventory.msgs.removeFromRoom() : CInventory.msgs.addToRoom();

        } else if (type == Item.PET) {
            String style = "Room" + (active ? "Active" : "Inactive"); 
            label = MsoyUI.createActionLabel("", style, new ClickListener () {
                public void onClick (Widget sender) {
                    if (active) {
                        FlashClients.removePet(_item.itemId);
                    } else {
                        FlashClients.usePet(_item.itemId);
                    }
                }
            });
            tip = active ? CInventory.msgs.removeFromRoom() : CInventory.msgs.addToRoom();

        } else {
            String style = "Room" + (active ? "Active" : "Inactive");
            label = MsoyUI.createActionLabel("", style, new ClickListener () {
                public void onClick (Widget sender) {
                    if (active) {
                        FlashClients.removeFurni(_item.itemId, _item.getType());
                    } else {
                        FlashClients.useItem(_item.itemId, _item.getType());
                    }
                }
            });
            tip = active ? CInventory.msgs.removeFromRoom() : CInventory.msgs.addToRoom();
        }
        setWidget(1, 0, label);
        setText(1, 1, tip);
        getFlexCellFormatter().setStyleName(1, 1, "Tip");

        // adjust the rest of the table to accomodate our bits
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setColSpan(0, 1, 2);
    }

    protected void clearListener ()
    {
        if (_listener != null) {
            FlashEvents.removeListener(_listener);
            _listener = null;
        }
    }

    protected ItemPanel _panel;
    protected Item _item;

    protected FlashEventListener _listener;
    protected List _itemList;
}
