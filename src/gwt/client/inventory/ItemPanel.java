//
// $Id$

package client.inventory;

import java.util.ArrayList;

import client.inventory.ItemContainer;
import client.inventory.ItemEditor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.client.WebContext;

/**
 * Displays all items of a particular type in a player's inventory.
 */
public class ItemPanel extends VerticalPanel
{
    public ItemPanel (WebContext ctx, String type)
    {
        // setStyleName("inventory_item");
        _ctx = ctx;
        _type = type;

        // this will contain our items
        add(_contents = new FlowPanel());

        // this will allow us to create new items
        add(_create = new Button("Create new..."));
        _create.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                createNewItem();
            }
        });
        add(_status = new Label(""));
    }

    // TODO: each ItemPanel is currently loading everything up when
    // the inventory panel loads.
    // We should only load a category when it's made visible.
    // (In addition to only loading inventory in chunks. We cannot show
    // the user's whole inventory, even in one category.)
    protected void onLoad ()
    {
        // load the users inventory if we have no already
        if (_items == null) {
            _ctx.itemsvc.loadInventory(_ctx.creds, _type, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _items = (ArrayList)result;
                    if (_items == null || _items.size() == 0) {
                        _contents.add(
                            new Label("You have no " + _type + " items."));
                    } else {
                        for (int ii = 0; ii < _items.size(); ii++) {
                            _contents.add(new ItemContainer(
                                (Item)_items.get(ii), ItemPanel.this));
                        }
                    }
                }
                public void onFailure (Throwable caught) {
                    GWT.log("loadInventory failed", caught);
                    // TODO: if ServiceException, translate
                    add(new Label("Failed to load inventory."));
                }
            });
        }
    }

    protected void listItem (int itemId, String type)
    {
        _ctx.catalogsvc.listItem(
            _ctx.creds, itemId, type, new AsyncCallback() {
                public void onSuccess (Object result) {
                    setStatus("Item listed.");
                }
                public void onFailure (Throwable caught) {
                    String reason = caught.getMessage();
                    setStatus("Item listing failed: " + reason);
                }
            });
    }

    protected void remixItem (int itemId, String type)
    {
        _ctx.itemsvc.remixItem(
            _ctx.creds, itemId, type, new AsyncCallback() {
                public void onSuccess (Object result) {
                    // TODO: update display
                    setStatus("Item remixed.");
                }
                public void onFailure (Throwable caught) {
                    String reason = caught.getMessage();
                    setStatus("Item remixing failed: " + reason);
                }
            });
    }

    protected void createNewItem ()
    {
        ItemEditor editor = null;
        Item item = null;
        if (_type.equals("PHOTO")) {
            editor = new PhotoEditor();
        } else if (_type.equals("DOCUMENT")) {
            editor = new DocumentEditor();
        } else if (_type.equals("FURNITURE")) {
            editor = new FurnitureEditor();
        } else if (_type.equals("GAME")) {
            editor = new GameEditor();
        } else if (_type.equals("AVATAR")) {
            editor = new AvatarEditor();
        }
        if (editor != null) {
            editor.init(_ctx, this);
            editor.setItem(editor.createBlankItem());
            remove(_create);
            insert(editor, 1);
        }
    }

    /**
     * Called by an active {@link ItemEditor} when it is ready to go away
     * (either the editing is done or the user canceled).
     *
     * @param item if the editor was creating a new item, the new item should
     * be passed to this method so that it can be added to the display.
     */
    protected void editComplete (ItemEditor editor, Item item)
    {
        remove(editor);
        insert(_create, 1);
        if (item != null) {
            // we really need to re-fetch the item from the database to get
            // things like itemId set. just refresh the entire list for now.
            _contents.clear();
            _items = null;
            onLoad();
        }
    }

    /**
     * Displays a status message to the user, may be called by item editors.
     */
    protected void setStatus (String status)
    {
        _status.setText(status);
    }

    protected WebContext _ctx;

    protected FlowPanel _contents;
    protected Button _create;
    protected Label _status;

    protected String _type;
    protected ArrayList _items;
}
