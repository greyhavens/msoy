//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A class for creating and editing {@link Avatar} digital items.
 */
public class AvatarEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _avatar = (Avatar)item;
        _mainUploader.setMedia(_avatar.avatarMedia);
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        tabs.add(createMainUploader(_ctx.imsgs.avatarMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.hasFlashVisual()) {
                    return _ctx.imsgs.errAvatarNotFlash();
                }
                _avatar.avatarMedia = desc;
                return null;
            }
        }), _ctx.imsgs.avatarMainTab());

        super.createInterface(contents, tabs);
    }

    // @Override from ItemEditor
    protected void createThumbUploader (TabPanel tabs)
    {
        String title = _ctx.imsgs.avatarThumbTitle();
        _thumbUploader = createUploader(Item.THUMB_MEDIA, title, true, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.isImage()) {
                    return _ctx.imsgs.errThumbNotImage();
                }
                _item.thumbMedia = desc;
                return null;
            }
        });
        tabs.add(_thumbUploader, _ctx.imsgs.avatarThumbTab());
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Avatar();
    }

    protected Avatar _avatar;
}
