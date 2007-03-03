//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Video;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A class for creating and editing {@link Photo} digital items.
 */
public class VideoEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _video = (Video)item;
        _mainUploader.setMedia(_video.videoMedia);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Video();
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        tabs.add(createMainUploader(CEditem.emsgs.videoMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.isVideo()) {
                    return CEditem.emsgs.errVideoNotVideo();
                }
                _video.videoMedia = desc;
                return null;
            }
        }), CEditem.emsgs.videoMainTab());

        super.createInterface(contents, tabs);
    }

    protected Video _video;
}
