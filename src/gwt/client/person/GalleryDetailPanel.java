//
// $Id$

package client.person;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryData;

/**
 * Displays the Gallery meta data: name, description, thumbnail, yada yada.
 *
 * @author mjensen
 */
public class GalleryDetailPanel extends AbsolutePanel
{
    public GalleryDetailPanel (GalleryData galleryData)
    {
        Gallery gallery = galleryData.gallery;
        setStyleName("galleryDetailPanel");

        // Thumbnail
        if (gallery.thumbMedia != null) {
            add(MsoyUI.createSimplePanel("GalleryThumbnail", MediaUtil.createMediaView(
                gallery.thumbMedia, MediaDesc.THUMBNAIL_SIZE)), 10, 60);
        } else {
            add(MsoyUI.createLabel("no image", "GalleryThumbnail"), 10, 60);
        }

        String countText = galleryData.photos.size() == 1 ? _pmsgs.onePhoto() :
            _pmsgs.photoCount(""+galleryData.photos.size());
        add(MsoyUI.createLabel(countText, "Count"), 110, 70);

        // add name and description labels
        add(MsoyUI.createLabel(GalleryPanel.getGalleryLabel(gallery), "Name"), 10, 10);
        add(MsoyUI.createLabel(gallery.description, "Description"), 10, 140);

        // if the current member owns this read-only gallery, add an edit button
        if (galleryData.ownerId == CShell.getMemberId()) {
            final String args = Args.compose(GalleryEditPanel.EDIT_ACTION, gallery.galleryId);
            final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
            add(MsoyUI.createButton(MsoyUI.LONG_THIN, _pmsgs.editButton(), listener), 40, 270);
        }
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
}
