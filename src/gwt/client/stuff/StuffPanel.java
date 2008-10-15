//
// $Id$

package client.stuff;

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;

import client.item.SideBar;
import client.shell.Args;
import client.shell.DynamicLookup;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.SearchBox;
import client.ui.StretchButton;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.NaviUtil;

/**
 * Displays all items of a particular type in a player's inventory, or display the main inventory
 * page with a list of recent items of all types.
 */
public class StuffPanel extends FlowPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 4;

    public StuffPanel (InventoryModels models, byte type)
    {
        setStyleName("itemPanel");

        _models = models;
        _type = type;
        boolean isCatalogType = isCatalogItem(type);

        // prepare the search box
        _search = new HorizontalPanel();
        _search.setStyleName("Search");
        _search.setSpacing(5);
        _search.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        _search.add(MsoyUI.createLabel("Search", "SearchTitle"));
        final ListBox searchTypes = new ListBox();
        for (byte searchType : Item.STUFF_TYPES) {
            searchTypes.addItem(_dmsgs.xlate("pItemType" + searchType), searchType + "");
            if (searchType == type) {
                searchTypes.setSelectedIndex(searchTypes.getItemCount() - 1);
            }
        }
        _search.add(searchTypes);
        _searchBox = new SearchBox(new SearchBox.Listener() {
            public void search (String query) {
                String type = searchTypes.getValue(searchTypes.getSelectedIndex());
                Link.go(Pages.STUFF, Args.compose(type, 0, query));
            }
            public void clearSearch () {
                Link.go(Pages.STUFF, Args.compose(_type, 0));
            }
        });
        _search.add(_searchBox);
        _search.add(MsoyUI.createImageButton("GoButton", _searchBox.makeSearchListener()));

        // a drop down for setting filters
        _filters = new ListBox();
        for (String element2 : FLABELS) {
            _filters.addItem(element2);
        }
        _filters.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                showInventory(0, FILTERS.get(_filters.getSelectedIndex()), null);
            }
        });

        // compute the number of rows of items we can fit on the page
        int used = isCatalogType ? NAVIGATION_HEIGHT + GET_STUFF_HEIGHT : NAVIGATION_HEIGHT;
        int rows = Math.max(2, (Window.getClientHeight() - used) / ITEM_BOX_HEIGHT);

        // now create our grid of items
        _contents = new PagedGrid<Item>(rows, COLUMNS) {
            @Override protected void displayPageFromClick (int page) {
                // route our page navigation through the URL
                Link.go(Pages.STUFF, ((InventoryModels.Stuff)_model).makeArgs(page));
            }
            @Override protected Widget createWidget (Item item) {
                return new ItemEntry(item);
            }
            @Override protected String getEmptyMessage () {
                String query = _model instanceof InventoryModels.Stuff
                    ? ((InventoryModels.Stuff)_model).query : null;
                return (query == null) ? _msgs.panelNoItems(_dmsgs.xlate("itemType" + _type)) :
                    _msgs.panelNoMatches(query);
            }
            @Override protected boolean displayNavi (int items) {
                return true;
            }
            @Override protected void addCustomControls (FlexTable controls) {
                controls.setText(0, 0, _msgs.ipfTitle());
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
     * Requests that the specified page of inventory items be displayed and that the specified
     * query be used when fetching contents. Both are optional.
     */
    public void setArgs (int page, String query)
    {
        // if we're asked to display the "default" page, display the last page we remember
        if (page < 0) {
            page = _mostRecentPage;
        }
        _mostRecentPage = page; // now remember this page

        // update our search box
        _searchBox.setText(query);

        // make sure we're showing and have our data
        showInventory(page, new Predicate.TRUE<Item>(), query);
    }

    protected boolean isCatalogItem (byte type)
    {
        for (byte element2 : Item.SHOP_TYPES) {
            if (type == element2) {
                return true;
            }
        }
        return false;
    }

    protected void createUploadInterface ()
    {
        _upload = new AbsolutePanel();
        _upload.setStyleName("GetStuff");
        _upload.add(MsoyUI.createLabel(_msgs.getStuffTitle(), "GetStuffTitle"), 60, 10);
        _upload.add(MsoyUI.createHTML(_dmsgs.xlate("getStuffBuy" + _type), "GetStuffBuy"), 165,
            85);
        _upload.add(MsoyUI.createHTML(_dmsgs.xlate("getStuffCreate" + _type), "GetStuffCreate"),
            360, 85);
        _upload.add(new StretchButton(StretchButton.BLUE_THICK, _msgs.getStuffShop(),
            Link.createListener(Pages.SHOP, _type + "")), 10, 90);
        _upload.add(MsoyUI.createButton(MsoyUI.MEDIUM_THICK, _msgs.getStuffUpload(),
            NaviUtil.onCreateItem(_type, (byte)0, 0)), 535, 90);
    }

    /**
     * Requests that the current inventory page be displayed (clearing out any currently displayed
     * item detail view).
     * @pred a method of filtering the data. If non-null, will be applied to the list of items
     * @param query If non-null, this query is being applied so data must be reloaded from db
     */
    protected void showInventory (final int page, final Predicate<Item> pred, final String query)
    {
        // don't fiddle with things if the inventory is already showing
        if (!_contents.isAttached()) {
            clear();
            String title = (_type == Item.NOT_A_TYPE) ? _msgs.stuffTitleMain() :
                _msgs.stuffTitle(_dmsgs.xlate("pItemType" + _type));
            add(MsoyUI.createLabel(title, "TypeTitle"));
            add(_search);
            HorizontalPanel row = new HorizontalPanel();
            row.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
            row.add(new SideBar(new SideBar.Linker() {
                public boolean isSelected (byte itemType) {
                    return itemType == _type;
                }
                public Widget createLink (String name, byte itemType) {
                    return Link.create(name, Pages.STUFF, ""+itemType);
                }
            }, Item.STUFF_TYPES, null));
            row.add(_contents);
            add(row);
            if (_upload != null) {
                add(_upload);
            }
        }

        // maybe we're changing our predicate or changing page on an already loaded model
        SimpleDataModel<Item> model = _models.getModel(_type, query);
        if (model != null) {
            _contents.setModel(model.filter(pred), page);
            return;
        }

        // otherwise we have to load
        _models.loadModel(_type, query, new MsoyCallback<DataModel<Item>>() {
            public void onSuccess (DataModel<Item> result) {
                SimpleDataModel<Item> model = (SimpleDataModel<Item>)result;
                _contents.setModel(model.filter(pred), page);
            }
        });
    }

    protected InventoryModels _models;
    protected byte _type;
    protected int _mostRecentPage;

    protected HorizontalPanel _search;
    protected SearchBox _searchBox;
    protected ListBox _filters;
    protected PagedGrid<Item> _contents;
    protected AbsolutePanel _upload;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);

    protected static final String[] FLABELS = {
        _msgs.ipfAll(),
        _msgs.ipfUploaded(),
        _msgs.ipfPurchased(),
        _msgs.ipfUnused(),
        _msgs.ipfUsed()
    };

    protected static final List<Predicate<Item>> FILTERS = new ArrayList<Predicate<Item>>(); {
        FILTERS.add(new Predicate.TRUE<Item>()); // show all
        FILTERS.add(new Predicate<Item>() { // uploaded
            public boolean isMatch (Item item) {
                return item.sourceId == 0;
            }
        });
        FILTERS.add(new Predicate<Item>() { // purchased
            public boolean isMatch (Item item) {
                return item.sourceId != 0;
            }
        });
        FILTERS.add(new Predicate<Item>() { // unused
            public boolean isMatch (Item item) {
                return !item.isUsed();
            }
        });
        FILTERS.add(new Predicate<Item>() { // used
            public boolean isMatch (Item item) {
                return item.isUsed();
            }
        });
    }

    /** Height of page above items. Main top navigation is outside of iframe so not counted. */
    protected static final int NAVIGATION_HEIGHT = 50 /* search */ + 50 /* grid top */;
    protected static final int ITEM_BOX_HEIGHT = 120;
    protected static final int GET_STUFF_HEIGHT = 160;
}
