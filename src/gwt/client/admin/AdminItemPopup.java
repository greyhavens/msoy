//
// $Id$

package client.admin;

import client.editem.ItemEditor;
import client.item.BaseItemDetailPopup;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.item.data.all.Item;

/**
 * Displays a popup detail view of an item for an administrator.
 */
public class AdminItemPopup extends BaseItemDetailPopup
{
    public AdminItemPopup (Item item, ReviewPopup parent)
    {
        super(item);
        _parent = parent;
    }

    // @Override // BaseItemDetailPopup
    protected void createInterface (VerticalPanel details, VerticalPanel controls)
    {
        super.createInterface(details, controls);
        
        // if it's an original item, an admin can edit it
        if (_item.parentId == 0) {
            Button button = new Button(CAdmin.msgs.itemPopupEdit());
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    ItemEditor editor = ItemEditor.createItemEditor(_item.getType(), _parent);
                    editor.setItem(_item);
                    editor.show();
                    hide();
                }
            });
            controls.add(button);
        }
    }

    protected ReviewPopup _parent;
    protected Label _status;
}
