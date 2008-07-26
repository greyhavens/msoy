//
// $Id$

package client.stuff;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.FlashClients;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.StuffNaviBar;

/**
 * Displays all items of a particular type in a player's inventory.
 */
public class ItemPanel extends VerticalPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 5;

    public ItemPanel (InventoryModels models, byte type)
    {
        setStyleName("itemPanel");

        _models = models;
        _type = type;

        boolean isCatalogType = isCatalogItem(type);

        // a drop down for setting filters (eventually it will have user creatable folders)
        _filters = new ListBox();
        for (int ii = 0; ii < FLABELS.length; ii++) {
            _filters.addItem(FLABELS[ii]);
        }
        _filters.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                showInventory(0, FILTERS[_filters.getSelectedIndex()]);
            }
        });

        if (isCatalogType) {
            _shop = new HorizontalPanel();
            _shop.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
            _shop.add(MsoyUI.createLabel(CStuff.msgs.ipShopFor(), null));
            _shop.add(WidgetUtil.makeShim(5, 5));
            ClickListener onClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Page.SHOP, ""+_type);
                }
            };
            _shop.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, CStuff.msgs.ipToCatalog(), onClick));
            _shop.add(WidgetUtil.makeShim(10, 10));
        }

        // compute the number of rows of items we can fit on the page
        int used = Frame.HEADER_HEIGHT + NAV_BAR_ETC;
        if (isCatalogType) {
            used += BLURB_HEIGHT;
        }
        int boxHeight = BOX_HEIGHT;
        if (FlashClients.clientExists()) {
            boxHeight += ACTIVATOR_HEIGHT;
        }
        int rows = Math.max(1, (Window.getClientHeight() - used) / boxHeight);

        // now create our grid of items
        _contents = new PagedGrid<Item>(rows, COLUMNS) {
            protected void displayPageFromClick (int page) {
                // route our page navigation through the URL
                Link.go(Page.STUFF, Args.compose(new String[] { ""+_type, ""+page }));
            }
            protected Widget createWidget (Item item) {
                return new ItemEntry(item);
            }
            protected String getEmptyMessage () {
                return CStuff.msgs.panelNoItems(CStuff.dmsgs.getString("itemType" + _type));
            }
            protected boolean displayNavi (int items) {
                return true;
            }
            protected void addCustomControls (FlexTable controls) {
                controls.setText(0, 0, CStuff.msgs.ipfTitle());
                controls.getFlexCellFormatter().setStyleName(0, 0, "Show");
                controls.setWidget(0, 1, _filters);
            }
        };
        _contents.addStyleName("Contents");

        // finally optionally add the "create your own" sales blurb
        if (isCatalogType) {
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

    protected boolean isCatalogItem (byte type)
    {
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            if (type == Item.TYPES[ii]) {
                return true;
            }
        }
        return false;
    }

    protected void createUploadInterface ()
    {
        // this will allow us to create new items
        _upload = new SmartTable("Upload", 0, 0);
        _upload.setText(0, 0, CStuff.dmsgs.getString("itemUploadTitle" + _type), 2, "Header");

        // add the various "why to upload" pitches
        String why = getPitch("a") + "<br>" + getPitch("b") + "<br>" + getPitch("c");
        _upload.setHTML(1, 0, why);
        _upload.getFlexCellFormatter().setStyleName(1, 0, "Pitch");

        // add the create button
        _upload.setWidget(1, 1, new Button(CStuff.msgs.panelCreateNew(), new ClickListener() {
            public void onClick (Widget widget) {
                CStuff.createItem(_type, (byte)0, 0);
            }
        }), 1, "Button");
        _upload.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_RIGHT);
    }

    protected String getPitch (String postfix)
    {
        String pitch = CStuff.dmsgs.getString("itemUploadPitch" + _type + postfix);
        if (-1 != pitch.indexOf("@MEMBER_ID@")) {
            return pitch.replaceAll("@MEMBER_ID@", "" + CStuff.getMemberId());
        }
        return pitch;
    }

    /**
     * Requests that the current inventory page be displayed (clearing out any currently displayed
     * item detail view).
     */
    protected void showInventory (final int page, final Predicate<Item> pred)
    {
        // don't fiddle with things if the inventory is already showing
        if (!_contents.isAttached()) {
            clear();
            add(new StuffNaviBar(_type));
            if (_shop != null) {
                add(_shop);
                setCellHorizontalAlignment(_shop, HasAlignment.ALIGN_RIGHT);
            }
            add(_contents);
            if (_upload != null) {
                add(_upload);
            }
        }

        // maybe we're changing our predicate or changing page on an already loaded model
        SimpleDataModel<Item>  model = _models.getModel(_type, 0);
        if (model != null) {
            if (pred == null) {
                _contents.displayPage(page, true);
            } else {
                _contents.setModel(model.filter(pred), page);
            }
            return;
        }

        // otherwise we have to load
        _models.loadModel(_type, 0, new MsoyCallback<SimpleDataModel<Item>>() {
            public void onSuccess (SimpleDataModel<Item> model) {
                if (pred != null) {
                    model = model.filter(pred);
                }
                _contents.setModel(model, page);
            }
        });
    }

    protected InventoryModels _models;
    protected byte _type;
    protected int _mostRecentPage;

    protected HorizontalPanel _shop;
    protected ListBox _filters;
    protected PagedGrid<Item> _contents;
    protected SmartTable _upload;

    protected static final String[] FLABELS = {
        CStuff.msgs.ipfAll(),
        CStuff.msgs.ipfUploaded(),
        CStuff.msgs.ipfPurchased(),
        CStuff.msgs.ipfUnused(),
        CStuff.msgs.ipfUsed()
    };

    protected static final Predicate[] FILTERS = {
        new Predicate.TRUE<Item>(), // show all
        new Predicate<Item>() { // uploaded
            public boolean isMatch (Item item) {
                return item.sourceId == 0;
            }
        },
        new Predicate<Item>() { // purchased
            public boolean isMatch (Item item) {
                return item.sourceId != 0;
            }
        },
        new Predicate<Item>() { // unused
            public boolean isMatch (Item item) {
                return !item.isUsed();
            }
        },
        new Predicate<Item>() { // used
            public boolean isMatch (Item item) {
                return item.isUsed();
            }
        }
    };

    protected static final int NAV_BAR_ETC = 80 /* item navi */ + 24 /* shop */ +
        29 /* grid navi */ + 20 /* margin */;
    protected static final int BLURB_HEIGHT = 33 /* title */ + 71 /* contents */;
    protected static final int BOX_HEIGHT = 104;
    protected static final int ACTIVATOR_HEIGHT = 22;
}
