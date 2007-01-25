//
// $Id$

package client.inventory;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Photo;

import client.editem.ItemEditor;
import client.item.BaseItemDetailPopup;
import client.shell.MsoyEntryPoint;
import client.util.ClickCallback;

/**
 * Displays a popup detail view of an item from the user's inventory.
 */
public class ItemDetailPopup extends BaseItemDetailPopup
{
    public ItemDetailPopup (Item item, ItemPanel parent)
    {
        super(item);
        _parent = parent;
    }

    // @Override // BaseItemDetailPopup
    protected Widget createPreview (Item item)
    {
        // TODO: maybe ItemUtil should handle this
        if (item instanceof Avatar) {
            MediaDesc avatarMedia = ((Avatar)_item).avatarMedia;
            String path = MsoyEntryPoint.toMediaPath(avatarMedia.getMediaPath());
            return WidgetUtil.createFlashContainer(
                "avatarViewer", "/clients/avatarviewer.swf", 300, 500,
                "avatar=" + URL.encodeComponent(path));
        } else {
            return super.createPreview(item);
        }
    }

    // @Override // BaseItemDetailPopup
    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        super.createInterface(details, controls);

        if (_item instanceof Furniture) {
            // TODO: "Action", ((Furniture)_item).action

        } else if (_item instanceof Game) {
            // TODO: ((Game)_item).name,
            // TODO: "# Players (Desired)", String.valueOf(((Game)_item).desiredPlayers));
            // TODO: "# Players (Minimum)", String.valueOf(((Game)_item).minPlayers),
            // TODO: "# Players (Maximum)", String.valueOf(((Game)_item).maxPlayers));

            int gameId = _item.getProgenitorId();
            controls.add(new HTML("<a href=\"/game/index.html#" + gameId + "\">" +
                                  CInventory.msgs.detailPlay() + "</a>"));

        } else if (_item instanceof Photo) {
            // TODO: "Caption", ((Photo)_item).caption);
        }

        // we'll need this now so that we can pass it to our click callbacks
        _status = new Label("");
        _status.setStyleName("itemDetailStatus");

        Button button;
        if (_item.parentId == 0) {
            button = new Button(CInventory.msgs.detailList());
            new ClickCallback(button, _status) {
                public boolean callService () {
                    // make sure the item is kosher; TODO: make this less of a hack
                    if (_item.name.trim().length() == 0) {
                        _status.setText(CInventory.msgs.errItemMissingName());
                        return false;
                    }
                    if (_item.description.trim().length() == 0) {
                        _status.setText(CInventory.msgs.errItemMissingDescrip());
                        return false;
                    }
                    CInventory.catalogsvc.listItem(CInventory.creds, _item.getIdent(), true, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    _status.setText(CInventory.msgs.msgItemListed());
                    return false; // don't reenable list button
                }
            };

        } else {
            button = new Button(CInventory.msgs.detailRemix());
            new ClickCallback(button, _status) {
                public boolean callService () {
                    CInventory.itemsvc.remixItem(CInventory.creds, _item.getIdent(), this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    // TODO: update item panel
                    _status.setText(CInventory.msgs.msgItemRemixed());
                    return false; // don't reenable remix button
                }
            };
        }
        controls.add(button);

        button = new Button(CInventory.msgs.detailDelete());
        new ClickCallback(button, _status) {
            public boolean callService () {
                CInventory.itemsvc.deleteItem(CInventory.creds, _item.getIdent(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                _parent.itemDeleted(_item);
                hide();
                return false;
            }
        };
        controls.add(button);

        if (_item.parentId == 0) {
            button = new Button(CInventory.msgs.detailEdit());
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = _parent.createItemEditor(_item.getType());
                    editor.setItem(_item);
                    editor.show();
                    hide();
                }
            });
            controls.add(button);
        }

        controls.add(_status);
    }

    protected ItemPanel _parent;
    protected Label _status;
}
