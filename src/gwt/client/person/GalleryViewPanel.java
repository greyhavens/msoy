//
// $Id$

package client.person;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.CenteredBox;
import com.threerings.gwt.ui.InlinePanel;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.person.gwt.GalleryData;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.GalleryServiceAsync;

/**
 * A read-only view of the photos in a gallery. Also handles profile galleries which are special
 * in that there is only one per member (although it may or may not exist), and may be fetched
 * using the memberId instead of the galleryId.
 *
 * @author mjensen
 */
public class GalleryViewPanel extends FlowPanel
{
    public static final String VIEW_ACTION = "gallery";
    public static final String VIEW_PROFILE_ACTION = "pgallery";

    /**
     * Expects either galleryId if displaying a specific gallery, or a memberId if displaying that
     * member's profile gallery.
     * @param galleryId
     * @param memberId
     */
    public GalleryViewPanel (final int galleryId, final int memberId, final int photoId)
    {
        addStyleName("galleryViewPanel");

        if (galleryId > 0) {
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
            public void onSuccess (GalleryData galleryData) {
                displayGallery(galleryData, photoId);
            }
        });
        } else {
            _profileMemberId = memberId;
            _gallerysvc.loadMeGallery(memberId, new MsoyCallback<GalleryData>() {
                public void onSuccess (GalleryData galleryData) {
                    displayGallery(galleryData, photoId);
                }
            });
        }
    }

    /**
     * If galleryData contains a list of images, display the gallery details and thumbnails for
     * those images, otherwise display an error and/or edit button.
     */
    protected void displayGallery (GalleryData galleryData, final int photoId)
    {
        FlowPanel error = MsoyUI.createFlowPanel("Error");
        if (galleryData == null) {
            if (_profileMemberId > 0) {
                if (_profileMemberId == CShell.getMemberId()) {
                    error.add(new HTML(_pmsgs.galleryProfileNoPhotosSelf()));
                    final String args = Args.compose(GalleryEditPanel.CREATE_PROFILE_ACTION,
                        _profileMemberId);
                    final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
                    error.add(MsoyUI.createActionLabel(_pmsgs.galleryAddPhotos(), listener));
                } else {
                    error.add(new HTML(_pmsgs.galleryNoPhotosOther()));
                }
            } else {
                error.add(new HTML(_pmsgs.galleryNotFound()));
            }

        } else if (galleryData.photos == null || galleryData.photos.size() == 0) {
            if (galleryData.owner.getMemberId() == CShell.getMemberId()) {
                error.add(new HTML(_pmsgs.galleryNoPhotosSelf()));
                final String args = Args.compose(GalleryEditPanel.EDIT_ACTION,
                    galleryData.gallery.galleryId);
                final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
                error.add(MsoyUI.createActionLabel(_pmsgs.galleryAddPhotos(), listener));
            } else {
                error.add(new HTML(_pmsgs.galleryNoPhotosOther()));
            }
        }
        // if there is an error, display it and stop rendering page
        if (error.getWidgetCount() > 0) {
            add(error);
            return;
        }
        _galleryData = galleryData;

        // click gallery detail panel to return to list of images
        ClickListener galleryDetailListener = new ClickListener() {
            public void onClick (Widget sender) {
                setPhoto(-1);
            }
        };
        FocusPanel galleryDetailClick = new FocusPanel(new GalleryDetailPanel(galleryData));
        galleryDetailClick.addClickListener(galleryDetailListener);
        add(galleryDetailClick);

        // slieshow | view all galleries | edit actions
        InlinePanel actions = new InlinePanel("Actions");
        ClickListener slideshowClick = new ClickListener() {
            public void onClick (Widget sender) {
                Label slideshowLabel = (Label)sender;
                // toggle slideshow and text of this link
                if (slideshowLabel.getText().equals(_pmsgs.gallerySlideshowStart())) {
                    slideshowLabel.setText(_pmsgs.gallerySlideshowStop());
                    slideshowLabel.addStyleName("Stop");
                    startSlideshow();
                } else {
                    slideshowLabel.setText(_pmsgs.gallerySlideshowStart());
                    slideshowLabel.removeStyleName("Stop");
                    stopSlideshow();
                }
            }
        };
        actions.add(MsoyUI.createActionLabel(_pmsgs.gallerySlideshowStart(), slideshowClick));
        actions.add(new Label("|"));
        final String viewGalleriesArgs = Args.compose(GalleryPanel.GALLERIES_ACTION,
            _galleryData.owner.getMemberId());
        actions.add(MsoyUI.createActionLabel(_pmsgs.galleryViewAll(),
            Link.createListener(Pages.PEOPLE, viewGalleriesArgs)));
        if (galleryData.owner.getMemberId() == CShell.getMemberId()) {
            actions.add(new Label("|"));
            final String args = Args.compose(GalleryEditPanel.EDIT_ACTION,
                _galleryData.gallery.galleryId);
            final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
            actions.add(MsoyUI.createActionLabel(_pmsgs.editButton(), listener));
        }
        add(actions);

        // list all the photos
        add(_photoPanel = new FlowPanel());
        for (int ii = 0; ii < galleryData.photos.size(); ii++) {
            final int photoIndex = ii;
            Photo photo = galleryData.photos.get(ii);

            // clicking on an image will bring up the full view of it
            ClickListener thumbClickListener = new ClickListener() {
                public void onClick (Widget sender) {
                    setPhoto(photoIndex);
                }
            };

            // add thumbnail and image name to a box
            Widget image = MediaUtil.createMediaView(photo.getPreviewMedia(),
                MediaDesc.getWidth(MediaDesc.PREVIEW_SIZE) / 2,
                MediaDesc.getHeight(MediaDesc.PREVIEW_SIZE) / 2, thumbClickListener);
            FlowPanel thumbnail = MsoyUI.createFlowPanel("Thumbnail");
            // size the containing box here, include space for 1px border
            thumbnail.add(new CenteredBox(image, "Image",
                MediaDesc.getWidth(MediaDesc.PREVIEW_SIZE) / 2 + 2,
                MediaDesc.getHeight(MediaDesc.PREVIEW_SIZE) / 2 + 2));
            thumbnail.add(MsoyUI.createLabel(photo.name, "Name"));
            _photoPanel.add(thumbnail);
        }

        // this will be filled with the photos
        _currentPhoto = new AbsolutePanel();
        _currentPhoto.addStyleName("CurrentPhoto");

        // set the current photo if there was one in the URL
        if (photoId > 0) {
            for (Photo photo : _galleryData.photos) {
                if (photo.itemId == photoId) {
                    setPhoto(_galleryData.photos.indexOf(photo));
                    return;
                }
            }
        }
    }

    /**
     * Change which photo is being shown. If photo is null, clear the current photo.
     * TODO change the url to match selected photo
     */
    protected void setPhoto (final int photoIndex)
    {
        _currentPhoto.clear();
        _currentPhotoIndex = photoIndex;

        if (photoIndex < 0 || photoIndex >= _galleryData.photos.size()) {
            remove(_currentPhoto);
            add(_photoPanel);
            return;
        }
        Photo photo = _galleryData.photos.get(photoIndex);
        remove(_photoPanel);
        add(_currentPhoto);

        // photo name
        _currentPhoto.add(MsoyUI.createLabel(photo.name, "FullPhotoName"), 10, 0);

        // prev and next buttons
        ClickListener onPrev = new ClickListener() {
            public void onClick (Widget sender) {
                setPhoto(photoIndex == 0 ? _galleryData.photos.size() - 1 : photoIndex - 1);
            }
        };
        ClickListener onNext = new ClickListener() {
            public void onClick (Widget sender) {
                setPhoto((photoIndex + 1) % _galleryData.photos.size());
            }
        };
        _currentPhoto.add(MsoyUI.createPrevNextButtons(onPrev, onNext), 560, 0);

        // determine the width of the most constrained side and override the media constraint
        int width = photo.photoWidth;
        int height = photo.photoHeight;
        if (width > MAX_PHOTO_WIDTH) {
            width = MAX_PHOTO_WIDTH;
            height = Math.round((MAX_PHOTO_WIDTH / (float)photo.photoWidth) * photo.photoHeight);
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
                setPhoto(-1);
            }
        };
        Widget fullPhoto = MediaUtil.createMediaView(photo.photoMedia, width, height, onClick);
        _currentPhoto.add(MsoyUI.createSimplePanel(fullPhoto, "FullPhoto"), 0, 35);
    }

    /**
     * Start the slideshow immediately at the first photo or next photo if one is being shown.
     */
    protected void startSlideshow ()
    {
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
        if (_currentPhotoIndex < 0) {
            setPhoto(0);
        } else {
            setPhoto((_currentPhotoIndex + 1) % _galleryData.photos.size());
        }
        _slideshowTimer.schedule(5000);
    }

    /**
     * Halt the slideshow and return to the list of photos.
     */
    protected void stopSlideshow ()
    {
        _slideshowTimer.cancel();
        _slideshowTimer = null;
        setPhoto(-1);
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);

    /** List of photos and gallery details */
    protected GalleryData _galleryData;

    /** The index of the photo currently being displayed, used for slideshow */
    protected int _currentPhotoIndex;

    /** Fires every 5 seconds while slideshow is running */
    protected Timer _slideshowTimer;

    /** List of photos; hidden while viewing one photo in full */
    protected FlowPanel _photoPanel;

    /** Panel to display the photo currently being displayed in full */
    protected AbsolutePanel _currentPhoto;

    /** If this is a profile gallery, whose profile is this */
    protected int _profileMemberId = 0;

    protected static int MAX_PHOTO_WIDTH = 600;
    protected static int MAX_PHOTO_HEIGHT = 400;
}
