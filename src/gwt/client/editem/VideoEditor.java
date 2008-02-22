//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Video;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;

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
        setUploaderMedia(Item.MAIN_MEDIA, _video.videoMedia);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Video();
    }

    // @Override from ItemEditor
    protected void addExtras ()
    {
        addRow(CShell.emsgs.videoLabel(), createMainUploader(TYPE_VIDEO, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                // TODO: remove this hack?
                if (!desc.isVideo()) {
                    return CShell.emsgs.errVideoNotVideo();
                }
                _video.videoMedia = desc;
                return null;
            }
        }), CShell.emsgs.videoTip());

        super.addExtras();
    }

    protected Video _video;
}
