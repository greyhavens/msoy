//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * Handles the uploading of digital media for later use by a digital item.
 */
public class UploadServlet extends HttpServlet
{
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        int length = req.getContentLength();
        if (length <= 0) {
            rsp.sendError(HttpServletResponse.SC_LENGTH_REQUIRED);
            return;
        }
        if (length > MAX_UPLOAD_SIZE) {
            rsp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }

        // TODO: create a custom file item factory that just puts items in the
        // right place from the start and computes the SHA hash on the way
        ServletFileUpload upload =
            new ServletFileUpload(new DiskFileItemFactory());
        String mediaHash = "";
        try {
            for (Object obj : upload.parseRequest(req)) {
                FileItem item = (FileItem)obj;
                if (item.isFormField()) {
                    // do we care?
                } else {
                    // TODO: check that this is a supported content type
                    log.info("Receiving file [type: " + item.getContentType() +
                             ", size=" + item.getSize() + "].");
                    mediaHash = handleFileItem(item);
                }
            }

        } catch (FileUploadException e) {
            log.info("File upload choked: " + e + ".");
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // TODO: write out the magical incantations that are needed to cause
        // our magical little frame to communicate the newly assigned mediaHash
        // to the ItemEditor widget
        PrintStream out = new PrintStream(rsp.getOutputStream());
        try {
            out.println(mediaHash);
        } finally {
            StreamUtil.close(out);
        }
    }

    /**
     * Computes and returns the SHA hash of the supplied item and puts it in
     * the proper place in the media upload directory.
     */
    protected String handleFileItem (FileItem item)
        throws IOException
    {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create message digest.", e);
            return null;
        }

        // first write the file to disk under a temporary name and compute its
        // digest in the process
        InputStream in = item.getInputStream();
        File output = File.createTempFile(
            "upload", ".tmp", ServerConfig.mediaDir);
        FileOutputStream out = null;
        try {
            byte[] buffer = new byte[UPLOAD_BUFFER_SIZE];
            out = new FileOutputStream(output);
            long length = item.getSize();
            while (length > 0) {
                int read = in.read(buffer);
                if (read == -1) {
                    break;
                }
                length -= read;
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }

        } finally {
            StreamUtil.close(out);
            StreamUtil.close(in);
        }

        // for now just use the client's suffix, but later we want to create
        // a suffix based on the content-type
        int didx = item.getName().lastIndexOf(".");
        String suff = (didx == -1) ? ".dat" : item.getName().substring(didx);

        // now name it using the digest value and the suffix
        String hash = StringUtil.hexlate(digest.digest());
        // TODO: turn XXXXXXX... into XX/XX/XXXX... to avoid freaking out the
        // file system with the amazing four hundred billion files
        File target = new File(ServerConfig.mediaDir, hash + suff);
        if (!output.renameTo(target)) {
            log.warning("Unable to rename uploaded file [temp=" + output +
                        ", perm=" + target + "].");
            return null;
        }

        return hash;
    }

    /** Prevent Captain Insano from showing up to fill our drives. */
    protected static final int MAX_UPLOAD_SIZE = 5 * 1024 * 1024;

    /** Le chunk! */
    protected static final int UPLOAD_BUFFER_SIZE = 4096;
}
