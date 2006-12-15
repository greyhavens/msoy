//
// $Id$

package client.inventory;

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
        _headShotUploader.setMedia(_avatar.getHeadShotMedia());
    }

    // @Override from ItemEditor
    protected void createMainInterface (VerticalPanel main)
    {
        super.createMainInterface(main);

        String title = "Main Avatar media";
        main.add(createMainUploader(title, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.hasFlashVisual()) {
                    return "Avatars must be a web-viewable media.";
                }
                _avatar.avatarMedia = desc;
                recenter(true);
                return null;
            }
        }));

        title = "Image show in a Game Lobby";
        _headShotUploader = new MediaUploader(HEADSHOT_ID, title, true, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.isImage()) {
                    return "Head shots must be an image type.";
                }
                _avatar.headShotMedia = desc;
                recenter(true);
                return null;
            }
        });
        main.add(_headShotUploader);
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Avatar();
    }

    // @Override from ItemEditor
    protected MediaUploader getUploader (String id)
    {
        if (HEADSHOT_ID.equals(id)) {
            return _headShotUploader;
        } else {
            return super.getUploader(id);
        }
    }

    protected Avatar _avatar;
    protected MediaUploader _headShotUploader;

    protected static final String HEADSHOT_ID = "headshot";
}
