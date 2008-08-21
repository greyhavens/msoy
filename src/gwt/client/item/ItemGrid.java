//
// $Id$

package client.item;

import java.util.ArrayList;
import java.util.List;

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.util.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;
import com.threerings.msoy.item.data.all.Item;

/**
 * A reusable item grid.
 *
 * @author mjensen
 */
public abstract class ItemGrid extends PagedGrid<Item>
{
    public ItemGrid (Pages parentPage, int rows, int columns)
    {
        super(rows, columns);
        _parentPage = parentPage;

        addStyleName("Contents");
    }

    /**
     * Sets the optional command arguments to passed to the page. This is particularly useful when
     * this is not the only grid on the page.
     */
    public void setPrefixArgs (String[] prefixArgs)
    {
        _prefixArgs = prefixArgs;
    }

    /**
     * Gets the item type being displayed in this grid.
     */
    public byte getItemType ()
    {
        if (_listDataModel != null) {
            return _listDataModel.getItemType();
        }

        return Item.NOT_A_TYPE;
    }

    public void setModel (DataModel<Item> model, int page)
    {
        super.setModel(model, page);

        if (model instanceof ItemListDataModel) {
            _listDataModel = (ItemListDataModel) model;
        }

        updateTitle();
    }

    /**
     * Updates the arguments passed to the parent page so that the given grid page is displayed.
     */
    protected void displayPageFromClick (int page)
    {
        // route our page navigation through the URL
        List<String> args = new ArrayList<String>();
        if (_prefixArgs != null) {
            for (String arg : _prefixArgs) {
                args.add(arg);
            }
        }
        args.add(String.valueOf(getItemType()));
        args.add(String.valueOf(page));

        Link.go(_parentPage, Args.compose(args));
    }

    public void setDisplayNavigation (boolean displayControls)
    {
        _displayNavigation = displayControls;
    }

    /**
     * Gets the custom title to display for this grid.
     */
    public abstract String getTitle ();

    @Override
    protected boolean displayNavi (int itemCount)
    {
        return _displayNavigation;
    }

    protected void addCustomControls (FlexTable controls)
    {
        _titleLabel = new Label(getTitle());
        controls.setWidget(0, 0, _titleLabel);
        controls.getFlexCellFormatter().setStyleName(0, 0, "Show");
    }

    protected void updateTitle ()
    {
        _titleLabel.setText(getTitle());
    }

    /**
     * Returns the message to display when the grid is empty.
     */
    protected abstract String getEmptyMessage ();

    protected Pages _parentPage;

    protected String[] _prefixArgs;

    protected boolean _displayNavigation = true;

    protected ItemListDataModel _listDataModel;

    protected Label _titleLabel;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
