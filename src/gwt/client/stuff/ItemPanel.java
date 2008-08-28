//
// $Id$

package client.stuff;

import java.util.List;
import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.Predicate;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.MsoyUI;
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

        _search = new HorizontalPanel();
        _search.setStyleName("Search");
        _search.setSpacing(5);
        _search.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        _search.add(MsoyUI.createLabel("Search", "SearchTitle"));
        final ListBox searchTypes = new ListBox();
        for (byte searchType : Item.TYPES) {
            searchTypes.addItem(_dmsgs.getString("pItemType" + searchType), searchType + "");
        }
        _search.add(searchTypes);
        final TextBox searchBox = new TextBox();
        searchBox.setVisibleLength(20);
        _search.add(searchBox);
        // TODO show query: if (query != null) { searchBox.setText(query); }

        // TODO get from somewhere.. _filters?
        final byte sortMethod = 0;

        ClickListener searchListener = new ClickListener() {
            public void onClick (Widget sender)
            {
                String newQuery = searchBox.getText().trim();
                Link.go(Pages.STUFF, Args.compose(new String[] {
                    searchTypes.getValue(searchTypes.getSelectedIndex()), sortMethod + "",
                    newQuery }));
            }
        };
        searchBox.addKeyboardListener(new EnterClickAdapter(searchListener));
        _search.add(MsoyUI.createImageButton("GoButton", searchListener));

        // a drop down for setting filters
        _filters = new ListBox();
        for (String element2 : FLABELS) {
            _filters.addItem(element2);
        }
        _filters.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                showInventory(0, FILTERS.get(_filters.getSelectedIndex()));
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
                Link.go(Pages.STUFF, Args.compose(new String[] { ""+_type, ""+page }));
            }
            @Override protected Widget createWidget (Item item) {
                return new ItemEntry(item);
            }
            @Override protected String getEmptyMessage () {
                return CStuff.msgs.panelNoItems(_dmsgs.getString("itemType" + _type));
            }
            @Override protected boolean displayNavi (int items) {
                return true;
            }
            @Override protected void addCustomControls (FlexTable controls) {
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

        // make sure we're showing and have our data
        showInventory(page, null);
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
        _upload.setText(0, 0, _dmsgs.getString("itemUploadTitle" + _type), 2, "Header");

        // add the various "why to upload" pitches
        String why = getPitch("a") + "<br>" + getPitch("b") + "<br>" + getPitch("c");
        _upload.setWidget(1, 0, MsoyUI.createHTML(why, null));
        _upload.getFlexCellFormatter().setStyleName(1, 0, "Pitch");

        // add the create button
        _upload.setWidget(1, 1, new Button(CStuff.msgs.panelCreateNew(), new ClickListener() {
            public void onClick (Widget widget) {
                NaviUtil.createItem(_type, (byte)0, 0);
            }
        }), 1, "Button");
        _upload.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_RIGHT);
    }

    protected String getPitch (String postfix)
    {
        String pitch = _dmsgs.getString("itemUploadPitch" + _type + postfix);
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
            String title = _type == Item.NOT_A_TYPE ? CStuff.msgs.stuffTitleMain()
                : CStuff.msgs.stuffTitle(_dmsgs.getString("pItemType" + _type));
            add(MsoyUI.createLabel(title, "TypeTitle"));

            // TODO: this has been styled but the functionality not yet implemented
            // add(_search);

            add(new StuffNaviBar(_type));

            // TODO: takes up too much room - display only on main stuff page?
            // Image shopImage = MsoyUI.createActionImage("/images/stuff/shop.png",
            // Link.createListener(Pages.SHOP, ""));
            // shopImage.addStyleName("Shop");
            // add(shopImage);

            // TODO replace this old image with the above shop image
            HorizontalPanel shop = new HorizontalPanel();
            shop.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
            shop.add(MsoyUI.createLabel(CStuff.msgs.ipShopFor(), null));
            shop.add(WidgetUtil.makeShim(5, 5));
            ClickListener onClick = new ClickListener() {
                public void onClick (Widget sender)
                {
                    Link.go(Pages.SHOP, "" + _type);
                }
            };
            shop.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, CStuff.msgs.ipToCatalog(), onClick));
            shop.add(WidgetUtil.makeShim(10, 10));
            add(MsoyUI.createSimplePanel("Shop", shop));

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
        _models.loadModel(_type, 0, new MsoyCallback<DataModel<Item>>() {
            public void onSuccess (DataModel<Item> model) {
                if (pred != null) {
                    model = ((SimpleDataModel<Item>) model).filter(pred);
                }
                _contents.setModel(model, page);
            }
        });
    }

    protected InventoryModels _models;
    protected byte _type;
    protected int _mostRecentPage;
    protected ListBox _filters;
    protected PagedGrid<Item> _contents;
    protected SmartTable _upload;
    protected HorizontalPanel _search;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);

    protected static final String[] FLABELS = {
        CStuff.msgs.ipfAll(),
        CStuff.msgs.ipfUploaded(),
        CStuff.msgs.ipfPurchased(),
        CStuff.msgs.ipfUnused(),
        CStuff.msgs.ipfUsed()
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
        + 20 /* margin */+ 50 /* TODO calculate this correctly */;
    protected static final int BLURB_HEIGHT = 33 /* title */ + 71 /* contents */;
    protected static final int BOX_HEIGHT = 104;
    protected static final int ACTIVATOR_HEIGHT = 22;
}
