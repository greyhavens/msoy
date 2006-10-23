//
// $Id$

package client.inventory;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import client.util.PagedGrid;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.client.WebContext;

/**
 * Displays all items of a particular type in a player's inventory.
 */
public class ItemPanel extends VerticalPanel
{
    public static final int COLUMNS = 2;

    public static final int ROWS = 3;

    public ItemPanel (WebContext ctx, byte type)
    {
        // setStyleName("inventory_item");
        _ctx = ctx;
        _type = type;

        // this will contain our items (an extra for for "next, back")
        add(_contents = new PagedGrid(ROWS+1, COLUMNS) {
            protected Widget createWidget (Object item) {
                return new ItemContainer(ItemPanel.this, (Item)item);
            }
            protected String getEmptyMessage () {
                return "You have no " + Item.getTypeName(_type) + " items.";
            }
        });
        _contents.setStyleName("inventory_contents");

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
        if (!_contents.hasItems()) {
            _ctx.itemsvc.loadInventory(_ctx.creds, _type, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _contents.setItems((ArrayList)result);
                }
                public void onFailure (Throwable caught) {
                    GWT.log("loadInventory failed", caught);
                    // TODO: if ServiceException, translate
                    add(new Label("Failed to load inventory."));
                }
            });
        }
    }

    protected void listItem (ItemIdent item)
    {
        _ctx.catalogsvc.listItem(_ctx.creds, item, new AsyncCallback() {
            public void onSuccess (Object result) {
                setStatus("Item listed.");
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                setStatus("Item listing failed: " + reason);
            }
        });
    }

    protected void remixItem (ItemIdent item)
    {
        _ctx.itemsvc.remixItem(_ctx.creds, item, new AsyncCallback() {
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
        ItemEditor editor = createItemEditor(_type);
        if (editor != null) {
            _create.setEnabled(false);
            editor.setItem(editor.createBlankItem());
            editor.setPopupPosition(
                _create.getAbsoluteLeft()+20, _create.getAbsoluteTop()-200);
            editor.show();
        }
    }

    /**
     * Creates an item editor interface for items of the specified type.
     * Returns null if the type is unknown.
     */
    protected ItemEditor createItemEditor (int type)
    {
        ItemEditor editor = null;
        if (_type == Item.PHOTO) {
            editor = new PhotoEditor();
        } else if (_type == Item.DOCUMENT) {
            editor = new DocumentEditor();
        } else if (_type == Item.FURNITURE) {
            editor = new FurnitureEditor();
        } else if (_type == Item.GAME) {
            editor = new GameEditor();
        } else if (_type == Item.AVATAR) {
            editor = new AvatarEditor();
        } else if (_type == Item.PET) {
            editor = new PetEditor();
        } else {
            return null; // woe be the caller
        }
        editor.init(_ctx, this);
        return editor;
    }

    /**
     * Called by an active {@link ItemEditor} when it is ready to go away
     * (either the editing is done or the user canceled).
     *
     * @param item if the editor was creating a new item, the new item should
     * be passed to this method so that it can be added to the display.
     */
    protected void editComplete (Item item)
    {
        _create.setEnabled(true);

        if (item != null) {
            // we really need to re-fetch the item from the database to get
            // things like itemId set. just refresh the entire list for now.
            _contents.setItems(null);
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

    protected PagedGrid _contents;
    protected Button _create, _next, _prev;
    protected Label _status;

    protected byte _type;
    protected ArrayList _items;
    protected int _page;
}
