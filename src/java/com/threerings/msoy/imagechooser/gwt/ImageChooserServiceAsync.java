//
// $Id$

package com.threerings.msoy.imagechooser.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.Photo;

/**
 * Provides the asynchronous version of {@link ImageChooserService}.
 */
public interface ImageChooserServiceAsync
{
    /**
     * The async version of {@link ImageChooserService#loadPhotos}.
     */
    void loadPhotos (AsyncCallback<List<Photo>> callback);
}
