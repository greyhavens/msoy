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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

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
public class GalleryViewPanel extends AbsolutePanel
{
    public static final String VIEW_ACTION = "gallery";
    public static final String VIEW_PROFILE_ACTION = "profileGallery";

    /**
     * Expects either galleryId if displaying a specific gallery, or a memberId if displaying that
     * member's profile gallery.
     * @param galleryId
     * @param memberId
     */
    public GalleryViewPanel (final int galleryId, final int memberId)
    {
        addStyleName("galleryViewPanel");

        if (galleryId > 0) {
        _gallerysvc.loadGallery(galleryId, new MsoyCallback<GalleryData>() {
            public void onSuccess (GalleryData galleryData) {
                displayGallery(galleryData);
            }
        });
        } else {
            _profileMemberId = memberId;
            _gallerysvc.loadMeGallery(memberId, new MsoyCallback<GalleryData>() {
                public void onSuccess (GalleryData galleryData) {
                    displayGallery(galleryData);
                }
            });
        }
    }

    /**
     * If galleryData contains a list of images, display the gallery details and thumbnails for
     * those images, otherwise display an error and/or edit button.
     */
    protected void displayGallery (GalleryData galleryData)
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
            if (galleryData.ownerId == CShell.getMemberId()) {
                error.add(new HTML(_pmsgs.galleryNoPhotosSelf()));
                final String args = Args.compose(GalleryEditPanel.EDIT_ACTION,
                    galleryData.gallery.galleryId);
                final ClickListener listener = Link.createListener(Pages.PEOPLE, args);
                error.add(MsoyUI.createActionLabel(_pmsgs.galleryAddPhotos(), listener));
            } else {
                error.add(new HTML(_pmsgs.galleryNoPhotosOther()));
            }
        }
        if (error.getWidgetCount() > 0) {
            add(error);
            return;
        }
        _galleryData = galleryData;

        // add read-only gallery detail panel
        add(new GalleryDetailPanel(galleryData, true), 0, 0);

        FlowPanel photoPanel = new FlowPanel();
        for (int i = 0; i < galleryData.photos.size(); i++) {
            final int photoIndex = i;
            ClickListener thumbClickListener = new ClickListener() {
                public void onClick (Widget sender) {
                    setPhoto(photoIndex);
                }
            };

            // use a table to center image vertically
            Photo photo = galleryData.photos.get(i);
            SmartTable thumbnail = new SmartTable("Thumbnail", 0, 0);
            thumbnail.addWidget(MediaUtil.createMediaView(photo.thumbMedia,
                MediaDesc.THUMBNAIL_SIZE, thumbClickListener), 0, null);
            thumbnail.getCellFormatter().setHeight(0, 0, MediaDesc.THUMBNAIL_HEIGHT + "px");
            thumbnail.getCellFormatter().setWidth(0, 0, MediaDesc.THUMBNAIL_WIDTH + "px");
            thumbnail.getCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_MIDDLE);
            thumbnail.getCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            thumbnail.addWidget(MsoyUI.createLabel(photo.name, "Name"), 0, null);
            photoPanel.add(thumbnail);
        }
        add(photoPanel, 225, 0);

        // this will be filled with the photos
        _currentPhoto = new AbsolutePanel();
        _currentPhoto.addStyleName("CurrentPhoto");
    }

    /**
     * Change which photo is being shown. If photo is null, just clear the current photo.
     */
    protected void setPhoto (final int photoIndex)
    {
        _currentPhoto.clear();

        if (photoIndex < 0 || photoIndex >= _galleryData.photos.size()) {
            remove(_currentPhoto);
            return;
        }
        Photo photo = _galleryData.photos.get(photoIndex);

        // grey out the bg
        _currentPhoto.add(MsoyUI.createFlowPanel("GreyMask"), 0, 0);

        // determine the width of the most constrained side and override the media constraint
        int width = photo.photoWidth;
        int height = photo.photoHeight;
        if (width > MAX_PHOTO_WIDTH) {
            width = MAX_PHOTO_WIDTH;
            height = (photo.photoWidth / MAX_PHOTO_WIDTH) * photo.photoHeight;
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

        // Image name, photo and description will be centered
        FlowPanel fullSizeContainer = MsoyUI.createFlowPanel("FullSize");
        Widget fullSize = MediaUtil.createMediaView(photo.photoMedia, width, height, onClick);
        fullSize.addStyleName("Photo");
        fullSizeContainer.add(fullSize);
        fullSizeContainer.add(MsoyUI.createLabel(photo.name, "Name"));
        fullSizeContainer.add(MsoyUI.createLabel(photo.description, "Description"));
        _currentPhoto.add(fullSizeContainer, 0, 0);

        // either a start or stop slideshow button depending on if it is running.
        if (_slideshowTimer == null) {
            ClickListener slideshowClick = new ClickListener() {
                public void onClick (Widget sender) {
                    startSlideshow(photoIndex);
                }
            };
            _currentPhoto.add(new Button(_pmsgs.slideshowStart(), slideshowClick), 240, 520);
        } else {
            ClickListener slideshowClick = new ClickListener() {
                public void onClick (Widget sender) {
                    stopSlideshow();
                }
            };
            _currentPhoto.add(new Button(_pmsgs.slideshowStop(), slideshowClick), 260, 520);
        }

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
        _currentPhoto.add(MsoyUI.createPrevNextButtons(onPrev, onNext), 330, 520);

        add(_currentPhoto, 0, 0);
    }

    /**
     * Starts the slideshow immediately at a given photo index.
     */
    protected void startSlideshow (final int photoIndex)
    {
        _slideshowIndex = photoIndex;
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
        _slideshowIndex = (_slideshowIndex + 1) % _galleryData.photos.size();
        setPhoto(_slideshowIndex);
        _slideshowTimer.schedule(5000);
    }

    /**
     * Halt the slideshow, and reset the slideshow button by setting the photo one more time.
     */
    protected void stopSlideshow ()
    {
        _slideshowTimer.cancel();
        _slideshowTimer = null;
        setPhoto(_slideshowIndex);
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
    protected static final GalleryServiceAsync _gallerysvc = (GalleryServiceAsync)
        ServiceUtil.bind(GWT.create(GalleryService.class), GalleryService.ENTRY_POINT);

    /** List of photos and gallery details */
    protected GalleryData _galleryData;

    /** Current photo index for the slideshow */
    protected int _slideshowIndex;

    /** Fires every 5 seconds while slideshow is running */
    protected Timer _slideshowTimer;

    /** Panel to display the photo currently being displayed in full */
    protected AbsolutePanel _currentPhoto;

    /** If this is a profile gallery, whose profile is this */
    protected int _profileMemberId = 0;

    protected static int MAX_PHOTO_WIDTH = 400;
    protected static int MAX_PHOTO_HEIGHT = 400;
}
