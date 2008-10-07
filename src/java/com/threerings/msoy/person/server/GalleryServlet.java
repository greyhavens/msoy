//
// $Id$

package com.threerings.msoy.person.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.PrimitiveArrays;
import com.google.inject.Inject;

import com.samskivert.util.IntIntMap;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.PhotoRepository;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryData;
import com.threerings.msoy.person.gwt.GalleryListData;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.ProfileCodes;
import com.threerings.msoy.person.server.persist.GalleryRecord;
import com.threerings.msoy.person.server.persist.GalleryRepository;

import static com.threerings.msoy.Log.log;

/**
 * Implements the {@link GalleryService}.
 *
 * @author mjensen
 */
public class GalleryServlet extends MsoyServiceServlet
    implements GalleryService
{
    // from GalleryService
    public Gallery createGallery (Gallery gallery, List<Integer> photoItemIds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        // players can only have one "me" gallery with a null name, so check for existance
        if (gallery.name == null) {
            GalleryRecord existingMeGallery = _galleryRepo.loadMeGallery(memrec.memberId);
            if (existingMeGallery != null) {
                // if somehow a second me gallery is being created, instead overwrite the first
                gallery.galleryId = existingMeGallery.galleryId;
                updateGallery(gallery, photoItemIds);
                return gallery;
            }
        }

        // only add photos that the member owns
        photoItemIds.removeAll(validateOwnership(memrec.memberId, photoItemIds));

        return _galleryRepo.insertGallery(
            memrec.memberId, gallery, PrimitiveArrays.toIntArray(photoItemIds)).toGallery();
    }

    // from GalleryService
    public void updateGallery (Gallery gallery, List<Integer> photoItemIds)
        throws ServiceException
    {
        // load the existing gallery record
        GalleryRecord existingGallery = _galleryRepo.loadGallery(gallery.galleryId);

        // check whether gallery exists
        if (existingGallery == null) {
            log.warning("Gallery does not exist.", "galleryId", gallery.galleryId);
            throw new ServiceException(ProfileCodes.E_GALLERY_DOES_NOT_EXIST);
        }

        MemberRecord member = requireAuthedUser();
        if (existingGallery.ownerId != member.memberId) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        // photos already added to the gallery are known to be valid
        List<Integer> newPhotoIds = Lists.newArrayList(photoItemIds);
        newPhotoIds.removeAll(PrimitiveArrays.asList(existingGallery.photoItemIds));
        // remove any rejects
        photoItemIds.removeAll(validateOwnership(member.memberId, newPhotoIds));

        _galleryRepo.updateGallery(gallery, PrimitiveArrays.toIntArray(photoItemIds));
    }

    // from GalleryService
    public void deleteGallery (int galleryId)
        throws ServiceException
    {
        _galleryRepo.deleteGallery(galleryId);
    }

    // from GalleryService
    public GalleryListData loadGalleries (int memberId)
        throws ServiceException
    {
        GalleryListData data = new GalleryListData();
        data.owner = _memberRepo.loadMemberName(memberId);
        if (data.owner == null) {
            throw new ServiceException(ProfileCodes.E_MEMBER_DOES_NOT_EXIST);
        }
        data.galleries = _galleryLogic.loadGalleries(memberId);
        return data;
    }

    // from GalleryService
    public GalleryData loadGallery (int galleryId)
        throws ServiceException
    {
        return loadGalleryData(_galleryRepo.loadGallery(galleryId));
    }

    // from GalleryService
    public GalleryData loadMeGallery (int memberId)
        throws ServiceException
    {
        return loadGalleryData(_galleryRepo.loadMeGallery(memberId));
    }

    /**
     * Build and return the GalleryData object containing both gallery details and photos
     */
    protected GalleryData loadGalleryData (GalleryRecord galleryRecord)
        throws ServiceException
    {
        if (galleryRecord == null) {
            return null;
        }
        GalleryData data = new GalleryData();
        data.gallery = galleryRecord.toGallery();
        // the photos list must be an ArrayList otherwise it gets cranky when serialized
        data.photos = Lists.newArrayList(Lists.transform(
            _photoRepo.loadItemsInOrder(PrimitiveArrays.asList(galleryRecord.photoItemIds)),
            new ItemRecord.ToItem<Photo>()));
        data.owner = _memberRepo.loadMemberName(galleryRecord.ownerId);
        return data;
    }

    /**
     * Finds Photo item IDs that the given member does not own.
     * @return the list of rejected photo IDs that the member does not own.
     */
    protected List<Integer> validateOwnership (int memberId, List<Integer> photoItemIds)
    {
        List<Integer> rejects = Lists.newArrayList();
        if (!photoItemIds.isEmpty()) {
            IntIntMap ownerMap = _photoRepo.loadOwnerIds(photoItemIds);
            for (int photoId : ownerMap.getKeys()) {
                if (memberId != ownerMap.get(photoId)) {
                    rejects.add(photoId);
                    log.warning("Member tried to add a photo that they do not own to a gallery.",
                                "memberId", memberId, "photoItemId", photoId);
                }
            }
        }
        return rejects;
    }

    @Inject protected GalleryRepository _galleryRepo;
    @Inject protected PhotoRepository _photoRepo;
    @Inject protected GalleryLogic _galleryLogic;
}
