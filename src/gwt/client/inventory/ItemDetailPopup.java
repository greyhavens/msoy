//
// $Id$

package client.inventory;

import java.util.Collection;
import java.util.Iterator;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
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

import client.item.BaseItemDetailPopup;
import client.shell.MsoyEntryPoint;
import client.util.WebContext;

/**
 * Displays a popup detail view of an item from the user's inventory.
 */
public class ItemDetailPopup extends BaseItemDetailPopup
{
    public ItemDetailPopup (WebContext ctx, Item item, ItemPanel parent)
    {
        super(ctx, item);
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
            controls.add(new HTML("<a href=\"/game/index.html#" + gameId + "\">Play!</a>"));

        } else if (_item instanceof Photo) {
            // TODO: "Caption", ((Photo)_item).caption);
        }

        Button button;
        if (_item.parentId == -1) {
            button = new Button("List in Catalog ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    listItem(_item);
                }
            });

        } else {
            button = new Button("Remix ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    remixItem(_item);
                }
            });
        }
        controls.add(button);

        if (_item.parentId == -1) {
            button = new Button("Edit ...");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = _parent.createItemEditor(_item.getType());
                    editor.setItem(_item);
                    editor.show();
                }
            });
            controls.add(button);
        }

        controls.add(_status = new Label(""));
    }

    protected void listItem (Item item)
    {
        _ctx.catalogsvc.listItem(_ctx.creds, item.getIdent(), true, new AsyncCallback() {
            public void onSuccess (Object result) {
                _status.setText("Item listed.");
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                _status.setText("Item listing failed: " + reason);
            }
        });
    }

    protected void remixItem (Item item)
    {
        _ctx.itemsvc.remixItem(_ctx.creds, item.getIdent(), new AsyncCallback() {
            public void onSuccess (Object result) {
                // TODO: update display
                _status.setText("Item remixed.");
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                _status.setText("Item remixing failed: " + reason);
            }
        });
    }

    protected ItemPanel _parent;
    protected Label _status;
}
