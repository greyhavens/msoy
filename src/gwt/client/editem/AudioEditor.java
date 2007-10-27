//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;

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
        tabs.add(createMainUploader(CShell.emsgs.audioMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.isAudio()) {
                    return CShell.emsgs.errAudioNotAudio();
                }
                _audio.audioMedia = desc;
                return null;
            }
        }), CShell.emsgs.audioMainTab());

        super.createInterface(contents, tabs);
    }

    protected Audio _audio;
}
