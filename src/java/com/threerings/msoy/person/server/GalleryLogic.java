//
// $Id$

package com.threerings.msoy.person.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.server.persist.GalleryInfoRecord;
import com.threerings.msoy.person.server.persist.GalleryRepository;
import com.threerings.msoy.web.gwt.ServiceException;

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
        return Lists.newArrayList(Lists.transform(
            _galleryRepo.loadGalleries(memberId), GalleryInfoRecord.TO_GALLERY));
    }

    @Inject protected GalleryRepository _galleryRepo;
}
