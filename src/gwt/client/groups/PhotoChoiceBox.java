//
// $Id$

package client.groups;

import com.threerings.orth.data.MediaDesc;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.data.all.MediaDescSize;

import client.ui.MsoyUI;
import client.util.MediaUtil;
import client.util.InfoCallback;

import client.imagechooser.ImageChooserPopup;
/**
 * Displays a small preview image and allows the selection of an image from the user's inventory.
 */
public class PhotoChoiceBox extends FlexTable
{
    public PhotoChoiceBox (final boolean thumbnail, MediaDesc media)
    {
        setStyleName("photoChoiceBox");
        setCellPadding(0);
        setCellSpacing(0);

        setWidget(0, 0, _preview = new SimplePanel());
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setStyleName(0, 0, "Preview");

        setMedia(media);

        setWidget(0, 1, MsoyUI.createTinyButton("Choose...", new ClickHandler() {
            public void onClick (ClickEvent event) {
                ImageChooserPopup.displayImageChooser(thumbnail, new InfoCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc media) {
                        if (media != null) {
                            setMedia(media);
                        }
                    }
                });
            }
        }));

        setWidget(1, 0, MsoyUI.createTinyButton("Clear", new ClickHandler() {
            public void onClick (ClickEvent event) {
                setMedia(null);
            }
        }));
    }

    public void setMedia (MediaDesc media)
    {
        if ((_media = media) == null) {
            _preview.setWidget(null);
        } else {
            _preview.setWidget(MediaUtil.createMediaView(_media, MediaDescSize.HALF_THUMBNAIL_SIZE));
        }
    }

    public MediaDesc getMedia ()
    {
        return _media;
    }

    protected MediaDesc _media;
    protected SimplePanel _preview;
}
