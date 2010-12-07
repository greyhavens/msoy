//
// $Id$

package client.groups;

import com.threerings.orth.data.MediaDesc;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.orth.data.MediaDescSize;

import client.ui.MsoyUI;
import client.util.MediaUtil;
import client.util.InfoCallback;

import client.imagechooser.ImageChooserPopup;
/**
 * Displays a small preview image and allows the selection of an image from the user's inventory.
 * A null (or reverted) media will be displayed as the default but returned as null.
 */
public class PhotoChoiceBox extends FlexTable
{
    public PhotoChoiceBox (
        final boolean thumbnail, MediaDesc initialMedia, final MediaDesc defaultMedia)
    {
        setStyleName("photoChoiceBox");
        setCellPadding(0);
        setCellSpacing(0);

        setWidget(0, 0, _preview = new SimplePanel());
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setStyleName(0, 0, "Preview");

        _initialMedia = initialMedia;
        _defaultMedia = defaultMedia;

        buildUI(thumbnail);
    }

    public void setMedia (MediaDesc media)
    {
        _currentMedia = media;

        _preview.setWidget(MediaUtil.createMediaView(
            (_currentMedia != null) ? _currentMedia : _defaultMedia, MediaDescSize.HALF_THUMBNAIL_SIZE));

        _clearButton.setEnabled(_currentMedia != null);
        if (_revertButton != null) {
            _revertButton.setEnabled(_currentMedia == null || !_currentMedia.equals(_initialMedia));
        }
    }

    public MediaDesc getMedia ()
    {
        return _currentMedia;
    }

    protected void buildUI (final boolean thumbnail)
    {
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

        _clearButton = MsoyUI.createTinyButton("Clear", new ClickHandler() {
            public void onClick (ClickEvent event) {
                setMedia(null);
            }
        });
        setWidget(1, 0, _clearButton);

        if (_initialMedia != null) {
            _revertButton = MsoyUI.createTinyButton("Revert", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    setMedia(_initialMedia);
                }
            });
            setWidget(1, 1, _revertButton);
        }

        setMedia(_initialMedia);
    }

    protected Button _clearButton, _revertButton;
    protected MediaDesc _currentMedia, _initialMedia, _defaultMedia;
    protected SimplePanel _preview;
}
