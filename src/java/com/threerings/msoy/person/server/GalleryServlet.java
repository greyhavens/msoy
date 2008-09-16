//
// $Id$

package com.threerings.msoy.person.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.PrimitiveArrays;
import com.google.inject.Inject;

import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.PhotoRepository;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.server.persist.GalleryInfoRecord;
import com.threerings.msoy.person.server.persist.GalleryRecord;
import com.threerings.msoy.person.server.persist.GalleryRepository;

/**
 * Implements the {@link GalleryService}.
 *
 * @author mjensen
 */
public class GalleryServlet extends MsoyServiceServlet
    implements GalleryService
{
    // from GalleryService
    public Gallery createGallery (String name, List<Integer> photoItemIds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        return _galleryRepo.insertGallery(
            memrec.memberId, name, PrimitiveArrays.toIntArray(photoItemIds)).toGallery();
    }

    // from GalleryService
    public void updateGallery (int galleryId, String name, List<Integer> photoItemIds)
        throws ServiceException
    {
        _galleryRepo.updateGallery(galleryId, name, PrimitiveArrays.toIntArray(photoItemIds));
    }

    // from GalleryService
    public void deleteGallery (int galleryId)
        throws ServiceException
    {
        _galleryRepo.deleteGallery(galleryId);
    }

    // from GalleryService
    public List<Gallery> loadGalleries (int memberId)
        throws ServiceException
    {
        // load records and convert them to their pojo form
        return Lists.newArrayList(Lists.transform(_galleryRepo.loadGalleries(memberId),
                                                  GalleryInfoRecord.TO_GALLERY));
    }

    // from GalleryService
    public List<Photo> loadGallery (int galleryId)
        throws ServiceException
    {
        return loadPhotos(_galleryRepo.loadGallery(galleryId));
    }

    // from GalleryService
    public List<Photo> loadMeGallery (int memberId)
        throws ServiceException
    {
        return loadPhotos(_galleryRepo.loadMeGallery(memberId));
    }

    protected List<Photo> loadPhotos (GalleryRecord gallery)
        throws ServiceException
    {
        return Lists.transform(_photoRepo.loadItems(PrimitiveArrays.asList(gallery.photoItemIds)),
                               new ItemRecord.ToItem<Photo>());
    }

    @Inject protected GalleryRepository _galleryRepo;
    @Inject protected PhotoRepository _photoRepo;
}
