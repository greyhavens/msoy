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

import client.util.ItemUtil;
import client.util.FlashClients;
import client.util.MediaUtil;
import client.util.MsoyUI;
import client.util.events.AvatarChangedEvent;
import client.util.events.AvatarChangeListener;
import client.util.events.BackgroundChangedEvent;
import client.util.events.BackgroundChangeListener;
import client.util.events.FlashEvents;
import client.util.events.FurniChangedEvent;
import client.util.events.FurniChangeListener;
import client.util.events.FlashEventListener;
import client.util.events.PetEvent;
import client.util.events.PetListener;

/**
 * Displays a thumbnail version of an item.
 */
public class ItemContainer extends FlexTable
{
    public ItemContainer (ItemPanel panel, Item item, List itemList)
    {
        setCellPadding(0);
        setCellSpacing(0);
        _panel = panel;
        _itemList = itemList;
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
            item.getThumbnailMedia(), MediaDesc.HALF_THUMBNAIL_SIZE);
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

        addListener();
    }

    // @Override // from Panel
    public void clear ()
    {
        super.clear();
        removeListener();
    }

    // @Override // from Panel
    protected void onDetach ()
    {
        super.onDetach();
        removeListener();
    }

    // @Override // from Panel
    protected void onAttach ()
    {
        super.onAttach();
        addListener();
    }

    protected void addListener ()
    {
        if (_listener != null || !FlashClients.clientExists()) {
            return;
        }

        byte type = _item.getType();
        if (type == Item.AVATAR) { 
            setWidget(1, 0, generateActionLabel(FlashClients.getAvatarId() == _item.itemId));
            FlashEvents.addListener(_listener = new AvatarChangeListener() {
                public void avatarChanged (AvatarChangedEvent event) {
                    if (event.getAvatarId() == _item.itemId) {
                        setWidget(1, 0, generateActionLabel(true));
                    } else if (event.getOldAvatarId() == _item.itemId) {
                        setWidget(1, 0, generateActionLabel(false));
                    }
                }
            });

        } else if (type == Item.DECOR || type == Item.AUDIO) {
            setWidget(1, 0, generateActionLabel(
                          FlashClients.getSceneItemId(_item.getType()) == _item.itemId));
            FlashEvents.addListener(_listener = new BackgroundChangeListener() {
                public void backgroundChanged (BackgroundChangedEvent event) {
                    if (event.getType() == _item.getType()) {
                        if (event.getBackgroundId() == _item.itemId) {
                            setWidget(1, 0, generateActionLabel(true));
                        } else if (event.getOldBackgroundId() == _item.itemId) {
                            setWidget(1, 0, generateActionLabel(false));
                        }
                    }
                }
            });

        } else if (type == Item.FURNITURE || type == Item.GAME || type == Item.PHOTO ||
                type == Item.VIDEO) {
            final ItemIdent ident = new ItemIdent(type, _item.itemId);
            setWidget(1, 0, generateActionLabel(_itemList.contains(ident)));
            FlashEvents.addListener(_listener = new FurniChangeListener() {
                public void furniChanged (FurniChangedEvent event) {
                    if (event.getAddedFurni().contains(ident)) {
                        setWidget(1, 0, generateActionLabel(true));
                    } else if (event.getRemovedFurni().contains(ident)) {
                        setWidget(1, 0, generateActionLabel(false));
                    }
                }
            });
        } else if (type == Item.PET) {
            setWidget(1, 0, generateActionLabel(_itemList.contains(
                new ItemIdent(type, _item.itemId))));
            FlashEvents.addListener(_listener = new PetListener() {
                public void petUpdated (PetEvent event) {
                    if (event.getPetId() == _item.itemId) {
                        if (event.addedToRoom()) {
                            setWidget(1, 0, generateActionLabel(true));
                        } else {
                            setWidget(1, 0, generateActionLabel(false));
                        }
                    }
                }
            });
        }
    }

    protected void removeListener ()
    {
        if (_listener != null) {
            FlashEvents.removeListener(_listener);
            _listener = null;
        }
    }

    protected Widget generateActionLabel (final boolean active)
    {
        byte type = _item.getType();
        // This is a style label, not text to be displayed.  Therefore, it does not require i18n.
        String lbl;
        if (type == Item.AVATAR) {
            lbl = "Avatar" + (active ? "Active" : "Inactive");
            return MsoyUI.createActionLabel("", lbl, new ClickListener () {
                public void onClick (Widget sender) {
                    FlashClients.useAvatar(active ? 0 : _item.itemId,
                                           active ? 0 : ((Avatar) _item).scale);
                }
            });

        } else if (type == Item.DECOR || type == Item.AUDIO) {
            lbl = "Room" + (active ? "Active" : "Inactive");
            return MsoyUI.createActionLabel("", lbl, new ClickListener () {
                public void onClick (Widget sender) {
                    FlashClients.useItem(active ? 0 : _item.itemId, _item.getType());
                }
            });

        } else if (type != Item.PET) {
            lbl = "Room" + (active ? "Active" : "Inactive");
            return MsoyUI.createActionLabel("", lbl, new ClickListener () {
                public void onClick (Widget sender) {
                    if (active) {
                        FlashClients.removeFurni(_item.itemId, _item.getType());
                    } else {
                        FlashClients.useItem(_item.itemId, _item.getType());
                    }
                }
            });

        } else {
            lbl = "Room" + (active ? "Active" : "Inactive"); 
            return MsoyUI.createActionLabel("", lbl, new ClickListener () {
                public void onClick (Widget sender) {
                    if (active) {
                        FlashClients.removePet(_item.itemId);
                    } else {
                        FlashClients.usePet(_item.itemId);
                    }
                }
            });
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
    protected List _itemList;
}
