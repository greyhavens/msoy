//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;

/**
 * A class for creating and editing {@link Avatar} digital items.
 */
public class AvatarEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _avatar = (Avatar)item;
        setUploaderMedia(Item.MAIN_MEDIA, _avatar.avatarMedia);
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Avatar();
    }

    @Override // from ItemEditor
    protected void addExtras ()
    {
        addSpacer();
        addRow(_emsgs.avatarLabel(), createMainUploader(TYPE_FLASH, false,
            new MediaUpdater() {
                public String updateMedia (String name, MediaDesc desc, int width, int height) {
                    if (!isValidPrimaryMedia(desc)) {
                        return _emsgs.errAvatarNotFlash();
                    }
                    _avatar.avatarMedia = desc;
                    _avatar.scale = 1f;
                    return null;
                }
            }), _emsgs.avatarTip());

        super.addExtras();
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        // nada: avatars should no longer have a furni visualization
    }

    protected Avatar _avatar;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
