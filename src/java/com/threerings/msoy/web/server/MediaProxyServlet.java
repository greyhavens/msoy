//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.samskivert.io.StreamUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * Proxies downloads of Java game jar files so that we can work around an infuriatingly annoying
 * "feature" of the applet security sandbox. If you have an applet with two jar files in its
 * "archive" property, say:
 *
 * http://GAME_SERVER_IP/client/game-client.jar
 * http://MEDIA_SERVER_IP/media/game_media_hash.jar
 *
 * Then the applet will happily load <em>code</em> from both jar files, but it will refuse to load
 * media from any but the first. Having perused the source, it seems that getResource() in
 * AppletClassLoader checks security restrictions for resources but when using getResource() to
 * load code, it does not check those restrictions. That sure seems like a bug to me (either it
 * should or shouldn't for both) but fixing it is not a solution to our problem since we can't very
 * well require that users have the JDK 1.6.whenever_they_fixed_our_bug plugin installed.
 */
public class MediaProxyServlet extends HttpServlet
{
    protected void doHead (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        proxy("HEAD", req, rsp);
    }

    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        proxy("GET", req, rsp);
    }

    protected void proxy (String method, HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        InputStream in = null;

        try {
            // determine the path to the media they requested
            URL requrl = new URL(req.getRequestURL().toString());
            String rsrc = requrl.getPath().substring(DeploymentConfig.PROXY_PREFIX.length());

            // reroute the URL to our media server
            URL url = new URL(ServerConfig.mediaURL + rsrc);

            // if our media is being served from this host, simply serve up the data directly from
            // the file system instead of proxying (Jetty 6 doesn't like to proxy from itself)
            if (requrl.getHost().equals(url.getHost())) {
                File media = new File(ServerConfig.mediaDir, rsrc);
                in = new FileInputStream(media);

            } else {
                // open the connection and copy the request data
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod(method);

                // convey the response back to the requester
                int rcode = conn.getResponseCode();
                if (rcode != HttpServletResponse.SC_OK) {
                    log.warning("Proxy failed [url=" + url + ", rcode=" + rcode +
                                ", rmsg=" + conn.getResponseMessage() + "].");
                    rsp.sendError(rcode, conn.getResponseMessage());
                    return;
                }
                in = conn.getInputStream();
            }

            IOUtils.copy(in, rsp.getOutputStream());

        } catch (MalformedURLException mue) {
            throw new IOException("Failed to create proxy URL: " + mue);

        } finally {
            StreamUtil.close(in);
        }
    }
}
