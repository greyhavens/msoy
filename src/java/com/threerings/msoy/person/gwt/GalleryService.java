//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides gallery related functionality.
 *
 * @author mdb
 * @author mjensen
 */
public interface GalleryService
{
    /**
     * Loads all of the galleries belonging to the given member ID.
     */
    public List<Gallery> loadGalleries (int memberId)
        throws ServiceException;

    /**
     * Gets the special "Photos of Me" gallery for the given member ID.
     */
    public List<Photo> loadMeGallery (int memberId)
        throws ServiceException;

    /**
     * Gets all of the photos in the given gallery.
     */
    public List<Photo> loadGallery (int galleryId)
        throws ServiceException;

    /**
     * Creates a new gallery for the current user and adds the given photo IDs to the gallery.
     *
     * @return the galleryID for the new gallery.
     */
    public int createGallery (String name, List<Integer> photoItemIds)
        throws ServiceException;

    /**
     * Updates the name and photo IDs for the indicated gallery.
     */
    public void updateGallery (int galleryId, String name, List<Integer> photoItemIds)
        throws ServiceException;

    /**
     * Deletes the specified gallery.
     */
    public void deleteGallery (int galleryId)
        throws ServiceException;
}
