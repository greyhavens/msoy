//
// $Id$

package client.inventory;

import java.util.List;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.editem.EditorHost;
import client.editem.ItemEditor;
import client.util.FlashClients;
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
    public static final int COLUMNS = 3;

    public ItemPanel (byte type)
    {
        _type = type;

        // this will contain our items
        int rows = (Window.getClientHeight() - Application.HEADER_HEIGHT -
                    NAV_BAR_ETC - BLURB_HEIGHT) / BOX_HEIGHT;
        _contents = new PagedGrid(rows, COLUMNS) {
            protected void displayPageFromClick (int page) {
                // route our page navigation through the URL
                String args = Page.composeArgs(new int[] { _type, page });
                History.newItem(Application.createLinkToken("inventory", args));
            }
            protected Widget createWidget (Object item) {
                return new ItemContainer(ItemPanel.this, (Item)item, _itemList);
            }
            protected String getEmptyMessage () {
                return CInventory.msgs.panelNoItems(Item.getTypeName(_type));
            }
        };
        _contents.addStyleName("inventoryContents");

        // this will allow us to create new items
        _upload = new VerticalPanel();
        _upload.setSpacing(0);
        _upload.setStyleName("uploadBlurb");

        Grid header = new Grid(1, 3);
        header.setStyleName("Header");
        header.setCellSpacing(0);
        header.setCellPadding(0);
        header.getCellFormatter().setStyleName(0, 0, "TitleLeft");
        header.getCellFormatter().setStyleName(0, 1, "TitleCenter");
        header.getCellFormatter().setStyleName(0, 2, "TitleRight");
        header.setText(0, 1, CInventory.dmsgs.getString("itemUploadTitle" + type));
        _upload.add(header);

        VerticalPanel cwrap = new VerticalPanel();
        cwrap.setStyleName("Contents");
        _upload.add(cwrap);
        
        Grid contents = new Grid(1, 2); 
        contents.setStyleName("Table");
        contents.setCellSpacing(0);
        contents.setCellPadding(0);
        contents.getCellFormatter().setStyleName(0, 0, "Pitch");
        contents.getCellFormatter().setStyleName(0, 1, "Upload");
        contents.getCellFormatter().setHorizontalAlignment(0, 1, ALIGN_RIGHT);
        contents.getCellFormatter().setVerticalAlignment(0, 1, ALIGN_MIDDLE);
        cwrap.add(contents);

        // add the various "why to upload" pitches
        contents.setHTML(0, 0, CInventory.dmsgs.getString("itemUploadPitch" + type + "a") + "<br>" +
                         CInventory.dmsgs.getString("itemUploadPitch" + type + "b") + "<br>" +
                         CInventory.dmsgs.getString("itemUploadPitch" + type + "c"));

        // add the create button
        _create = new Button(CInventory.msgs.panelCreateNew());
        _create.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                createNewItem();
            }
        });
        contents.setWidget(0, 1, _create);
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
        CInventory.itemsvc.loadItemDetail(CInventory.ident, ident, new AsyncCallback() {
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
            // indicate that we need to refresh the loaded inventory list
            _contentsModelDirty = true;
            // refresh the detail view
            showDetail(new ItemIdent(item.getType(), item.itemId));
        }
    }

    protected void loadInventory ()
    {
        CInventory.membersvc.loadInventory(CInventory.ident, _type, new AsyncCallback() {
            public void onSuccess (Object result) {
                _contentsModelDirty = false;
                if (_type == Item.PET) {
                    _itemList = FlashClients.getPetList();
                } else {
                    _itemList = FlashClients.getFurniList();
                }
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
            add(_upload);
        }

        // trigger the loading of our inventory the first time we're displayed
        if (!_contents.hasModel() || _contentsModelDirty) {
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
    protected VerticalPanel _upload;

    protected byte _type;
    protected int _startPage;

    /** A flag to indicate that the next time we display this item panel, the data needs to be
     * refetched from the server. */
    protected boolean _contentsModelDirty = false;

    /** Only get the furni list for the current room once, and feed it to each ItemContainer */
    protected List _itemList;

    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ + 10 /* gap */;
    protected static final int BLURB_HEIGHT = 25 /* gap */ + 33 /* title */ + 72 /* contents */;
    protected static final int BOX_HEIGHT = MediaDesc.THUMBNAIL_HEIGHT/2 + 15 /* gap */;
}
