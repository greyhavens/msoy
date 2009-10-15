//
// $Id: $

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.stuff.gwt.StuffService.InventoryResult;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.InfoCallback;

/**
 * Displays the avatars in a theme's lineup.
 */
public class ThemeLineupPanel extends FlowPanel
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 4;

    public ThemeLineupPanel (int groupId)
    {
        setStyleName("lineupPanel");

        _groupId = groupId;

        // compute the number of rows of items we can fit on the page
        int used = NAVIGATION_HEIGHT;
        int rows = MsoyUI.computeRows(used, ITEM_BOX_HEIGHT, 2);

        // now create our grid of items
        _contents = new PagedGrid<Avatar>(rows, COLUMNS) {
            @Override protected void displayPageFromClick (int page) {
                // route our page navigation through the URL
                Link.go(Pages.STUFF, "t",  _groupId, page);
            }
            @Override protected Widget createWidget (Avatar item) {
                return new ItemEntry(item, false);
            }
            @Override protected String getEmptyMessage () {
                return _msgs.lineupNoLineup(_theme.toString());
            }
            @Override protected boolean displayNavi (int items) {
                return true;
            }
        };
        _contents.addStyleName("Contents");
    }

    /**
     * Requests that the (optionally) specified page of items be displayed.
     */
    public void setArgs (int page)
    {
        showInventory(page);
    }

    /**
     * Requests that the given page be displayed.
     */
    protected void showInventory (final int page)
    {
        if (_contents.getModel() != null) {
            _contents.displayPage(page, false);
            return;
        }
        _stuffsvc.loadThemeLineup(_groupId, new InfoCallback<InventoryResult<Avatar>>() {
            public void onSuccess (InventoryResult<Avatar> result) {
                _theme = result.theme;
                SimpleDataModel<Avatar> model = new SimpleDataModel<Avatar>(result.items);
                _contents.setModel(model, page);

                clear();
                String title = _msgs.lineupTitle();
                add(MsoyUI.createLabel(title, "TypeTitle"));
                add(MsoyUI.createHTML(_msgs.lineupIntro(_theme.toString()), "Intro"));
                add(_contents);
            }
        });
    }

    protected int _groupId;
    protected int _mostRecentPage;
    protected PagedGrid<Avatar> _contents;
    protected GroupName _theme;

    protected static final StuffServiceAsync _stuffsvc = GWT.create(StuffService.class);

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);

    /** Height of page above items. Main top navigation is outside of iframe so not counted. */
    protected static final int NAVIGATION_HEIGHT = 50 /* search */ + 50 /* grid top */;
    protected static final int ITEM_BOX_HEIGHT = 120;
    protected static final int GET_STUFF_HEIGHT = 160;
}
