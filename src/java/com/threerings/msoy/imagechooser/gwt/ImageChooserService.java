//
// $Id$

package com.threerings.msoy.imagechooser.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Provides digital items related services.
 */
@RemoteServiceRelativePath(ImageChooserService.REL_PATH)
public interface ImageChooserService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/imgchoosersvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + ImageChooserService.ENTRY_POINT;

    /**
     * Loads up all of this member's photo inventory. This exists separate from
     * StuffService.loadInventory because we want to allow photo selection in many places in the
     * website, but we don't want to have to compile in the entire Item hiearchy to do so.
     */
    List<Photo> loadPhotos ()
        throws ServiceException;

}
