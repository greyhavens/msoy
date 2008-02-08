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

import static com.threerings.msoy.Log.log;

public class EchoUploadServlet extends AbstractUploadServlet
{
    @Override
    protected void handleFileItems (
        FileItem item, FileItem[] allItems, int uploadLength,
        HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, FileUploadException, AccessDeniedException
    {
        // TODO: auth?

        String encoded = new String(Base64.encodeBase64(item.get()));
        rsp.setContentType("text/plain");
        rsp.setContentLength(encoded.length());

        PrintWriter out = rsp.getWriter();
        try {
            out.print(encoded);
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
