//
// $Id$

package client.stuff;

import java.util.ArrayList;
import java.util.List;

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.util.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.msoy.item.data.all.Item;

/**
 * A reusable item grid.
 *
 * @author mjensen
 */
public class ItemGrid extends PagedGrid<Item>
{
    public ItemGrid (Pages parentPage, byte itemType, int rows, int columns, String title, String emptyMessage)
    {
        super(rows, columns);
        _parentPage = parentPage;
        _itemType = itemType;
        _emptyMessage = emptyMessage;
        _title = title;

        addStyleName("Contents");
    }

    /**
     * Sets the optional command arguments to passed to the page. This is particularly useful when this
     * is not the only grid on the page.
     */
    public void setPrefixArgs (String[] prefixArgs)
    {
        _prefixArgs = prefixArgs;
    }

    public void setItemType (byte itemType)
    {
        _itemType = itemType;
    }

    protected void displayPageFromClick (int page)
    {
        // route our page navigation through the URL
        List<String> args = new ArrayList<String>();

        if (_prefixArgs != null) {
            for (String arg : _prefixArgs) {
                args.add(arg);
            }
        }

        args.add(String.valueOf(_itemType));
        args.add(String.valueOf(page));

        Link.go(_parentPage, Args.compose(args));
    }

    protected Widget createWidget (Item item)
    {
        return new ItemEntry(item);
    }

    protected String getEmptyMessage ()
    {
        return _emptyMessage;
    }

    @Override
    protected boolean displayNavi (int itemCount)
    {
        return _displayNavigation;
    }

    public void setDisplayNavigation (boolean displayControls)
    {
        _displayNavigation = displayControls;
    }

    protected void addCustomControls (FlexTable controls)
    {
        controls.setText(0, 0, _title);
        controls.getFlexCellFormatter().setStyleName(0, 0, "Show");
    }

    protected byte _itemType = Item.NOT_A_TYPE;

    protected Pages _parentPage;

    protected String[] _prefixArgs;

    protected String _title;

    protected String _emptyMessage;

    protected boolean _displayNavigation = true;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
