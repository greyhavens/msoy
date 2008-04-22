//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;

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
        setUploaderMedia(Item.MAIN_MEDIA, _avatar.avatarMedia);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Avatar();
    }

    // @Override from ItemEditor
    protected void addExtras ()
    {
        addSpacer();
        addRow(CShell.emsgs.avatarLabel(), createMainUploader(TYPE_FLASH, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                // TODO: when all item types support zipped media, change
                // hasFlashVisual to support ZIP, then we won't need
                // the special check for REMIXABLE here.
                if (!desc.hasFlashVisual() && !desc.isRemixable()) {
                    return CShell.emsgs.errAvatarNotFlash();
                }
                _avatar.avatarMedia = desc;
                _avatar.scale = 1f;
                return null;
            }
        }), CShell.emsgs.avatarTip());

        super.addExtras();
    }

    // @Override from ItemEditor
    protected void addFurniUploader ()
    {
        // nada: avatars should no longer have a furni visualization
    }

    protected Avatar _avatar;
}
