//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.item.data.Item;

/**
 * The base class for an interface for creating and editing digital items.
 *
 * <p> Styles:
 * <ul>
 * <li> item_editor - the style of the main editor
 * <li> item_editor_title - the style of the title label
 * <li> item_editor_submit - the style of the submit button
 * </ul>
 */
public abstract class ItemEditor extends DockPanel
{
    public ItemEditor ()
    {
        setStyleName("item_editor");
        setSpacing(5);
        add(_etitle = new Label("title"), NORTH);
        _etitle.setStyleName("item_editor_title");
        add(_esubmit = new Button("submit"), SOUTH);
        setCellHorizontalAlignment(_esubmit, ALIGN_RIGHT);
        _esubmit.setStyleName("item_editor_submit");
        _esubmit.setEnabled(false);
    }

    /**
     * Configures this editor with an item to edit. The item may be freshly
     * constructed if we are using the editor to create a new item.
     */
    public void setItem (Item item)
    {
        _item = item;
        _etitle.setText((item.itemId <= 0) ? "Create" : "Edit");
        _esubmit.setText((item.itemId <= 0) ? "Create" : "Update");
        updateSubmittable();
    }

    /**
     * Returns the currently configured item.
     */
    public Item getItem ()
    {
        return _item;
    }

    /**
     * Editors should override this method to indicate when the item is in a
     * consistent state and may be uploaded.
     */
    protected boolean itemConsistent ()
    {
        return false;
    }

    /**
     * Editors should call this method when something changes that might render
     * an item consistent or inconsistent. It will update the enabled status of
     * the submit button.
     */
    protected void updateSubmittable ()
    {
        _esubmit.setEnabled(itemConsistent());
    }

    /** Displays the title of this editor. */
    protected Label _etitle;

    /** The button that submits the item for upload. */
    protected Button _esubmit;

    /** The item we are editing. */
    protected Item _item;
}
