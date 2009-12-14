//
// $Id$

package com.threerings.msoy.imagechooser.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.server.persist.PhotoRecord;
import com.threerings.msoy.item.server.persist.PhotoRepository;

import com.threerings.msoy.imagechooser.gwt.ImageChooserService;

public class ImageChooserServlet extends MsoyServiceServlet
    implements ImageChooserService
{
    @Override // from interface ImageChooserService
    public List<Photo> loadPhotos ()
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        List<Photo> photos = Lists.newArrayList();
        for (PhotoRecord record : _photoRepo.loadOriginals(memrec.memberId)) {
            photos.add((Photo)record.toItem());
        }
        for (PhotoRecord record : _photoRepo.loadClones(memrec.memberId)) {
            photos.add((Photo)record.toItem());
        }
        Collections.sort(photos);
        return photos;
    }

    @Inject protected PhotoRepository _photoRepo;
}
