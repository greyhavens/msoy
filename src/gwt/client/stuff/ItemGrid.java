package client.stuff;

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
    public ItemGrid (Pages parentPage, String pageArg, byte itemType, int rows, int columns, String emptyMessage)
    {
        super(rows, columns);
        this._parentPage = parentPage;
        this._firstArg = pageArg;
        this._itemType = itemType;
        this._emptyMessage = emptyMessage;
    }

    protected void displayPageFromClick (int page)
    {
        // route our page navigation through the URL
        String[] args;
        if (_firstArg != null) {
            args = new String[] { _firstArg, String.valueOf(_itemType), String.valueOf(page) };
        } else {
            args = new String[] { String.valueOf(_itemType), String.valueOf(page) };
        }
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

    protected boolean displayNavi (int items)
    {
        return true;
    }

    protected void addCustomControls (FlexTable controls)
    {
        controls.setText(0, 0, CStuff.msgs.favorites());
        controls.getFlexCellFormatter().setStyleName(0, 0, "Show");
    }

    protected byte _itemType = Item.NOT_A_TYPE;

    protected Pages _parentPage;

    protected String _firstArg;

    protected String _emptyMessage;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
