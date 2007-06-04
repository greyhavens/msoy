//
// $Id$

package client.inventory;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.editem.ItemEditor;
import client.item.BaseItemDetailPanel;
import client.shell.Application;
import client.util.ClickCallback;
import client.util.FlashClients;
import client.util.FlashEvents;
import client.util.ItemUtil;
import client.util.PopupMenu;
import client.util.events.AvatarChangeListener;
import client.util.events.AvatarChangedEvent;
import client.util.events.FlashEventListener;
import client.shell.Page;

/**
 * Displays a popup detail view of an item from the user's inventory.
 */
public class ItemDetailPanel extends BaseItemDetailPanel
{
    public ItemDetailPanel (ItemDetail detail, ItemPanel parent)
    {
        super(detail);
        _parent = parent;
    }

    // @Override // BaseItemDetailPanel
    protected void createInterface (VerticalPanel details)
    {
        super.createInterface(details);

        ItemUtil.addItemSpecificButtons(_item, _buttons);

        Button button = new Button(CInventory.msgs.detailDelete());
        new ClickCallback(button) {
            public boolean callService () {
                CInventory.itemsvc.deleteItem(CInventory.ident, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                _parent.itemDeleted(_item);
                return false;
            }
        };
        _buttons.add(button);

        if (_item.parentId == 0) {
            button = new Button(CInventory.msgs.detailEdit());
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = ItemEditor.createItemEditor(_item.getType(), _parent);
                    editor.setItem(_item);
                    editor.show();
                }
            });
            _buttons.add(button);
        }

        if (_item.parentId == 0) {
            _details.add(WidgetUtil.makeShim(1, 10));
            _details.add(new Label(CInventory.msgs.detailListTip()));
            button = new Button(CInventory.msgs.detailList(), new ClickListener() {
                public void onClick (Widget sender) {
                    new DoListItemPopup(_item).show();
                }
            });
            _details.add(button);

        } else /* TODO: if (remixable) */ {
            _details.add(WidgetUtil.makeShim(1, 10));
            _details.add(new Label(CInventory.msgs.detailRemixTip()));
            button = new Button(CInventory.msgs.detailRemix());
            new ClickCallback(button) {
                public boolean callService () {
                    CInventory.itemsvc.remixItem(CInventory.ident, _item.getIdent(), this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    _parent.itemRemixed (_item, (Item) result);
                    return false;
                }
            };
            _details.add(button);
        }

        if (FlashClients.inRoom()) {
            _details.add(WidgetUtil.makeShim(1, 10));
            byte type = _detail.item.getType();
            if (type == Item.DECOR) {
                boolean using = (FlashClients.getSceneItemId(Item.DECOR) == _detail.item.itemId);
                button = new UpdateFlashButton(using, CInventory.msgs.detailRemoveDecor(),
                    CInventory.msgs.detailUseDecor()) {
                    public void onClick () {
                        FlashClients.useItem(_active ? 0 : _detail.item.itemId, Item.DECOR);
                    }
                };
            } else if (type == Item.AUDIO) {
                boolean using = (FlashClients.getSceneItemId(Item.AUDIO) == _detail.item.itemId);
                button = new UpdateFlashButton(using, CInventory.msgs.detailRemoveAudio(),
                    CInventory.msgs.detailUseAudio()) {
                    public void onClick () {
                        FlashClients.useItem(_active ? 0 : _detail.item.itemId, Item.AUDIO);
                    }
                };
            } else if (type == Item.AVATAR) { 
                boolean wearing = (FlashClients.getAvatarId() == _detail.item.itemId);
                final UpdateFlashButton ufb = new UpdateFlashButton(wearing, 
                    CInventory.msgs.detailRemoveAvatar(), CInventory.msgs.detailUseAvatar()) {
                    public void onClick () {
                        FlashClients.useAvatar(_active ? 0 : _detail.item.itemId,
                            _active ? 0 : ((Avatar) _detail.item).scale);
                    }
                };
                FlashEvents.addListener(_listener = new AvatarChangeListener() {
                    public void avatarChanged (AvatarChangedEvent event) {
                        CInventory.log("new: " + event.getAvatarId() + ", old: " + 
                            event.getOldAvatarId());
                        if (event.getAvatarId() == _detail.item.itemId) {
                            ufb.setActive(true);
                        } else if (event.getOldAvatarId() == _detail.item.itemId) {
                            ufb.setActive(false);
                        }
                    }
                });
                button = ufb;
            } else {
                button = new Button(CInventory.msgs.detailAddToRoom());
                button.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        FlashClients.useItem(_detail.item.itemId, _detail.item.getType());
                    }
                });
            }
            _details.add(button);
        }

        // TODO: When catalog browsing is fully URL-friendly, browsing catalog by creator from here
        // will be straightforward
        /*_creator.setMember(_detail.creator, new PopupMenu() {
            protected void addMenuItems () {
                this.addMenuItem(CInventory.imsgs.viewProfile(), new Command() {
                    public void execute () {
                        History.newItem(Application.createLinkToken("profile",
                            "" + _detail.creator.getMemberId()));
                    }
                });
                this.addMenuItem(CInventory.imsgs.browseCatalogFor(), new Command() {
                    public void execute () {
                        // TODO
                    }
                });
            }
        });*/
    }

    // @Override // Panel
    protected void onDetach ()
    {
        super.onDetach();

        if (_listener != null) {
            FlashEvents.removeListener(_listener);
        }
    }

    // @Override // BaseItemDetailPanel
    protected void returnToList ()
    {
        _parent.requestClearDetail();
    }

    // @Override // BaseItemDetailPanel
    protected boolean allowAvatarScaleEditing ()
    {
        return true;
    }

    protected ItemPanel _parent;

    protected FlashEventListener _listener;

    protected abstract class UpdateFlashButton extends Button 
    {
        public UpdateFlashButton (boolean active, String activeLabel, String inactiveLabel)
        {
            super(active ? activeLabel : inactiveLabel);
            addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    UpdateFlashButton.this.onClick();
                }
            });

            _active = active;
            _activeLabel = activeLabel;
            _inactiveLabel = inactiveLabel;
        }

        public abstract void onClick ();

        public void setActive (boolean active) 
        {
            _active = active;
            setText(_active ? _activeLabel : _inactiveLabel);
        }

        protected boolean _active;
        protected String _activeLabel;
        protected String _inactiveLabel;
    }
}
