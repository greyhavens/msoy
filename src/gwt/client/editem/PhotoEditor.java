//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;

import client.shell.CShell;

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
        tabs.add(createMainUploader(CShell.emsgs.photoMainTitle(), new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CShell.emsgs.errPhotoNotFlash();
                }
                _photo.photoMedia = desc;
                _photo.photoWidth = width;
                _photo.photoHeight = height;
                maybeSetNameFromFilename(name);
                return null;
            }
        }), CShell.emsgs.photoMainTab());

        super.createInterface(contents, tabs);
    }

    // @Override from ItemEditor
    protected int getDefaultTabIndex ()
    {
        // if this is a blank item, start on the main media panel, otherwise the default
        return (_photo.photoMedia == null) ? 1 : super.getDefaultTabIndex();
    }

    protected Photo _photo;
}
