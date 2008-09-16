//
// $Id$

package com.threerings.msoy.person.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.PrimitiveArrays;
import com.google.inject.Inject;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.server.persist.GalleryInfoRecord;
import com.threerings.msoy.person.server.persist.GalleryRecord;
import com.threerings.msoy.person.server.persist.GalleryRepository;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Implements the {@link GalleryService}.
 *
 * @author mjensen
 */
public class GalleryServlet extends MsoyServiceServlet
    implements GalleryService
{
    // from GalleryService
    public int createGallery (String name, List<Integer> photoItemIds)
        throws ServiceException
    {
        // TODO Auto-generated method stub
        return 0; // return the ID of the new gallery
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
        List<Gallery> list = Lists.newArrayList();
        // load records and convert them to their pojo form
        list.addAll(Lists.transform(_galleryRepo.loadGalleries(memberId), GalleryInfoRecord.TO_GALLERY));
        return list;
    }

    // from GalleryService
    public List<Photo> loadGallery (int galleryId)
        throws ServiceException
    {
        return loadGallery(_galleryRepo.loadGallery(galleryId));
    }

    // from GalleryService
    public List<Photo> loadMeGallery (int memberId)
        throws ServiceException
    {
        return loadGallery(_galleryRepo.loadMeGallery(memberId));
    }

    protected List<Photo> loadGallery (GalleryRecord gallery)
        throws ServiceException
    {
        ItemRepository<ItemRecord> photoRepo = _itemLogic.getRepository(Item.PHOTO);
        return Lists.transform(photoRepo.loadItems(PrimitiveArrays.asList(gallery.photoItemIds)),
                               new ItemRecord.ToItem<Photo>());
    }

    // from GalleryService
    public void updateGallery (int galleryId, String name, List<Integer> photoItemIds)
        throws ServiceException
    {
        // TODO Auto-generated method stub

    }

    @Inject protected ItemLogic _itemLogic;
    @Inject protected GalleryRepository _galleryRepo;
}
