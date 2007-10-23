//
// $Id$

package client.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Hyperlink;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Application;

/**
 * Displays an item type selection tab bar. The tabs are designed to work with the history system,
 * meaning they use {@link Hyperlink} to switch the browser's URL and expect the containing
 * application to turn around and call {@link #selectTab} with the newly arrived history token.
 */
public class ItemTypePanel extends FlexTable
{
    public ItemTypePanel (String page)
    {
        setStyleName("itemTypePanel");
        setCellPadding(0);
        setCellSpacing(0);
        _page = page;
        _selectedType = -1;
        redrawPanel();
    }

    /**
     * Select a new tab, associated with the given item type. This method is run when a tab is
     * clicked, or it can be called programmatically.
     *
     * @return false if the sepecified tab was already selected, true if the selection changed.
     */
    public boolean selectTab (byte itemType)
    {
        if (_selectedType == itemType) {
            return false;
        }
        _selectedType = itemType;
        redrawPanel();
        return true;
    }

    protected void redrawPanel ()
    {
        setVisible(false);
        clear();
        _column = 0;
        _rightBit = null;
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            addTab(Item.TYPES[ii]);
        }
        getFlexCellFormatter().setStyleName(0, _column++, _rightBit);
        setVisible(true);
    }

    protected void addTab (byte itemType)
    {
        String prefix = "";
        if (itemType == _selectedType) {
            // the selected tab has a left border, and ignores _rightBit
            getFlexCellFormatter().setStyleName(0, _column++, "SelLeft");
            prefix = "Sel";
        } else {
            if (_column == 0) {
                getFlexCellFormatter().setStyleName(0, _column++, "Left");
            } else {
                getFlexCellFormatter().setStyleName(0, _column++, _rightBit);
            }
        }
        String name = CItem.dmsgs.getString("pItemType" + itemType);
        Hyperlink button = Application.createLink(name, _page, ""+itemType);
        button.setStyleName("Button");
        getFlexCellFormatter().setStyleName(0, _column, prefix + "Tab");
        setWidget(0, _column++, button);
        _rightBit = prefix + "Right";
    }

    /** The page on which we're operating. */
    protected String _page;

    /** The previous tab's right-hand bit, deferred so selected tabs can eat them. */
    protected String _rightBit;

    /** An index into the table we're using for layout. */
    protected int _column;

    /** The item type corresponding to the currently selected tab. */
    protected byte _selectedType;
}
