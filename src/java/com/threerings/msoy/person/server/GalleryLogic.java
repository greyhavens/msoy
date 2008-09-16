//
// $Id$

package com.threerings.msoy.person.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.server.persist.PhotoRecord;
import com.threerings.msoy.item.server.persist.PhotoRepository;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.server.persist.GalleryRecord;
import com.threerings.msoy.person.server.persist.GalleryRepository;
import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides shared Gallery related methods.
 */
@Singleton @BlockingThread
public class GalleryLogic
{
    // from GalleryService
    public List<Gallery> loadGalleries (int memberId)
        throws ServiceException
    {
        List<Gallery> galleries = new ArrayList<Gallery>();

        // fetch the gallery thumbnails and create the serializable Gallery records
        for (GalleryRecord record : _galleryRepo.loadGalleries(memberId)) {
            Gallery gallery = record.toGallery();

            // use the first photo as the thumbnail for each gallery
            PhotoRecord firstPhoto = _photoRepo.loadItem(record.photoItemIds[0]);
            gallery.thumbMedia = new MediaDesc(firstPhoto.thumbMediaHash,
                firstPhoto.thumbMimeType, firstPhoto.thumbConstraint);
            galleries.add(gallery);
        }

        // return the galleries in order of the most recent modification
        Collections.sort(galleries, new Comparator<Gallery>() {
            public int compare (Gallery gallery1, Gallery gallery2) {
                return gallery2.lastModified.compareTo(gallery1.lastModified);
            }
        });

        return galleries;
    }

    @Inject protected GalleryRepository _galleryRepo;
    @Inject protected PhotoRepository _photoRepo;
}
