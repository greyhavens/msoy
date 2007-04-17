//
// $Id$

package client.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Application;

/**
 * Displays an item type selection tab bar. The tabs are designed to work with the history system,
 * meaning they use {@link Hyperlink} to switch the browser's URL and expect the containing
 * application to turn around and call {@link #selectTab} with the newly arrived history token.
 */
public class ItemTypePanel extends FlexTable
    implements SourcesTabEvents
{
    public ItemTypePanel (String page, TabListener listener)
    {
        setStyleName("itemTypePanel");
        setCellPadding(0);
        setCellSpacing(0);
        _page = page;
        _selectedType = -1;
        addTabListener(listener);
        redrawPanel();
    }

    /**
     * Select a new tab, associated with the given item type. This method is run
     * when a tab is clicked, or it can be called programmatically.
     */
    public boolean selectTab (byte itemType)
    {
        if (_selectedType == itemType) {
            return false;
        }

        Iterator i = _listeners.iterator();
        while (i.hasNext()) {
            TabListener listener = ((TabListener) i.next());
            if (!listener.onBeforeTabSelected(ItemTypePanel.this, itemType)) {
                // a listener forbade the selection
                return false;
            }
        }
        _selectedType = itemType;
        i = _listeners.iterator();
        while (i.hasNext()) {
            ((TabListener) i.next()).onTabSelected(ItemTypePanel.this, itemType);
        }
        redrawPanel();
        return true;
    }

    // from SourcesTabEvents
    public void addTabListener (TabListener listener)
    {
        _listeners.add(listener);
    }

    // from SourcesTabEvents
    public void removeTabListener (TabListener listener)
    {
        _listeners.remove(listener);
    }

    protected void redrawPanel ()
    {
        setVisible(false);
        clear();
        _column = 0;
        _rightBit = null;
        addTab(Item.AVATAR);
        addTab(Item.FURNITURE);
        addTab(Item.DECOR);
        addTab(Item.PET);
        addTab(Item.GAME);
        addTab(Item.PHOTO);
        addTab(Item.AUDIO);
        // addTab(Item.VIDEO);
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

    /** A list of objects that want to know about our tab events. */
    protected List _listeners = new ArrayList();

    /** The previous tab's right-hand bit, deferred so selected tabs can eat them. */
    protected String _rightBit;

    /** An index into the table we're using for layout. */
    protected int _column;

    /** The item type corresponding to the currently selected tab. */
    protected byte _selectedType;
}
