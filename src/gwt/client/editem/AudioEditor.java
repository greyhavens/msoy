//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;

/**
 * A class for creating and editing {@link Audio} digital items.
 */
public class AudioEditor extends BulkMediaEditor
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

    @Override // from BulkMediaEditor
    protected void addMainUploader ()
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
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        // suppress
    }

    protected Audio _audio;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
