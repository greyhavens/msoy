//
// $Id$

package client.inventory;

import java.util.ArrayList;
import java.util.List;

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
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.FlashClients;
import client.util.MsoyCallback;
import client.util.MsoyUI;

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
                Application.go(Page.INVENTORY, Args.compose(new String[] { ""+_type, ""+page }));
            }
            protected Widget createWidget (Object item) {
                return new ItemEntry(ItemPanel.this, (Item)item, _itemList);
            }
            protected String getEmptyMessage () {
                return CInventory.msgs.panelNoItems(CInventory.dmsgs.getString("itemType" + _type));
            }
        };
        _contents.addStyleName("inventoryContents");

        boolean isCatalogItem = false;
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            if (type == Item.TYPES[ii]) {
                isCatalogItem = true;
                break;
            }
        }
        if (isCatalogItem) {
            addUploadInterface();
        }
    }

    public void minimizeInventory ()
    {
        Frame.setContentMinimized(true, null);
    }

    /**
     * Requests that the specified page of inventory items be displayed.
     */
    public void setPage (int page)
    {
        // refresh our item list every time we switch pages; it's cheap
        _itemList.clear();
        if (_type == Item.PET) {
            _itemList.addAll(FlashClients.getPetList());
        } else {
            _itemList.addAll(FlashClients.getFurniList());
        }

        // make sure we're shoing and have our data
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
        if (_detail != null && _detail.item.getIdent().equals(ident)) {
            showDetail(_detail);
            return;
        }

        // load up the item details
        CInventory.itemsvc.loadItemDetail(CInventory.ident, ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                showDetail((ItemDetail)result);
            }
        });
    }

    /**
     * Displays the supplied item detail.
     */
    public void showDetail (ItemDetail detail)
    {
        _detail = detail;
        clear();
        add(new ItemDetailPanel(_detail, this));
    }

    /**
     * Called by the {@link ItemEntry} to request that we do the browser history jockeying to
     * cause the specified item's detail to be shown.
     */
    public void requestShowDetail (ItemIdent ident)
    {
        Application.go(Page.INVENTORY, Args.compose(new String[] {
            ""+ident.type, ""+_contents.getPage(), ""+ident.itemId }));
    }

    // from EditorHost
    public void editComplete (Item item)
    {
        _create.setEnabled(true);

        if (item != null) {
            // refresh the detail view
            if (_detail != null && _detail.item.getIdent().equals(item.getIdent())) {
                _detail.item = item;
                showDetail(_detail);
            } else {
                ((SimpleDataModel)_contents.getModel()).addItem(0, item);
                _contents.displayPage(0, true);
            }
        }
    }

    protected void loadInventory ()
    {
        CInventory.membersvc.loadInventory(CInventory.ident, _type, 0, new AsyncCallback() {
            public void onSuccess (Object result) {
                _contents.setModel(new SimpleDataModel((List)result), _startPage);
            }
            public void onFailure (Throwable caught) {
                CInventory.log("loadInventory failed", caught);
                add(new Label(CInventory.serverError(caught)));
            }
        });
    }

    protected boolean isShowing (ItemIdent ident)
    {
        Widget top = (getWidgetCount() > 0) ? getWidget(0) : null;
        return (top instanceof ItemDetailPanel && ((ItemDetailPanel)top).isShowing(ident));
    }

    protected void createNewItem ()
    {
        ItemEditor editor = ItemEditor.createItemEditor(_type, this);
        if (editor != null) {
            _create.setEnabled(false);
            editor.setItem(editor.createBlankItem());
            editor.show();
        }
    }

    protected void addUploadInterface ()
    {
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
        header.setText(0, 1, CInventory.dmsgs.getString("itemUploadTitle" + _type));
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
        String why = (CInventory.dmsgs.getString("itemUploadPitch" + _type + "a") + "<br>" +
                      CInventory.dmsgs.getString("itemUploadPitch" + _type + "b") + "<br>" +
                      CInventory.dmsgs.getString("itemUploadPitch" + _type + "c"));
        contents.setHTML(0, 0, why);

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
     * Requests that the current inventory page be displayed (clearing out any currently displayed
     * item detail view).
     */
    protected void showInventory ()
    {
        // don't fiddle with things if the inventory is already showing
        if (!_contents.isAttached()) {
            clear();
            add(_contents);
            if (_upload != null) {
                add(_upload);
            }
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
        // this may be from a sub-item in which case we ignore it
        if (item.getType() == _type) {
            _contents.removeItem(item);
        }
    }

    /**
     * Called by an active {@link ItemDetailPanel} to let us know that an item has been remixed.
     * Since this is a relatively uncommon operation and completely replaces the item with a new
     * one with a new id, we force a server roundtrip refresh.
     */
    protected void itemRemixed (Item oldItem, Item newItem)
    {
        // this may be from a sub-item in which case we ignore it
        if (newItem.getType() == _type) {
            // TODO: add new item to our list
        }
    }

    protected PagedGrid _contents;
    protected Button _create, _next, _prev;
    protected VerticalPanel _upload;

    protected byte _type;
    protected int _startPage;
    protected ItemDetail _detail;

    /** Only get the furni list for the current room once, and feed it to each ItemEntry */
    protected List _itemList = new ArrayList();

    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ + 10 /* gap */;
    protected static final int BLURB_HEIGHT = 25 /* gap */ + 33 /* title */ + 72 /* contents */;
    protected static final int BOX_HEIGHT = MediaDesc.THUMBNAIL_HEIGHT/2 + 15 /* gap */;
}
