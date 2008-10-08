//
// $Id$

package client.stuff;

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;

import client.item.StuffNaviBar;
import client.shell.Args;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.SearchBox;
import client.util.FlashClients;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.NaviUtil;

/**
 * Displays all items of a particular type in a player's inventory, or display the main inventory
 * page with a list of recent items of all types.
 */
public class ItemPanel extends FlowPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 5;

    public ItemPanel (InventoryModels models, byte type)
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
        for (byte searchType : Item.TYPES) {
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
        int used = NAV_BAR_ETC;
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
        for (byte element2 : Item.TYPES) {
            if (type == element2) {
                return true;
            }
        }
        return false;
    }

    protected void createUploadInterface ()
    {
        // this will allow us to create new items
        _upload = new SmartTable("Upload", 0, 0);
        _upload.setText(0, 0, _dmsgs.xlate("itemUploadTitle" + _type), 2, "Header");

        // add the various "why to upload" pitches
        String why = getPitch("a") + "<br>" + getPitch("b") + "<br>" + getPitch("c");
        _upload.setWidget(1, 0, MsoyUI.createHTML(why, null));
        _upload.getFlexCellFormatter().setStyleName(1, 0, "Pitch");

        // add the create button
        _upload.setWidget(1, 1, new Button(_msgs.panelCreateNew(),
                                           NaviUtil.onCreateItem(_type, (byte)0, 0)), 1, "Button");
        _upload.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_RIGHT);
    }

    protected String getPitch (String postfix)
    {
        String pitch = _dmsgs.xlate("itemUploadPitch" + _type + postfix);
        if (-1 != pitch.indexOf("@MEMBER_ID@")) {
            return pitch.replaceAll("@MEMBER_ID@", "" + CShell.getMemberId());
        }
        return pitch;
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
            add(new StuffNaviBar(_type));
            add(_contents);
            if (_upload != null) {
                add(_upload);
            }
        }

        // maybe we're changing our predicate or changing page on an already loaded model
        SimpleDataModel<Item> model = _models.getModel(_type, 0, query);
        if (model != null) {
            _contents.setModel(model.filter(pred), page);
            return;
        }

        // otherwise we have to load
        _models.loadModel(_type, 0, query, new MsoyCallback<DataModel<Item>>() {
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
    protected SmartTable _upload;

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

    protected static final int NAV_BAR_ETC = 80 /* item navi */ + 24 /* shop */ +
        29 /* grid navi */
        + 20 /* margin */+ 50;
    protected static final int BLURB_HEIGHT = 33 /* title */ + 71 /* contents */;
    protected static final int BOX_HEIGHT = 104;
    protected static final int ACTIVATOR_HEIGHT = 22;
}
