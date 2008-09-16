//
// $Id$

package com.threerings.msoy.person.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.server.persist.GalleryInfoRecord;
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
        List<Gallery> galleries = Lists.newArrayList(Lists.transform(
            _galleryRepo.loadGalleries(memberId), GalleryInfoRecord.TO_GALLERY));

        // return the galleries in order of the most recent modification
        Collections.sort(galleries, new Comparator<Gallery>() {
            public int compare (Gallery gallery1, Gallery gallery2) {
                return gallery2.lastModified.compareTo(gallery1.lastModified);
            }
        });

        return galleries;
    }

    @Inject protected GalleryRepository _galleryRepo;
}
