//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.PromptPopup;
import client.util.Link;
import client.util.StringUtil;

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
        addRow(_emsgs.avatarLabel(), createMainUploader(TYPE_FLASH_ONLY, false,
            new MediaUpdater() {
                public String updateMedia (String name, MediaDesc desc, int width, int height) {
                    if (desc.isImage()) {
                        promptEasyItem(desc);
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

    protected void promptEasyItem (final MediaDesc image)
    {
        new PromptPopup(_emsgs.makeEasyAvatar(), new Command() {
            public void execute () {
                createEasyItem(image);
            }
        }).setContext(_emsgs.makeEasyAvatarDetails()).prompt();
    }

    protected void createEasyItem (MediaDesc image)
    {
        Item easy = createBlankItem();
        _easyImage = image;

        easy.name = StringUtil.isBlank(_avatar.name) ? _emsgs.easyAvatarName() : _avatar.name;
        easy.description = _avatar.description; // ok if blank
        easy.setThumbnailMedia(_avatar.getRawThumbnailMedia()); // ok if null
        ((Avatar) easy).avatarMedia = EASY_IMAGE_PROTOTYPE;
        setItem(easy);
        commitEdit();
    }

    @Override
    protected void editComplete (Item item, boolean created)
    {
        if (_easyImage != null) {
            Link.go(Pages.STUFF, Args.compose("r", item.getType(), item.itemId,
                MediaDesc.mdToString(_easyImage)));

        } else {
            super.editComplete(item, created);
        }
    }

    protected Avatar _avatar;

    /** A reference to an image the user wants to use while creating an easy image avatar. */
    protected MediaDesc _easyImage;

    protected static final MediaDesc EASY_IMAGE_PROTOTYPE = new MediaDesc(
        "c0f4e955e7634d68495fb327a51043b892ccbb64", MediaDesc.APPLICATION_ZIP,
        MediaDesc.NOT_CONSTRAINED);

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
