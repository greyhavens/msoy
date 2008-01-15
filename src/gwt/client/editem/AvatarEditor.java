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
        _mainUploader.setMedia(_avatar.avatarMedia);
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
        addRow(CShell.emsgs.avatarMainTab(), createMainUploader(false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CShell.emsgs.errAvatarNotFlash();
                }
                _avatar.avatarMedia = desc;
                _avatar.scale = 1f;
                return null;
            }
        }), CShell.emsgs.avatarMainTitle());

        super.addExtras();
    }

    // @Override from ItemEditor
    protected void addFurniUploader ()
    {
        // nada: avatars should no longer have a furni visualization
    }

    protected Avatar _avatar;
}
