//
// $Id$

package client.admin;

import client.item.BaseItemDetailPopup;
import client.shell.MsoyEntryPoint;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Label;
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
            String path = MsoyEntryPoint.toMediaPath(avatarMedia.getMediaPath());
            return WidgetUtil.createFlashContainer(
                "avatarViewer", "/clients/avatarviewer.swf", 300, 500,
                "avatar=" + URL.encodeComponent(path));
        } else {
            return super.createPreview(item);
        }
    }

    protected ReviewPopup _parent;
    protected Label _status;
}
