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
    public GalleryDetailPanel (GalleryData galleryData, boolean readOnly)
    {
        _gallery = galleryData.gallery;
        setStyleName("galleryDetailPanel");

        // Thumbnail
        if (_gallery.thumbMedia != null) {
            add(MsoyUI.createSimplePanel("GalleryThumbnail", MediaUtil.createMediaView(
                _gallery.thumbMedia, MediaDesc.THUMBNAIL_SIZE)), 10, 60);
        } else {
            add(MsoyUI.createLabel("no image", "GalleryThumbnail"), 10, 60);
        }

        _countLabel = MsoyUI.createLabel("", "Count");
        setCount(galleryData.photos.size());
        add(_countLabel, 110, 70);

        if (readOnly) {
            // add name and description labels
            add(MsoyUI.createLabel(GalleryPanel.getGalleryLabel(_gallery), "Name"), 10, 10);
            add(MsoyUI.createLabel(_gallery.description, "Description"), 10, 140);
        } else {
            // do not allow profile gallery name to be edited
            if (_gallery.isProfileGallery()) {
                add(MsoyUI.createLabel(GalleryPanel.getGalleryLabel(_gallery), "Name"), 10, 10);
            } else {
                TextBox name = MsoyUI.createTextBox(_gallery.name, Gallery.MAX_NAME_LENGTH,
                    Gallery.MAX_NAME_LENGTH);
                name.addStyleName("Name");
                name.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        _gallery.name = ((TextBox) sender).getText();
                    }
                });
                add(name, 10, 10);
            }
            final LimitedTextArea description = new LimitedTextArea(Gallery.MAX_DESCRIPTION_LENGTH,
                20, 10);
            description.setText(_gallery.description);
            description.addStyleName("Description");
            description.getTextArea().addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    _gallery.description = description.getText();
                }
            });
            add(description, 10, 140);
        }

        // if the current member owns this read-only gallery, add an edit button
        if (readOnly && galleryData.ownerId == CShell.getMemberId()) {
            final String args = Args.compose(GalleryEditPanel.EDIT_ACTION, _gallery.galleryId);
            final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
            add(MsoyUI.createButton(MsoyUI.LONG_THIN, _pmsgs.editButton(), listener), 40, 270);
        }
    }

    /**
     * Updates the count label with the current number of gallery photos.
     */
    public void setCount (int count)
    {
        String text = count == 1 ? _pmsgs.onePhoto() : _pmsgs.photoCount(""+count);
        _countLabel.setText(text);
    }

    protected Gallery _gallery;
    protected Label _countLabel;

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
}
