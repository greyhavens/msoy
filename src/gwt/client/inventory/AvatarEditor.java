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
        String title = "Avatar as seen in the World";
        tabs.add(createMainUploader(title, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.hasFlashVisual()) {
                    return "Avatars must be a web-viewable media.";
                }
                _avatar.avatarMedia = desc;
                return null;
            }
        }), "Avatar Media");

        super.createInterface(contents, tabs);
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Avatar();
    }

    protected Avatar _avatar;
}
