//
// $Id$

package client.person;

import client.images.slideshow.SlideshowImages;
import client.shell.Args;
import client.shell.Pages;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlinePanel;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.person.gwt.GalleryData;

/**
 * View a single image in a gallery. This panel will be a child of GalleryViewPanel which contains
 * data on all the photos in the gallery.
 */
public class GalleryPhotoPanel extends FlowPanel
{
    /**
     * Constructor.
     * @param memberId If > 0, fetch the data for the member's profile gallery.
     */
    public GalleryPhotoPanel (GalleryData galleryData)
    {
        _galleryData = galleryData;
        addStyleName("galleryPhotoPanel");
    }

    /**
     * Change which photo is being shown.
     * @param photoIndex Location of the photo in the gallery's list of photos.
     */
    protected void setPhoto (final int photoIndex)
    {
        // slideshow mode has a different look
        if (_slideshowTimer != null) {
            addStyleName("SlideshowMode");
            _slideshowTimer.schedule(SLIDESHOW_DELAY);
        } else {
            removeStyleName("SlideshowMode");
        }

        _currentPhotoIndex = photoIndex;
        final Photo photo = _galleryData.photos.get(photoIndex);
        clear();

        // Gallery and creator name are inline and onclick takes you back to the gallery
        InlinePanel nameAndCreator = new InlinePanel("");
        nameAndCreator.add(MsoyUI.createLabel(_galleryData.gallery.name, "GalleryName"));
        nameAndCreator.add(new CreatorLabel(_galleryData.owner));
        FocusPanel galleryNameFocusPanel = new FocusPanel(nameAndCreator);
        galleryNameFocusPanel.addStyleName("NameAndCreator");
        galleryNameFocusPanel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    stopSlideshow();
                    gotoPhotoIndex(-1);
                }
            });
        add(galleryNameFocusPanel);
        add(MsoyUI.createLabel(photo.name, "PhotoName"));

        // determine the width of the most constrained side and override the media constraint
        int width = photo.photoWidth;
        int height = photo.photoHeight;
        if (width > MAX_PHOTO_WIDTH) {
            width = MAX_PHOTO_WIDTH;
            height = Math.round((MAX_PHOTO_WIDTH / (float)photo.photoWidth) * photo.photoHeight);
        } else {
            width = 0;
        }
        if (height > MAX_PHOTO_HEIGHT) {
            height = MAX_PHOTO_HEIGHT;
            width = 0;
        } else {
            height = 0;
        }
        byte constraint = MediaDesc.NOT_CONSTRAINED;
        if (width > 0) {
            constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
        } else if (height > 0) {
            constraint = MediaDesc.VERTICALLY_CONSTRAINED;
        }
        photo.photoMedia.constraint = constraint;

        // clear the photo onclick
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                stopSlideshow();
                gotoPhotoIndex((photoIndex + 1) % _galleryData.photos.size());
            }
        };
        Widget largePhoto = MediaUtil.createMediaView(photo.photoMedia, width, height, onClick);
        FlowPanel largePhotoContainer = MsoyUI.createFlowPanel("LargePhoto");
        largePhotoContainer.add(largePhoto);
        add(largePhotoContainer);

        // slideshow, prev/next, full size controls
        final AbsolutePanel controls = new AbsolutePanel();
        controls.addStyleName("Controls");
        add(controls);

        // start slideshow button and text
        if (_slideshowTimer == null) {
            ClickListener slideshowClick = new ClickListener() {
                public void onClick (Widget sender) {
                    startSlideshow();
                }
            };
            controls.add(MsoyUI.createPushButton(
                _slideshowImages.play_default().createImage(),
                _slideshowImages.play_over().createImage(),
                _slideshowImages.play_down().createImage(), slideshowClick), 0, 0);
            controls.add(MsoyUI.createActionLabel(_pmsgs.gallerySlideshowStart(), slideshowClick),
                35, 5);
        }

        // prev and next buttons
        ClickListener onPrev = new ClickListener() {
            public void onClick (Widget sender) {
                gotoPhotoIndex(photoIndex == 0 ? _galleryData.photos.size() - 1 : photoIndex - 1);
            }
        };
        ClickListener onNext = new ClickListener() {
            public void onClick (Widget sender) {
                gotoPhotoIndex((photoIndex + 1) % _galleryData.photos.size());
            }
        };
        int prevNextLeftOffset = _slideshowTimer == null ? 300 : 230;
        controls.add(MsoyUI.createPrevNextButtons(onPrev, onNext), prevNextLeftOffset, 0);

        // controls for when slideshow is running (or paused)
        if (_slideshowTimer != null) {

            final int leftOffset = 380;
            // play and pause buttons swap when clicked
            _playButton = MsoyUI.createPushButton(
                _slideshowImages.play_default().createImage(),
                _slideshowImages.play_over().createImage(),
                _slideshowImages.play_down().createImage(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _slideshowPaused = false;
                        _slideshowTimer.schedule(1);
                        controls.remove(_playButton);
                        controls.add(_pauseButton, leftOffset, 0);
                    }
                });
            _pauseButton = MsoyUI.createPushButton(
                _slideshowImages.pause_default().createImage(),
                _slideshowImages.pause_over().createImage(),
                _slideshowImages.pause_down().createImage(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _slideshowPaused = true;
                        controls.remove(_pauseButton);
                        controls.add(_playButton, leftOffset, 0);
                    }
                });
            if (_slideshowPaused) {
                controls.add(_playButton, leftOffset, 0);
            } else {
                controls.add(_pauseButton, leftOffset, 0);
            }

            controls.add(MsoyUI.createPushButton(
                _slideshowImages.close_default().createImage(),
                _slideshowImages.close_over().createImage(),
                _slideshowImages.close_down().createImage(), new ClickListener() {
                    public void onClick (Widget sender) {
                        stopSlideshow();
                        gotoPhotoIndex(-1);
                    }
                }), 410, 0);
        }

        // if this image has been scaled down, link to the full size
        if (_slideshowTimer == null) {
            if (photo.photoMedia.constraint != MediaDesc.NOT_CONSTRAINED) {
                controls.add(MsoyUI.createExternalAnchor(photo.photoMedia.getMediaPath(),
                    _pmsgs.galleryFullSize()), 590, 5);
            }
        }
    }

    /**
     * Moves the page to the appropriate photo based on it's location in the list of photos. Use
     * itemId instead of photo index in case the order of photos changes.
     */
    public void gotoPhotoIndex (int photoIndex)
    {
        Photo photo = photoIndex >= 0 ? _galleryData.photos.get(photoIndex) : null;
        Link.go(Pages.PEOPLE, Args.compose(GalleryViewPanel.VIEW_PHOTO_ACTION,
            _galleryData.gallery.galleryId, photo == null ? 0 : photo.itemId));
    }

    /**
     * Start the slideshow immediately at the first photo or next photo if one is being shown.
     */
    protected void startSlideshow ()
    {
        _slideshowPaused = false;
        _slideshowTimer = new Timer() {
            @Override public void run() {
                advanceSlideshow();
            }
        };
        _slideshowTimer.schedule(1);
    }

    /**
     * Display the next image in the gallery and schedule another change in 5 seconds.
     */
    protected void advanceSlideshow ()
    {
        if (!_slideshowPaused) {
            if (_currentPhotoIndex < 0) {
                gotoPhotoIndex(0);
            } else {
                gotoPhotoIndex((_currentPhotoIndex + 1) % _galleryData.photos.size());
            }
        }
        // timer will also be rescheduled after the new image loads
    }

    /**
     * Halt the slideshow and return to the list of photos.
     */
    protected void stopSlideshow ()
    {
        if (_slideshowTimer != null) {
            _slideshowTimer.cancel();
            _slideshowTimer = null;
        }
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final SlideshowImages _slideshowImages = GWT.create(SlideshowImages.class);

    protected PushButton _playButton;
    protected PushButton _pauseButton;

    /** While set to true, timer events will fire but do nothing */
    protected boolean _slideshowPaused = false;

    /** List of photos and gallery details */
    protected GalleryData _galleryData;

    /** The index of the photo currently being displayed, used for slideshow */
    protected int _currentPhotoIndex;

    /** Fires every 5 seconds while slideshow is running */
    protected Timer _slideshowTimer;

    protected static int MAX_PHOTO_WIDTH = 600;
    protected static int MAX_PHOTO_HEIGHT = 400;
    protected static final int SLIDESHOW_DELAY = 5000;
}
