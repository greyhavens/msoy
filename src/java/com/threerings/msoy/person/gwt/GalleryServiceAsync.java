//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link GalleryService}.
 */
public interface GalleryServiceAsync
{
    /**
     * The asynchronous version of {@link GalleryService#loadGalleries}.
     */
    void loadGalleries (int memberId,
                        AsyncCallback<List<Gallery>> callback);

    /**
     * The asynchronous version of {@link GalleryService#loadMeGallery}.
     */
    void loadMeGallery (int memberId,
                        AsyncCallback<GalleryData> callback);

    /**
     * The asynchronous version of {@link GalleryService#loadGallery}.
     */
    void loadGallery (int galleryId,
                      AsyncCallback<GalleryData> callback);

    /**
     * The asynchronous version of {@link GalleryService#createGallery}.
     */
    void createGallery (Gallery gallery, List<Integer> photoItemIds,
                        AsyncCallback<Gallery> callback);

    /**
     * The asynchronous version of {@link GalleryService#updateGallery}.
     */
    void updateGallery (Gallery gallery, List<Integer> photoItemIds,
                        AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GalleryService#deleteGallery}.
     */
    void deleteGallery (int galleryId,
                        AsyncCallback<Void> callback);
}
