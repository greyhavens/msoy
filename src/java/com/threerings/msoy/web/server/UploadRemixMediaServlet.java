//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

import com.samskivert.io.StreamUtil;

import com.threerings.msoy.web.server.UploadUtil.MediaInfo;

public class UploadRemixMediaServlet extends AbstractUploadServlet
{
    @Override
    protected void handleFileItems (
        FileItem item, FileItem[] allItems, int uploadLength,
        HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, FileUploadException, AccessDeniedException
    {
        // TODO: check auth

        UploadFile uploadFile = new FileItemUploadFile(item);
        MediaInfo info = new MediaInfo(uploadFile.getHash(), uploadFile.getMimeType());

        // upload it to S3
        UploadUtil.publishUploadFile(uploadFile);

        // tell the remixer the hash and mimetype, so that it can find the file
        rsp.setContentType("text/plain");
        PrintWriter out = rsp.getWriter();
        try {
            out.println(info.hash + " " + info.mimeType);
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
