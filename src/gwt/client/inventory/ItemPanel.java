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
        add(_contents = new Grid(ROWS+1, COLUMNS));
        _contents.setStyleName("inventory_contents");

        // these will be used for navigation
        _next = new Button("Next");
        _next.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                displayPage(_page+1, false);
            }
        });
        _prev = new Button("Prev");
        _prev.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                displayPage(_page-1, false);
            }
        });

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
                    displayPage(_page, true);
                }
                public void onFailure (Throwable caught) {
                    GWT.log("loadInventory failed", caught);
                    // TODO: if ServiceException, translate
                    add(new Label("Failed to load inventory."));
                }
            });
        }
    }

    protected void displayPage (int page, boolean forceRefresh)
    {
        if (_page == page && !forceRefresh) {
            return; // NOOP!
        }

        _page = Math.max(page, 0);
        _contents.clear();
        if (_items == null || _items.size() == 0) {
            _contents.setText(0, 0, "You have no " + Item.getTypeName(_type) + " items.");
            return;
        }

        int count = COLUMNS * ROWS, start = COLUMNS * ROWS * page;
        int limit = Math.min(count, _items.size()-start), row = -1;
        for (int ii = 0; ii < limit; ii++) {
            _contents.setWidget(row = (ii / COLUMNS), ii % COLUMNS,
                                new ItemContainer(ItemPanel.this, (Item)_items.get(ii+start)));
        }

        _contents.setWidget(row+1, 0, _prev);
        _prev.setEnabled(start > 0);
        _contents.setWidget(row+1, 1, _next);
        _next.setEnabled(start+limit < _items.size());
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

    protected Grid _contents;
    protected Button _create, _next, _prev;
    protected Label _status;

    protected byte _type;
    protected ArrayList _items;
    protected int _page;
}
