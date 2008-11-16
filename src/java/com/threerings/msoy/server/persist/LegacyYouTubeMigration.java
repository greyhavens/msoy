//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.HashMap;

import com.google.inject.Inject;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DataMigration;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.item.server.persist.VideoRecord;
import com.threerings.msoy.item.server.persist.VideoRepository;

import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneFurniRecord;

import com.threerings.msoy.stuff.server.ExternalUploadFile;
import com.threerings.msoy.web.server.UploadUtil;

import static com.threerings.msoy.Log.log;

// TODO: this whole class can be removed after 2008-11-12
public class LegacyYouTubeMigration extends DataMigration
{
    public LegacyYouTubeMigration ()
    {
        super("2008_11_12_convert_legacy_youtube");
    }

    @Override public void invoke ()
        throws DatabaseException
    {
        int videoCount = 0;
        int furniCount = 0;

        for (VideoRecord vid : _videoRepo.loadLegacyYouTube()) {
            vid.videoMediaHash = convertMedia(vid.videoMediaHash);
            vid.videoMimeType = MediaDesc.EXTERNAL_YOUTUBE;
            _videoRepo.updateOriginalItem(vid, false);
            videoCount++;
        }

        for (SceneFurniRecord furni : _sceneRepo.loadLegacyYouTube()) {
            furni.mediaHash = convertMedia(furni.mediaHash);
            furni.mediaType = MediaDesc.EXTERNAL_YOUTUBE;
            _sceneRepo.updateLegacyYouTube(furni);
            furniCount++;
        }

        log.info("Converted legacy youtube videos", "videos", videoCount, "furni", furniCount,
            "uploadsToS3", _filesUploaded);
    }

    protected byte[] convertMedia (byte[] oldhash)
    {
        String id = decodeId(oldhash);
        byte[] newHash = _hashes.get(id);
        if (newHash != null) {
            return newHash;
        }

        String data = "id=" + StringUtil.encode(id);
        ExternalUploadFile file = new ExternalUploadFile(data, MediaDesc.EXTERNAL_YOUTUBE);
        try {
            UploadUtil.publishUploadFile(file);
            _filesUploaded++;

            newHash = StringUtil.unhexlate(file.getHash());
            _hashes.put(id, newHash);
            return newHash;

        } catch (Throwable ioe) {
            log.warning("Unable to publish external media file", ioe);
            throw new DatabaseException(ioe);
        }
    }

    protected String decodeId (byte[] bytes)
    {
        char[] chars = new char[bytes.length];
        for (int ii = 0; ii < bytes.length; ii++) {
            chars[ii] = (char) bytes[ii];
        }
        return new String(chars);
    }

    protected int _filesUploaded;

    protected HashMap<String, byte[]> _hashes = new HashMap<String, byte[]>();

    @Inject protected MsoySceneRepository _sceneRepo;

    @Inject protected VideoRepository _videoRepo;
}
