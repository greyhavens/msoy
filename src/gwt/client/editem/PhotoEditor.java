//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Photo;

/**
 * A class for creating and editing {@link Photo} digital items.
 */
public class PhotoEditor extends BulkMediaEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _photo = (Photo)item;
        setUploaderMedia(Item.MAIN_MEDIA, _photo.photoMedia);
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Photo();
    }

    @Override // from BulkMediaEditor
    protected void addMainUploader ()
    {
        addRow(_emsgs.photoLabel(), createMainUploader(TYPE_IMAGE, true, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.isImage()) {
                    return _emsgs.errPhotoNotImage();
                }
                _photo.photoMedia = desc;
                _photo.photoWidth = width;
                _photo.photoHeight = height;
                maybeSetNameFromFilename(name);
                return null;
            }
        }), _emsgs.photoTip());
    }

    @Override // from ItemEditor
    protected String getFurniTabText ()
    {
        return _emsgs.editorPhotoFurniTab();
    }

    @Override// from ItemEditor
    protected String getFurniTitleText ()
    {
        return _emsgs.editorPhotoFurniTitle();
    }

    protected Photo _photo;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
