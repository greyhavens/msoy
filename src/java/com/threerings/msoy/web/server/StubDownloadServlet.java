//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jswiff.SWFDocument;
import com.jswiff.SWFReader;
import com.jswiff.SWFWriter;
import com.jswiff.listeners.SWFDocumentReader;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.UnknownTag;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * Allows users to download a stub that connects to whirled with the desired args.
 */
public class StubDownloadServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        // TODO: security: only allow room manager or game owner to create a stub

        String args = req.getParameter("args");
        //System.err.println("Setting up stub with args: " + args);

        SWFReader reader = new SWFReader(
            new FileInputStream(new File(ServerConfig.serverRoot, STUB_LOCATION)));
        SWFDocumentReader docReader = new SWFDocumentReader();
        reader.addListener(docReader);
        reader.read();
        SWFDocument doc = docReader.getDocument();

        if (modifySWF(doc, STUB_TOKEN, args) &&
                modifySWF(doc, STUB_URL, DeploymentConfig.serverURL)) {
            rsp.setContentType("application/x-shockwave-flash");
            SWFWriter writer = new SWFWriter(doc, rsp.getOutputStream());
            writer.write();

        } else {
            log.warning("Unable to replace args in stub!");
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Modify the specified swf.
     */
    protected static boolean modifySWF (SWFDocument doc, String before, String after)
    {
        @SuppressWarnings("unchecked")
        List<Tag> tags = doc.getTags();
        for (int ii = 0, nn = tags.size(); ii < nn; ii++) {
            Tag tag = tags.get(ii);
            if (tag instanceof UnknownTag) {
                Tag replacement = modifyTag((UnknownTag) tag, before, after);
                if (replacement != null) {
                    tags.set(ii, replacement);
                    return true;
                }
            }
        }
        return false;
    }

    protected static UnknownTag modifyTag (UnknownTag tag, String before, String after)
    {
        byte[] data = tag.getData();
        byte[] bef = makeStringBytes(before);

        int index = indexOf(data, bef);
        if (index == -1) {
            return null;
        }

        byte[] aft = makeStringBytes(after);
        byte[] newData = new byte[data.length - bef.length + aft.length];

        System.arraycopy(data, 0, newData, 0, index);
        System.arraycopy(aft, 0, newData, index, aft.length);
        System.arraycopy(data, index + bef.length, newData, index + aft.length,
            data.length - (index + bef.length));
        return new UnknownTag((short)tag.getCode(), newData);
    }

    /**
     * Find the specified byte[] in the larger byte[].
     */
    protected static int indexOf (byte[] haystack, byte[] needle)
    {
        int mm = needle.length;
        int nn = haystack.length - mm + 1;
        for (int ii = 0; ii < nn; ii++) {
            for (int jj = 0; jj < mm; jj++) {
                if (haystack[ii + jj] != needle[jj]) {
                    break;
                } else if (jj == mm - 1) {
                    return ii; // found it
                }
            }
        }
        return -1; // never found it
    }

    /**
     * Create a byte[] representing the specified String prepended with the bytes
     * describing its length.
     */
    protected static byte[] makeStringBytes (String s)
    {   
        int len = s.length();
        if (len > 127) {
            // we could probably make this work by using more than 1 byte...
            throw new RuntimeException("Can't currently replace strings that length.");
        }
        
        byte[] result = new byte[len + 1];
        result[0] = (byte) len;
        System.arraycopy(s.getBytes(), 0, result, 1, len);
        return result;
    }

    protected static final String STUB_URL = "//stuburl//";

    /** The token we're looking to replace in the swf. */
    protected static final String STUB_TOKEN = "&&stubargs&&";

    /** The location of the raw stub. */
    protected static final String STUB_LOCATION = "/dist/embedstub-base.swf";
}
