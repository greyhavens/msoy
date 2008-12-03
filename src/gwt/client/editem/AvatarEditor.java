//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;
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
        Avatar avatar = new Avatar();
        avatar.scale = 1f;
        return avatar;
    }

    @Override // from ItemEditor
    protected void addExtras ()
    {
        addSpacer();
        addRow(_emsgs.avatarLabel(), createMainUploader(TYPE_FLASH_REMIXABLE, false,
            new MediaUpdater() {
                public String updateMedia (String name, MediaDesc desc, int width, int height) {
                    if (desc.isImage()) {
                        promptEasyItem(Item.MAIN_MEDIA,
                            new StaticMediaDesc(MediaDesc.APPLICATION_ZIP, "avatar", "easy-proto"),
                            desc, _emsgs.makeEasyAvatar(), _emsgs.makeEasyAvatarDetails());
                        return SUPPRESS_ERROR;
                    }

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
