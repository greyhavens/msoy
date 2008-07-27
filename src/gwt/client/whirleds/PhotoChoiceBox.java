//
// $Id$

package client.whirleds;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;

import client.ui.MsoyUI;
import client.util.ImageChooserPopup;
import client.util.MediaUtil;
import client.util.MsoyCallback;

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

        setWidget(0, 1, MsoyUI.createTinyButton("Choose...", new ClickListener() {
            public void onClick (Widget source) {
                ImageChooserPopup.displayImageChooser(thumbnail, new MsoyCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc media) {
                        setMedia(media);
                    }
                });
            }
        }));

        setWidget(1, 0, MsoyUI.createTinyButton("Clear", new ClickListener() {
            public void onClick (Widget source) {
                setMedia(null);
            }
        }));
    }

    public void setMedia (MediaDesc media)
    {
        if ((_media = media) == null) {
            _preview.setWidget(null);
        } else {
            _preview.setWidget(MediaUtil.createMediaView(_media, MediaDesc.HALF_THUMBNAIL_SIZE));
        }
    }

    public MediaDesc getMedia ()
    {
        return _media;
    }

    protected MediaDesc _media;
    protected SimplePanel _preview;
}
