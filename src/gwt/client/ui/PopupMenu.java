//
// $Id$

package client.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a popup menu on the supplied trigger label.
 */
public abstract class PopupMenu extends PopupPanel
{
    public PopupMenu ()
    {
        super(true);
    }

    public PopupMenu (Label trigger)
    {
        super(true);

        setTrigger(trigger);
    }

    /**
     * Set the label that will trigger the popping up of this menu
     */
    public void setTrigger (Label trigger)
    {
        trigger.addStyleName("LabelLink");
        trigger.addMouseListener(new MouseListenerAdapter() {
            public void onMouseDown (Widget sender, int x, int y) {
                clear();
                _menu = new MenuBar(true);
                addMenuItems();
                add(_menu);
                setPopupPosition(sender.getAbsoluteLeft() + x, sender.getAbsoluteTop() + y);
                show();
            }
        });
    }

    /**
     * Adds a menu item to this popup menu. This must only be called from within {@link
     * #addMenuItems} but is public because the {@link PopupMenu} itself may be passed to code that
     * adds menu items to it, triggered by the call to {@link #addMenuItems}.
     */
    public void addMenuItem (String label, final Command command)
    {
        _menu.addItem(new MenuItem(label, new Command() {
            public void execute () {
                hide(); // hide this popup menu
                command.execute();
            }
        }));
    }

    /**
     * This is called just before the popup is shown, and should populate the popup with menu items
     * (and any other desired widgets). The panel will be empty of children when this method is
     * called and any menu items added should be added via {@link #addMenuItem}.
     */
    protected abstract void addMenuItems ();

    protected MenuBar _menu;
}
