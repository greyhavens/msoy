//
// $Id$

package client.inventory;

import java.util.List;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.editem.EditorHost;
import client.editem.ItemEditor;
import client.util.MsoyUI;
import client.shell.Application;
import client.shell.Page;

/**
 * Displays all items of a particular type in a player's inventory.
 */
public class ItemPanel extends VerticalPanel
    implements EditorHost
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 4;

    /** The number of rows of items to display. */
    public static final int ROWS = 3;

    public ItemPanel (byte type)
    {
        _type = type;

        // this will contain our items
        _contents = new PagedGrid(ROWS, COLUMNS) {
            protected void displayPageFromClick (int page) {
                // route our page navigation through the URL
                String args = Page.composeArgs(new int[] { _type, page });
                History.newItem(Application.createLinkToken("inventory", args));
            }
            protected Widget createWidget (Object item) {
                return new ItemContainer(ItemPanel.this, (Item)item);
            }
            protected String getEmptyMessage () {
                return CInventory.msgs.panelNoItems(Item.getTypeName(_type));
            }
        };
        _contents.setStyleName("inventoryContents");

        // this will allow us to create new items
        _create = new Button(CInventory.msgs.panelCreateNew());
        _create.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                createNewItem();
            }
        });
    }

    /**
     * Requests that the specified page of inventory items be displayed.
     */
    public void setPage (int page)
    {
        showInventory();

        if (_contents.hasModel()) {
            _contents.displayPage(page, false);
        } else {
            _startPage = page;
        }
    }

    /**
     * Requests that detail for the specified item be displayed.
     */
    public void showDetail (ItemIdent ident)
    {
        // load up the item details
        CInventory.itemsvc.loadItemDetail(CInventory.creds, ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                clear();
                add(new ItemDetailPanel((ItemDetail)result, ItemPanel.this));
            }
            public void onFailure (Throwable caught) {
                MsoyUI.error(CInventory.serverError(caught));
            }
        });
    }

    /**
     * Called by the {@link ItemContainer} to request that we do the browser history jockeying to
     * cause the specified item's detail to be shown.
     */
    public void requestShowDetail (int itemId)
    {
        String args = Page.composeArgs(new int[] { _type, _contents.getPage(), itemId });
        History.newItem(Application.createLinkToken("inventory", args));
    }

    /**
     * Called by the {@link ItemDetailPanel} to request that we do the browser history jockeying to
     * return to our current page.
     */
    public void requestClearDetail ()
    {
        String args = Page.composeArgs(new int[] { _type, _contents.getPage() });
        History.newItem(Application.createLinkToken("inventory", args));
    }

    // from EditorHost
    public void editComplete (Item item)
    {
        _create.setEnabled(true);

        if (item != null) {
            // we really need to re-fetch the item from the database to get things like itemId
            // set. just refresh the entire list for now.
            loadInventory();
        }
    }

    protected void loadInventory ()
    {
        CInventory.membersvc.loadInventory(CInventory.creds, _type, new AsyncCallback() {
            public void onSuccess (Object result) {
                _contents.setModel(new SimpleDataModel((List)result), _startPage);
            }
            public void onFailure (Throwable caught) {
                CInventory.log("loadInventory failed", caught);
                add(new Label(CInventory.serverError(caught)));
            }
        });
    }

    protected void createNewItem ()
    {
        ItemEditor editor = ItemEditor.createItemEditor(_type, this);
        if (editor != null) {
            _create.setEnabled(false);
            Item item = editor.createBlankItem();
            // TEMP: workaround null description problem
            item.description = "";
            editor.setItem(item);
            editor.setPopupPosition(_create.getAbsoluteLeft()+20, _create.getAbsoluteTop()-200);
            editor.show();
        }
    }

    /**
     * Requests that the current inventory page be displayed (clearing out any currently displayed
     * item detail view).
     */
    protected void showInventory ()
    {
        // don't fiddle with things if the inventory is already showing
        if (!_contents.isAttached()) {
            clear();
            add(_contents);
            add(_create);
        }

        // trigger the loading of our inventory the first time we're displayed
        if (!_contents.hasModel()) {
            loadInventory();
        }
    }

    /**
     * Called by an active {@link ItemDetailPanel} to let us know that an item has been deleted
     * from our inventory.
     */
    protected void itemDeleted (Item item)
    {
        showInventory();
        _contents.removeItem(item);
        MsoyUI.info(CInventory.msgs.msgItemDeleted());
    }

    /**
     * Called by an active {@link ItemDetailPanel} to let us know that an item has been remixed.
     * Since this is a relatively uncommon operation and completely replaces the item with a new
     * one with a new id, we force a server roundtrip refresh.
     */
    protected void itemRemixed (Item oldItem, Item newItem)
    {
        MsoyUI.info(CInventory.msgs.msgItemRemixed());
        loadInventory();
    }

    protected PagedGrid _contents;
    protected Button _create, _next, _prev;

    protected byte _type;
    protected int _startPage;
}
