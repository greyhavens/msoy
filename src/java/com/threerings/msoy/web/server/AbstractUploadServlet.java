//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.common.collect.Maps;

import com.samskivert.io.StreamUtil;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.server.persist.MemberRecord;

import static com.threerings.msoy.Log.log;

/**
 * An abstract class to hold common UploadServlet functionality.
 */
public abstract class AbstractUploadServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        UploadContext ctx = new UploadContext(req, rsp);
        try {
            // validate the content length is sane
            // attempt to extract the FileItem from the servlet request and also verify the upload
            // is not larger than our maximum allowed size
            ctx.uploadLength = validateContentLength(req);
            ctx.allItems = extractFileItems(req);
            ctx.formFields = parseFormFields(ctx.allItems);
            ctx.file = findFile(ctx.allItems);
            validateAccess(ctx);

            if (ctx.file == null) {
                log.warning("Failed to extract file from upload request. [req= " + req + "].");
                internalError(rsp);
                return;
            }

            // pass the extracted file to the concrete class
            handleFileItems(ctx);

        } catch (ServletFileUpload.SizeLimitExceededException slee) {
            log.info(slee.getMessage() + " [size=" + slee.getActualSize() + " allowed=" +
                slee.getPermittedSize() + "].");
            uploadTooLarge(rsp);
            return;

        } catch (FileUploadException fue) {
            log.info("File upload failed [error=" + fue.getMessage() + "].");
            internalError(rsp);
            return;

        } catch (AccessDeniedException ade) {
            log.info("Access denied during upload [error=" + ade + "].");
            accessDenied(rsp);
            return;

        } catch (IOException ioe) {
            log.warning("File upload choked during file i/o [error=" + ioe + "].");
            internalError(rsp);
            return;

        } finally {
            // delete the temporary upload file data.
            // items may be null if extractFileItem throws an exception
            if (ctx.allItems != null) {
                for (FileItem item : ctx.allItems) {
                    if (item != null) {
                        item.delete();
                    }
                }
            }
        }
    }

    /**
     * Validate that this user has permission to upload, and populate the MemberRecord
     * in the context, if desired.
     */
    protected void validateAccess (UploadContext ctx)
        throws AccessDeniedException
    {
        // no validation here
        // ctx.memrec = null;
    }

    /**
     * Handles the extracted UploadFile in a concrete class specific way.
     */
    protected abstract void handleFileItems (UploadContext ctx)
        throws IOException, FileUploadException, AccessDeniedException;

    /**
     * Returns the maximum size for an upload, in bytes.
     */
    protected abstract int getMaxUploadSize ();

    /**
     * Parse the upload request and return all FileItems found, whether they correspond
     * to actual files, or merely form fields. Returns an empty array if no FileItem was found.
     * @throws FileUploadException
     */
    protected FileItem[] extractFileItems (HttpServletRequest req)
        throws FileUploadException
    {
        ArrayList<FileItem> items = new ArrayList<FileItem>();

        // TODO: create a custom file item factory that just puts items in the right place from the
        // start and computes the SHA hash on the way
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(
            DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, ServerConfig.mediaDir));

        // enforce maximum sizes for the client supplied content size and the temporary file
        upload.setSizeMax(getMaxUploadSize());
        upload.setFileSizeMax(getMaxUploadSize());

        for (Object obj : upload.parseRequest(req)) {
            items.add((FileItem)obj);
        }
        return items.toArray(new FileItem[]{});
    }

    /**
     * Converts all uploaded form fields into a map of field names to field values.
     */
    protected Map<String, String> parseFormFields (FileItem[] items)
    {
        Map<String, String> fields = Maps.newHashMap();
        for (FileItem item : items) {
            try {
                if (item.isFormField()) {
                    String name = item.getFieldName();
//                    BufferedReader in =
//                        new BufferedReader(new InputStreamReader(item.getInputStream()));
//                    String value = in.readLine();
// TODO: check this? does getString() always work?
                    String value = item.getString("UTF-8");
                    fields.put(name, value);
                }
            } catch (IOException ie) {
                // just skip this item
            }
        }
        return fields;
    }

    /** Returns the first form element that actually contains a file. */
    protected FileItem findFile (FileItem[] items)
    {
        for (FileItem item : items) {
            if (! item.isFormField()) {
                return item;
            }
        }
        return null;
    }

    /**
     * Validates the content length of the supplied HttpServletRequest is set correctly.
     * @return the content length
     * @throws FileUploadException thrown if the content length is less and or equal to 0
     */
    protected int validateContentLength (HttpServletRequest req)
        throws FileUploadException
    {
        int length = req.getContentLength();
        if (length <= 0) {
            throw new FileUploadException("Invalid content length set. [length=" + length + "].");
        }
        return length;
    }

    /**
     * Displays an internal error message to the GWT client.
     */
    protected void internalError (HttpServletResponse rsp)
    {
        displayError(rsp, JavascriptError.UPLOAD_ERROR);
    }

    /**
     * Displays an error message that the upload was too large to the GWT client.
     */
    protected void uploadTooLarge (HttpServletResponse rsp)
    {
        displayError(rsp, JavascriptError.UPLOAD_TOO_LARGE);
    }

    /**
     * Displays an error message that the user is not authorized to perform this upload.
     */
    protected void accessDenied (HttpServletResponse rsp)
    {
        displayError(rsp, JavascriptError.ACCESS_DENIED);
    }

    /**
     * Calls the function from the supplied JavascriptError on the GWT side to display an
     * error message to the user.
     * @throws IOException
     */
    protected void displayError (HttpServletResponse rsp, JavascriptError error)
    {
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println("<html>");
            out.println("<head></head>");
            out.println("<body onLoad=\"parent." + error.function + "();\"></body>");
            out.println("</html>");

        } catch (IOException ioe) {
            log.warning("Failed to setup OutputStream when displaying error.", ioe);

        } finally {
            StreamUtil.close(out);
        }
    }

    /**
     * An exception encountered when a user is not authorized to perform an upload.
     */
    protected static class AccessDeniedException extends Exception
    {
        AccessDeniedException (String reason)
        {
            super(reason);
        }
    }

    /**
     * A holder for a bunch of the objects we pass around.
     * Your subclass may leave some of these fields null.
     */
    protected static class UploadContext
    {
        public HttpServletRequest req;

        public HttpServletResponse rsp;

        public int uploadLength;

        /** The member record. */
        public MemberRecord memrec;

        public FileItem file;

        public FileItem[] allItems;

        public Map<String, String> formFields;

        /** Arbitrary data you may store here. */
        public Object data;

        /** Construct */
        public UploadContext (HttpServletRequest req, HttpServletResponse rsp)
        {
            this.req = req;
            this.rsp = rsp;
        }
    }

    /**
     * Stores the GWT javascript functions used for various error messages.
     * Note: Any concrete class implementing AbstractUploadServlet must have these javascript
     * functions implemented in the GWT client which calls that servlet.
     */
    protected static enum JavascriptError {
        UPLOAD_ERROR("uploadError"),
        UPLOAD_TOO_LARGE("uploadTooLarge"),
        ACCESS_DENIED("accessDenied");

        public String function;

        JavascriptError (String function)
        {
            this.function = function;
        }
    }

    protected static final int MEGABYTE = 1024 * 1024;
}
