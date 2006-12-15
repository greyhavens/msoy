//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Audio;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

/**
 *  * A class for creating and editing {@link Photo} digital items.
 *   */
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
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        tabs.add(createMainUploader("Upload your audio.", new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.isAudio()) {
                    return "Audio data must be audio!";
                }
                _audio.audioMedia = desc;
                recenter(true);
                return null;
            }
        }), "Audio Media");

        super.createInterface(contents, tabs);
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Audio();
    }

    protected Audio _audio;
}
