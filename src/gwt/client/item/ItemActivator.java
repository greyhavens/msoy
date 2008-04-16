//
// $Id$

package client.item;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;

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

import client.shell.CShell;
import client.shell.Frame;
import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Displays an interface for activating an item (wearing an avatar, adding furni to a room, etc.).
 * This should only be added if the Flash client is known to exist: {@link
 * FlashClients#clientExists}.
 */
public class ItemActivator extends FlowPanel
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
        updateActionLabel(FlashClients.isItemInUse(_item));
        setupListener();
    }

    // @Override // from Panel
    protected void onAttach ()
    {
        super.onAttach();
        updateActionLabel(FlashClients.isItemInUse(_item));
        setupListener();
    }

    // @Override // from Panel
    protected void onDetach ()
    {
        super.onDetach();
        clearListener();
    }

    protected void updateActionLabel (final boolean active)
    {
        clear();

        String suff = (active ? "active.png" : "inactive.png"), tip, path;
        ClickListener onClick;

        byte type = _item.getType();
        if (type == Item.AVATAR) {
            tip = active ? CShell.imsgs.removeAvatar() : CShell.imsgs.wearAvatar();
            path = "/images/ui/checkbox_avatar_" + suff;
            onClick = new ClickListener () {
                public void onClick (Widget sender) {
                    if (active) {
                        FlashClients.useAvatar(0, 0);
                    } else {
                        FlashClients.useAvatar(_item.itemId, ((Avatar) _item).scale);
                        // Frame.closeContent();
                    }
                }
            };

        } else {
            tip = active ? CShell.imsgs.removeFromRoom() : CShell.imsgs.addToRoom();
            path = "/images/ui/checkbox_room_" + suff;
            onClick = new ClickListener () {
                public void onClick (Widget sender) {
                    if (active) {
                        FlashClients.clearItem(_item.getType(), _item.itemId);
                    } else {
                        FlashClients.useItem(_item.getType(), _item.itemId);
                        // Frame.closeContent();
                    }
                }
            };
        }

        if (_bigAss) {
            add(MsoyUI.createButton(MsoyUI.LONG_THIN, tip, onClick));
        } else {
            add(MsoyUI.createActionImage(path, onClick));
            add(MsoyUI.createLabel(tip, "Tip"));
        }
    }

    protected void setupListener ()
    {
        clearListener();

        byte type = _item.getType();
        if (type == Item.AVATAR) {
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
            FlashEvents.addListener(_listener = new FurniChangeListener() {
                public void furniChanged (FurniChangedEvent event) {
                    if (event.getAddedFurni().contains(_item.getIdent())) {
                        updateActionLabel(true);
                    } else if (event.getRemovedFurni().contains(_item.getIdent())) {
                        updateActionLabel(false);
                    }
                }
            });

        } else if (type == Item.PET) {
            FlashEvents.addListener(_listener = new PetListener() {
                public void petUpdated (PetEvent event) {
                    if (event.getPetId() == _item.itemId) {
                        updateActionLabel(event.addedToRoom());
                    }
                }
            });
        }
    }

    protected void clearListener ()
    {
        if (_listener != null) {
            FlashEvents.removeListener(_listener);
            _listener = null;
        }
    }

    protected boolean _bigAss;
    protected Item _item;
    protected FlashEventListener _listener;
}
