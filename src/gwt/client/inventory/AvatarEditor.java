//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

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
        _description.setText((_avatar.description == null)
            ? "" : _avatar.description);
        _mainUploader.setMedia(_avatar.avatarMedia);
        _headShotUploader.setMedia(_avatar.getHeadShotMedia());
    }

    // @Override from ItemEditor
    protected void createEditorInterface ()
    {
        configureMainUploader("Upload your avatar.", new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.hasFlashVisual()) {
                    return "Avatars must be a web-viewable media.";
                }

                _avatar.avatarMedia = desc;
                recenter(true);
                return null;
            }
        });

        String title = "Head shot";
        _headShotUploader = createUploader(
            HEADSHOT_ID, title, ItemContainer.THUMB_HEIGHT, new MediaUpdater() {
                public String updateMedia (MediaDesc desc) {
                    if (!desc.isImage()) {
                        return "Head shots must be an image type.";
                    }

                    _avatar.headShotMedia = desc;
                    recenter(true);
                    return null;
                }
            });

        super.createEditorInterface();

        addRow("Description", _description = new TextBox());
        bind(_description, new Binder() {
            public void textUpdated (String text) {
                _avatar.description = text;
            }
        });
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
    protected TextBox _description;

    protected MediaUploader _headShotUploader;

    protected static final String HEADSHOT_ID = "headshot";
}
