//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.CopyUtils;

import com.samskivert.io.StreamUtil;
import com.threerings.msoy.server.ServerConfig;

/**
 * Hackery to pass POST requests through the GWT shell and on to the actual
 * MSOY server which is running separately. This is only used during
 * development, in deployment the AJAX code just talks directly to the MSOY
 * server.
 */
public class RedirectServlet extends HttpServlet
{
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        redirect(req, rsp);
    }

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        redirect(req, rsp);
    }

    protected void redirect (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        InputStream in = null;
        OutputStream out = null;

        try {
            // reroute the URL to our local MSOY server
            URL requrl = new URL(req.getRequestURL().toString());
            URL url = new URL("http", "localhost", ServerConfig.getHttpPort(),
                              requrl.getPath());

            // open the connection and copy the request data
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-type", req.getContentType());
            CopyUtils.copy(req.getInputStream(), out = conn.getOutputStream());

            // convey the response back to the requester
            int rcode = conn.getResponseCode();
            if (rcode == HttpServletResponse.SC_OK) {
                CopyUtils.copy(in = conn.getInputStream(),
                               rsp.getOutputStream());
            } else {
                rsp.sendError(rcode, conn.getResponseMessage());
            }

        } catch (MalformedURLException mue) {
            throw new IOException("Failed to create redirect URL: " + mue);

        } finally {
            StreamUtil.close(out);
            StreamUtil.close(in);
        }
    }
}
