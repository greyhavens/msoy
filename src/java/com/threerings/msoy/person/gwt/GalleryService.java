//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Provides gallery related functionality.
 *
 * @author mdb
 * @author mjensen
 */
@RemoteServiceRelativePath(GalleryService.REL_PATH)
public interface GalleryService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/gallerysvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + GalleryService.ENTRY_POINT;

    /**
     * Loads all of the galleries belonging to the given member ID.
     */
    public GalleryListData loadGalleries (int memberId)
        throws ServiceException;

    /**
     * Gets the special "Photos of Me" gallery for the given member ID.
     */
    public GalleryData loadMeGallery (int memberId)
        throws ServiceException;

    /**
     * Gets all of the photos in the given gallery.
     */
    public GalleryData loadGallery (int galleryId)
        throws ServiceException;

    /**
     * Creates a new gallery for the current user and adds the given photo IDs to the gallery.
     *
     * @return the new gallery.
     */
    public Gallery createGallery (Gallery gallery, List<Integer> photoItemIds)
        throws ServiceException;

    /**
     * Updates the name and photo IDs for the indicated gallery.
     */
    public void updateGallery (Gallery gallery, List<Integer> photoItemIds)
        throws ServiceException;

    /**
     * Deletes the specified gallery.
     */
    public void deleteGallery (int galleryId)
        throws ServiceException;
}
