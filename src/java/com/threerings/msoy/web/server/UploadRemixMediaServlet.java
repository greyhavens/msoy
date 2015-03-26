//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.fileupload.FileUploadException;

import com.samskivert.io.StreamUtil;

// import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.server.MediaDescFactory;
import com.threerings.msoy.web.server.UploadUtil.MediaInfo;

/**
 * Just accepts a file upload and shoots it over to S3, returning the hash and mimetype
 * info so that the caller can re-download it for inclusion in the remix.
 */
public class UploadRemixMediaServlet extends AbstractUploadServlet
{
    @Override
    protected void handleFileItems (UploadContext ctx)
        throws IOException, FileUploadException, AccessDeniedException
    {
        UploadFile uploadFile = new FileItemUploadFile(ctx.file);

        String hash = uploadFile.getHash();
        checkBlacklist(hash);

        MediaInfo info = new MediaInfo(hash, uploadFile.getMimeType());

        // upload it to S3
        UploadUtil.publishUploadFile(uploadFile);

        // // acquire a signed description of the file
        // CloudfrontMediaDesc desc = MediaDescFactory.createMediaDesc(
        //     info.hash, info.mimeType, info.constraint);

        // tell the remixer the hash and mimetype, so that it can find the file
        ctx.rsp.setContentType("text/plain");
        PrintWriter out = ctx.rsp.getWriter();
        try {
            out.println(info.hash + " " + info.mimeType);
            // out.println(info.hash + " " + info.mimeType + " " + desc.getExpiration()
            //     + " " + desc.getSignature());
        } finally {
            StreamUtil.close(out);
        }
    }

    @Override
    protected int getMaxUploadSize ()
    {
        return 10 * MEGABYTE;
    }
}
