//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

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
        addRow(_emsgs.audioLabel(), createMainUploader(TYPE_AUDIO, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.isAudio()) {
                    return _emsgs.errAudioNotAudio();
                }
                _audio.audioMedia = desc;
                maybeSetNameFromFilename(name);
                return null;
            }
        }), _emsgs.audioTip());

        super.addExtras();
    }

    protected Audio _audio;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
