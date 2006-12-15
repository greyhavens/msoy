//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Photo;

/**
 * A class for creating and editing {@link Photo} digital items.
 */
public class PhotoEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _photo = (Photo)item;
        _mainUploader.setMedia(_photo.photoMedia);
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        tabs.add(createMainUploader("Main Photo image", new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.hasFlashVisual()) {
                    return "Photos must be a web-viewable image type.";
                }
                _photo.photoMedia = desc;
                recenter(true);
                return null;
            }
        }), "Photo Media");

        super.createInterface(contents, tabs);
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Photo();
    }

    protected Photo _photo;
}
