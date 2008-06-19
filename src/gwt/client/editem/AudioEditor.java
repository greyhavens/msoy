//
// $Id$

package client.editem;

import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;

/**
 * A class for creating and editing {@link Photo} digital items.
 */
public class AudioEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _audio = (Audio)item;
        setUploaderMedia(Item.MAIN_MEDIA, _audio.audioMedia);
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Audio();
    }

    @Override // from ItemEditor
    protected void addExtras ()
    {
        addRow(CShell.emsgs.audioLabel(), createMainUploader(TYPE_AUDIO, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.isAudio()) {
                    return CShell.emsgs.errAudioNotAudio();
                }
                _audio.audioMedia = desc;
                maybeSetNameFromFilename(name);
                return null;
            }
        }), CShell.emsgs.audioTip());

        super.addExtras();
    }

    protected Audio _audio;
}
