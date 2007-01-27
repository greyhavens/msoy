//
// $Id$

package client.editem;

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
    public Item createBlankItem ()
    {
        return new Photo();
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        tabs.add(createMainUploader(CEditem.emsgs.photoMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CEditem.emsgs.errPhotoNotFlash();
                }
                _photo.photoMedia = desc;
                _photo.photoWidth = width;
                _photo.photoHeight = height;
                return null;
            }
        }), CEditem.emsgs.photoMainTab());

        super.createInterface(contents, tabs);
    }

    protected Photo _photo;
}
