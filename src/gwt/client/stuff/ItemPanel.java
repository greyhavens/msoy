//
// $Id$

package client.stuff;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.editem.EditorHost;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
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

    public ItemPanel (InventoryModels models, byte type)
    {
        setStyleName("itemPanel");

        _models = models;
        _type = type;

        // this will contain radio buttons for setting filters (eventually it will have user
        // creatable folders)
        int col = 0;
        _filters = new SmartTable("Filters", 0, 0);
        _filters.setText(0, col++, CStuff.msgs.ipfTitle());
        _filters.setWidget(0, col++, createFilter(CStuff.msgs.ipfAll(), Predicate.TRUE));
        _filters.setWidget(0, col++, createFilter(CStuff.msgs.ipfUploaded(), new Predicate() {
            public boolean isMatch (Object o) {
                return ((Item)o).sourceId == 0;
            }
        }));
        _filters.setWidget(0, col++, createFilter(CStuff.msgs.ipfPurchased(), new Predicate() {
            public boolean isMatch (Object o) {
                return ((Item)o).sourceId != 0;
            }
        }));

        if (isCatalogItem(type)) {
            String slabel = CStuff.msgs.ipShopFor(CStuff.dmsgs.getString("pItemType" + _type));
            _filters.setHTML(0, col++, "&nbsp;");
            _filters.setText(0, col++, slabel, 1, "Shop");
            _filters.setWidget(0, col++, new Button(CStuff.msgs.ipToCatalog(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.SHOP, ""+_type);
                }
            }));
        }

        // determine if we should show the creation UI at the bottom
        int used = Application.HEADER_HEIGHT + NAV_BAR_ETC;
        boolean showCreate = shouldShowCreate(type);
        if (showCreate) {
            used += BLURB_HEIGHT;
        }

        // this will contain our items
        int rows = Math.max(1, (Window.getClientHeight() - used) / BOX_HEIGHT);
        _contents = new PagedGrid(rows, COLUMNS) {
            protected void displayPageFromClick (int page) {
                // route our page navigation through the URL
                Application.go(Page.STUFF, Args.compose(new String[] { ""+_type, ""+page }));
            }
            protected Widget createWidget (Object item) {
                return new ItemEntry((Item)item);
            }
            protected String getEmptyMessage () {
                return CStuff.msgs.panelNoItems(CStuff.dmsgs.getString("itemType" + _type));
            }
            protected boolean displayNavi (int items) {
                return true;
            }
        };
        _contents.addStyleName("Contents");

        if (showCreate) {
            createUploadInterface();
        }
    }

    /**
     * Requests that the specified page of inventory items be displayed.
     */
    public void setPage (int page)
    {
        // if we're asked to display the "default" page, display the last page we remember
        if (page < 0) {
            page = _mostRecentPage;
        }
        _mostRecentPage = page; // now remember this age

        // make sure we're shoing and have our data
        showInventory(page, null);
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
        CStuff.itemsvc.loadItemDetail(CStuff.ident, ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                showDetail((ItemDetail)result);
            }
        });
    }

    /**
     * Requests that detail for the specified item be displayed.
     */
    public void showDetail (Item item)
    {
        if (_detail != null && _detail.item.getIdent().equals(item.getIdent())) {
            _detail.item = item;
            showDetail(_detail);
        } else {
            showDetail(item.getIdent());
        }
    }

    /**
     * Displays the supplied item detail.
     */
    public void showDetail (ItemDetail detail)
    {
        _detail = detail;
        clear();
        add(new ItemDetailPanel(_models, _detail, this));
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

    protected boolean shouldShowCreate (byte type)
    {
        // if this is photos, music or videos, always show create
        if (type == Item.PHOTO || type == Item.AUDIO || type == Item.VIDEO) {
            return true;
        }

        // otherwise it has to be a catalog item and we have to be a minimum level
        return isCatalogItem(type) && (CStuff.level >= MIN_CREATE_LEVEL);
    }

    protected boolean isCatalogItem (byte type)
    {
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            if (type == Item.TYPES[ii]) {
                return true;
            }
        }
        return false;
    }

    protected RadioButton createFilter (String label, final Predicate pred)
    {
        RadioButton button = new RadioButton("filters", label);
        button.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                showInventory(0, pred);
            }
        });
        button.setChecked(pred == Predicate.TRUE);
        return button;
    }

    protected boolean isShowing (ItemIdent ident)
    {
        Widget top = (getWidgetCount() > 0) ? getWidget(0) : null;
        return (top instanceof ItemDetailPanel && ((ItemDetailPanel)top).isShowing(ident));
    }

    protected void createUploadInterface ()
    {
        // this will allow us to create new items
        _upload = new VerticalPanel();
        _upload.setStyleName("Upload");

        Grid header = new Grid(1, 3);
        header.setStyleName("Header");
        header.setCellSpacing(0);
        header.setCellPadding(0);
        header.getCellFormatter().setStyleName(0, 0, "TitleLeft");
        header.getCellFormatter().setStyleName(0, 1, "TitleCenter");
        header.getCellFormatter().setStyleName(0, 2, "TitleRight");
        header.setText(0, 1, CStuff.dmsgs.getString("itemUploadTitle" + _type));
        _upload.add(header);

        VerticalPanel cwrap = new VerticalPanel();
        cwrap.setStyleName("Body");
        _upload.add(cwrap);

        Grid contents = new Grid(1, 2);
        contents.setStyleName("Table");
        contents.setCellSpacing(0);
        contents.setCellPadding(0);
        contents.getCellFormatter().setStyleName(0, 0, "Pitch");
        contents.getCellFormatter().setStyleName(0, 1, "Button");
        contents.getCellFormatter().setHorizontalAlignment(0, 1, ALIGN_RIGHT);
        contents.getCellFormatter().setVerticalAlignment(0, 1, ALIGN_MIDDLE);
        cwrap.add(contents);

        // add the various "why to upload" pitches
        String why = (CStuff.dmsgs.getString("itemUploadPitch" + _type + "a") + "<br>" +
                      CStuff.dmsgs.getString("itemUploadPitch" + _type + "b") + "<br>" +
                      CStuff.dmsgs.getString("itemUploadPitch" + _type + "c"));
        contents.setHTML(0, 0, why);

        // add the create button
        _create = new Button(CStuff.msgs.panelCreateNew());
        _create.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                CStuff.createItem(_type, (byte)0, 0);
            }
        });
        contents.setWidget(0, 1, _create);
    }

    /**
     * Requests that the current inventory page be displayed (clearing out any currently displayed
     * item detail view).
     */
    protected void showInventory (final int page, final Predicate pred)
    {
        // don't fiddle with things if the inventory is already showing
        if (!_contents.isAttached()) {
            clear();
            add(_filters);
            add(_contents);
            if (_tocat != null) {
                add(_tocat);
            }
            if (_upload != null) {
                add(_upload);
            }
        }

        // maybe we're changing our predicate or changing page on an already loaded model
        SimpleDataModel model = _models.getModel(_type, 0);
        if (model != null) {
            if (pred == null) {
                _contents.displayPage(page, true);
            } else {
                _contents.setModel(model.filter(pred), page);
            }
            return;
        }

        // otherwise we have to load
        _models.loadModel(_type, 0, new MsoyCallback() {
            public void onSuccess (Object result) {
                SimpleDataModel model = (SimpleDataModel)result;
                if (pred != null) {
                    model = model.filter(pred);
                }
                _contents.setModel(model, page);
            }
        });
    }

    protected SimpleDataModel getFilteredModel (Predicate pred)
    {
        return null;
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

    protected InventoryModels _models;
    protected byte _type;
    protected int _mostRecentPage;
    protected ItemDetail _detail;

    protected SmartTable _filters;
    protected PagedGrid _contents;
    protected Button _create, _next, _prev;
    protected SmartTable _tocat;
    protected VerticalPanel _upload;

    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ +
        10 /* gap */ + 25 /*  filters */;
    protected static final int BLURB_HEIGHT = 25 /* gap */ + 33 /* title */ + 72 /* contents */;
    protected static final int BOX_HEIGHT = MediaDesc.THUMBNAIL_HEIGHT + 5 /* gap */;

    protected static final int MIN_CREATE_LEVEL = 5;
}
