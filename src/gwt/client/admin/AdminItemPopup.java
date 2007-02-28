//
// $Id$

package client.admin;

import client.editem.ItemEditor;
import client.item.BaseItemDetailPopup;
import client.shell.MsoyEntryPoint;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

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
    protected Widget createPreview (Item item)
    {
        // TODO: maybe ItemUtil should handle this
        if (item instanceof Avatar) {
            MediaDesc avatarMedia = ((Avatar)_item).avatarMedia;
            return WidgetUtil.createFlashContainer(
                "avatarViewer", "/clients/avatarviewer.swf", 300, 500,
                "avatar=" + URL.encodeComponent(avatarMedia.getMediaPath()));
        } else {
            return super.createPreview(item);
        }
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
