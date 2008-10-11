//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Video;
import com.threerings.msoy.item.data.all.Item;

/**
 * A class for creating and editing {@link Video} digital items.
 */
public class VideoEditor extends BulkMediaEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _video = (Video)item;
        setUploaderMedia(Item.MAIN_MEDIA, _video.videoMedia);
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Video();
    }

    @Override // from BulkMediaEditor
    protected void addMainUploader ()
    {
        addRow(_emsgs.videoLabel(), createMainUploader(TYPE_VIDEO, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                // TODO: remove this hack?
                if (!desc.isVideo()) {
                    return _emsgs.errVideoNotVideo();
                }
                _video.videoMedia = desc;
                return null;
            }
        }), _emsgs.videoTip());
    }

    protected Video _video;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
