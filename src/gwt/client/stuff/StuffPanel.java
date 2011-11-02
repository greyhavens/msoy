//
// $Id$

package client.stuff;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.IdentGameItem;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.web.gwt.Pages;

import client.item.SideBar;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.SearchBox;
import client.ui.StretchButton;
import client.util.InfoCallback;
import client.util.Link;
import client.util.NaviUtil;

/**
 * Displays all items of a particular type in a player's inventory, or display the main inventory
 * page with a list of recent items of all types.
 */
public class StuffPanel extends FlowPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 4;

	public StuffPanel (InventoryModels models, int memberId, MsoyItemType type)
    {
        setStyleName("itemPanel");

        _models = models;
        _memberId = memberId;
        _type = type;
        boolean displayUpload = shouldDisplayUpload(type);

        // prepare the search box
        _search = new HorizontalPanel();
        _search.setStyleName("Search");
        _search.setSpacing(5);
        _search.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
        _search.add(MsoyUI.createLabel(_msgs.stuffSearch(), "SearchTitle"));
        final ListBox searchTypes = new ListBox();
        for (MsoyItemType searchType : MsoyItemType.STUFF_ITEMS) {
            searchTypes.addItem(_dmsgs.xlateItemsType(searchType), searchType.toByte() + "");
            if (searchType == type) {
                searchTypes.setSelectedIndex(searchTypes.getItemCount() - 1);
            }
        }
        _search.add(searchTypes);
        _searchBox = new SearchBox(new SearchBox.Listener() {
            public void search (String query) {
                String type = searchTypes.getValue(searchTypes.getSelectedIndex());
                Link.go(Pages.STUFF, type, _memberId, 0, query);
            }
            public void clearSearch () {
                Link.go(Pages.STUFF, _type, _memberId, 0);
            }
        });
        _search.add(_searchBox);
        _search.add(MsoyUI.createImageButton("GoButton", _searchBox.makeSearchListener()));

        // a drop down for setting filters
        _filters = new ListBox();
        for (String element2 : FLABELS) {
            _filters.addItem(element2);
        }
        _filters.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                showInventory(_mostRecentPage, null);
            }
        });

        // compute the number of rows of items we can fit on the page
        int used = displayUpload ? NAVIGATION_HEIGHT + GET_STUFF_HEIGHT : NAVIGATION_HEIGHT;
        int rows = MsoyUI.computeRows(used, ITEM_BOX_HEIGHT, 2);

        // now create our grid of items
        _contents = new PagedGrid<Item>(rows, COLUMNS) {
            @Override protected void displayPageFromClick (int page) {
                // route our page navigation through the URL
                Link.go(Pages.STUFF, ((InventoryModels.Stuff)_model).makeArgs(_memberId, page));
            }
            @Override protected Widget createWidget (Item item) {
                return new ItemEntry(item, !(item instanceof IdentGameItem));

            }
            @Override protected String getEmptyMessage () {
                if (_model instanceof InventoryModels.Stuff) {
                    String query = ((InventoryModels.Stuff)_model).query;
                    if (query != null) {
                        return _msgs.panelNoMatches(query);
                    }
                }
                return _msgs.panelNoItems(_dmsgs.xlateItemType(_type));
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
        if (displayUpload) {
            createUploadInterface();
        }
    }

    public int getMemberId ()
    {
        return _memberId;
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
        showInventory(page, query);
    }

    protected boolean shouldDisplayUpload (MsoyItemType type)
    {
        // for now, if you're in a theme, there is no uploading of stuff
        if (CShell.frame.getThemeId() != 0 && !type.isUsableAnywhere()) {
            // TODO: we should have Buy but not Upload, punt!
            return false;
        }
        if (type == MsoyItemType.LAUNCHER) {
            // TODO: we should have Buy but not Upload, punt!
            return false;
        }
        return type.isShopType();
    }

    protected void createUploadInterface ()
    {
        _upload = new AbsolutePanel();
        _upload.setStyleName("GetStuff");
        _upload.add(MsoyUI.createLabel(_msgs.getStuffTitle(), "GetStuffTitle"), 60, 10);
        _upload.add(MsoyUI.createHTML(_dmsgs.xlateGetStuffBuy(_type), "GetStuffBuy"), 165,
            85);
        _upload.add(MsoyUI.createHTML(_dmsgs.xlateGetStuffCreate(_type), "GetStuffCreate"),
            360, 85);
        _upload.add(new StretchButton(StretchButton.BLUE_THICK, _msgs.getStuffShop(),
            Link.createHandler(Pages.SHOP, _type + "")), 10, 90);
        _upload.add(MsoyUI.createButton(MsoyUI.MEDIUM_THICK, _msgs.getStuffUpload(),
            NaviUtil.onCreateItem(_type, 0)), 535, 90);
    }

    /**
     * Requests that the current inventory page be displayed (clearing out any currently displayed
     * item detail view).
     *
     * @param query If non-null, this query is being applied so data must be reloaded from db
     */
    protected void showInventory (final int page, final String query)
    {
        // don't fiddle with things if the inventory is already showing
        if (!_contents.isAttached()) {
            clear();
            String title = (_type == MsoyItemType.NOT_A_TYPE) ? _msgs.stuffTitleMain() :
                (_memberId == CShell.getMemberId()) ?
                    _msgs.stuffTitle(_dmsgs.xlateItemsType(_type)) :
                        _msgs.stuffTitlePlayer(_dmsgs.xlateItemsType(_type));

            add(MsoyUI.createLabel(title, "TypeTitle"));
            add(_search);
            HorizontalPanel row = new HorizontalPanel();
            row.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
            MsoyItemType[] items = CShell.getClientMode().isMinimal() ?
                MsoyItemType.DJ_ITEMS : MsoyItemType.STUFF_ITEMS;
            row.add(new SideBar(new SideBar.Linker() {
                public boolean isSelected (MsoyItemType itemType) {
                    return itemType == _type;
                }
                public Widget createLink (String name, MsoyItemType itemType) {
                    return Link.create(name, Pages.STUFF, itemType, _memberId);
                }
            }, items, null));

            row.add(_contents);
            add(row);
            if (_upload != null) {
                add(_upload);
            }
        }

        // determine which filter predicate we should be using
        final Predicate<Item> pred = FILTERS.get(_filters.getSelectedIndex());

        // maybe we're changing our predicate or changing page on an already loaded model
        SimpleDataModel<Item> model = _models.getModel(_memberId, _type, query, 0);
        if (model != null) {
            _contents.setModel(model.filter(pred), page);
            return;
        }

        // otherwise we have to load
        _models.loadModel(_memberId, _type, query, 0, new InfoCallback<DataModel<Item>>() {
            public void onSuccess (DataModel<Item> result) {
                SimpleDataModel<Item> model = (SimpleDataModel<Item>)result;
                _contents.setModel(model.filter(pred), page);
            }
        });
    }

    protected InventoryModels _models;
    protected int _memberId;
    protected MsoyItemType _type;
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

    protected static final List<Predicate<Item>> FILTERS = Lists.newArrayList(); {
        FILTERS.add(new Predicate<Item>() { // show all (TODO: use Predicates.alwaysTrue()
            public boolean apply (Item item) {
                return true;
            }
        });
        FILTERS.add(new Predicate<Item>() { // uploaded
            public boolean apply (Item item) {
                return item.sourceId == 0;
            }
        });
        FILTERS.add(new Predicate<Item>() { // purchased
            public boolean apply (Item item) {
                return item.sourceId != 0;
            }
        });
        FILTERS.add(new Predicate<Item>() { // unused
            public boolean apply (Item item) {
                return !item.isUsed();
            }
        });
        FILTERS.add(new Predicate<Item>() { // used
            public boolean apply (Item item) {
                return item.isUsed();
            }
        });
    }

    /** Height of page above items. Main top navigation is outside of iframe so not counted. */
    protected static final int NAVIGATION_HEIGHT = 50 /* search */ + 50 /* grid top */;
    protected static final int ITEM_BOX_HEIGHT = 117;
    protected static final int GET_STUFF_HEIGHT = 160;
}
