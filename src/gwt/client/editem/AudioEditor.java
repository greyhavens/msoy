//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Audio;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A class for creating and editing {@link Photo} digital items.
 */
public class AudioEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _audio = (Audio)item;
        _mainUploader.setMedia(_audio.audioMedia);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Audio();
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        tabs.add(createMainUploader(CEditem.emsgs.audioMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.isAudio()) {
                    return CEditem.emsgs.errAudioNotAudio();
                }
                _audio.audioMedia = desc;
                return null;
            }
        }), CEditem.emsgs.audioMainTab());

        super.createInterface(contents, tabs);
    }

    protected Audio _audio;
}
