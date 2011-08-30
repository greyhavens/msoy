//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Provides the asynchronous version of {@link GalleryService}.
 */
public interface GalleryServiceAsync
{
    /**
     * The async version of {@link GalleryService#loadGalleries}.
     */
    void loadGalleries (int memberId, AsyncCallback<GalleryListData> callback);

    /**
     * The async version of {@link GalleryService#loadMeGallery}.
     */
    void loadMeGallery (int memberId, AsyncCallback<GalleryData> callback);

    /**
     * The async version of {@link GalleryService#createGallery}.
     */
    void createGallery (Gallery gallery, List<Integer> photoItemIds, AsyncCallback<Gallery> callback);

    /**
     * The async version of {@link GalleryService#loadGallery}.
     */
    void loadGallery (int galleryId, AsyncCallback<GalleryData> callback);

    /**
     * The async version of {@link GalleryService#updateGallery}.
     */
    void updateGallery (Gallery gallery, List<Integer> photoItemIds, AsyncCallback<Void> callback);

    /**
     * The async version of {@link GalleryService#deleteGallery}.
     */
    void deleteGallery (int galleryId, AsyncCallback<Void> callback);
}
