//
// $Id$

package client.person;

import client.ui.MsoyUI;
import client.util.MediaUtil;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.gwt.Gallery;

/**
 * Displays the Gallery meta data: name, description, thumbnail, yada yada.
 *
 * @author mjensen
 */
public class GalleryDetailPanel extends SmartTable
{
    public GalleryDetailPanel (Gallery gallery, boolean readOnly)
    {
        _gallery = gallery;

        addStyleName("GalleryDetail");

        // Thumbnail
        if (gallery.thumbMedia != null) {
            setWidget(0, 0, MediaUtil.createMediaView(gallery.thumbMedia, MediaDesc.THUMBNAIL_SIZE));
        } else {
            setWidget(0, 0, MsoyUI.createLabel("TODO", "Thumb"));
        }

        // TODO # of photos - plus needs to listen for changes to count
        setWidget(0, 1, MsoyUI.createLabel("TODO", "Count"));

        if (readOnly) {
            // add name and description labels
            setWidget(1, 0, MsoyUI.createLabel(GalleryPanel.getGalleryLabel(gallery), "Name"));
            setWidget(2, 0, MsoyUI.createLabel(gallery.description, "Description"));

        } else {
            // do not allow profile gallery name to be edited
            if (gallery.isProfileGallery()) {
                setWidget(1, 0, MsoyUI.createLabel(GalleryPanel.getGalleryLabel(gallery), "Name"));
            } else {
                TextBox name = MsoyUI.createTextBox(gallery.name, Gallery.MAX_NAME_LENGTH,
                    VISIBLE_WIDTH);
                name.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        _gallery.name = ((TextBox) sender).getText();
                    }
                });
                name.addStyleName("Name");
                setWidget(1, 0, name);
            }

            TextArea description = MsoyUI.createTextArea(gallery.description, VISIBLE_WIDTH,
                VISIBLE_HEIGHT);
            description.addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    _gallery.description = ((TextArea) sender).getText();
                }
            });
            description.addStyleName("Description");
            setWidget(2, 0, description);
        }

        // let the name and description fields stretch their feet a bit
        getFlexCellFormatter().setColSpan(1, 0, 2);
        getFlexCellFormatter().setColSpan(2, 0, 2);
    }

    protected Gallery _gallery;

    protected static final int VISIBLE_WIDTH = 30;
    protected static final int VISIBLE_HEIGHT = 12;
}
