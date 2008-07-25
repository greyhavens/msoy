//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;

import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;

import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.web.client.DeploymentConfig;

import static com.threerings.msoy.Log.log;

/**
 * Handles HTTP requests made of the Msoy server by the AJAX client and other entities.
 */
@Singleton
public class MsoyHttpServer extends Server
{
    /**
     * Prepares our HTTP server for operation but does not yet start listening on the HTTP port.
     */
    public void init (Injector injector, File logdir)
        throws IOException
    {
        SelectChannelConnector conn = new SelectChannelConnector();
        conn.setPort(ServerConfig.httpPort);
        setConnectors(new Connector[] { conn });

        // wire up our various servlets
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        Context context = new Context(contexts, "/", Context.NO_SESSIONS);
        for (Map.Entry<String, Class<? extends HttpServlet>> entry : SERVLETS.entrySet()) {
            HttpServlet servlet = injector.getInstance(entry.getValue());
            context.addServlet(new ServletHolder(servlet), entry.getKey());
        }

        // wire up serving of static content
        context.setWelcomeFiles(new String[] { "index.html" });
        context.setResourceBase(new File(ServerConfig.serverRoot, "pages").getPath());

        HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(contexts);
        // turn on logging only if requested, it starts a daemon
        if (ServerConfig.config.getValue("log_http_requests", false)) {
            RequestLogHandler logger = new RequestLogHandler();
            // set up logging
            String logname = (ServerConfig.nodeName != null) ?
                "access_" + ServerConfig.nodeName + ".log.yyyy_mm_dd" : "access.log.yyyy_mm_dd";
            logger.setRequestLog(new NCSARequestLog(new File(logdir, logname).getPath()));
            handlers.addHandler(logger);
        }
        setHandler(handlers);
    }

    /** Handles redirecting to our magic version numbered client for embedding and does other
     * fiddling we want. */
    protected static class MsoyDefaultServlet extends DefaultServlet
    {
        protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
            throws ServletException, IOException {
            // TODO: handle this for more than just world-client.swf?
            if (req.getRequestURI().equals("/clients/world-client.swf")) {
                rsp.setContentLength(0);
                rsp.sendRedirect("/clients/" + DeploymentConfig.version + "/world-client.swf");
            } else {
                super.doGet(req, rsp);
            }
        }
    }

    protected static class MsoyThrottleServlet extends MsoyDefaultServlet
    {
        protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
            throws ServletException, IOException {
            // if we're only throttling media, see if this is media
            if (Boolean.getBoolean("throttle") || req.getRequestURI().startsWith("/media/")) {
                rsp = new HttpServletResponseWrapper(rsp) {
                    @Override
                    public ServletOutputStream getOutputStream () throws IOException {
                        if (_out == null) {
                            _out = new ThrottleOutputStream(super.getOutputStream());
                        }
                        return _out;
                    }
                    protected ServletOutputStream _out;
                };
            }
            super.doGet(req, rsp);
        }
    }

    protected static class ThrottleOutputStream extends ServletOutputStream
    {
        public ThrottleOutputStream (ServletOutputStream out) {
            _out = out;
        }

        public void write (int i) throws IOException {
            _out.write(i);
        }

        public void write (byte[] b, int off, int len) throws IOException {
            while (len > 0) {
                int toWrite = Math.min(len, 1024);
                _out.write(b, off, toWrite);
                off += toWrite;
                len -= toWrite;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                }
            }
        }

        public void print (boolean arg) throws IOException {
            _out.print(arg);
        }
        public void print (char arg) throws IOException {
            _out.print(arg);
        }
        public void print (double arg) throws IOException {
            _out.print(arg);
        }
        public void print (float arg) throws IOException {
            _out.print(arg);
        }
        public void print (int arg) throws IOException {
            _out.print(arg);
        }
        public void print (long arg) throws IOException {
            _out.print(arg);
        }
        public void print (String arg) throws IOException {
            _out.print(arg);
        }
        public void println () throws IOException {
            _out.println();
        }
        public void println (boolean arg) throws IOException {
            _out.println(arg);
        }
        public void println (char arg) throws IOException {
            _out.println(arg);
        }
        public void println (double arg) throws IOException {
            _out.println(arg);
        }
        public void println (float arg) throws IOException {
            _out.println(arg);
        }
        public void println (int arg) throws IOException {
            _out.println(arg);
        }
        public void println (long arg) throws IOException {
            _out.println(arg);
        }
        public void println (String arg) throws IOException {
            _out.println(arg);
        }

        protected ServletOutputStream _out;
    }

    protected static final Map<String, Class<? extends HttpServlet>> SERVLETS = Maps.newHashMap();
    static {
        SERVLETS.put("/usersvc", WebUserServlet.class);
        SERVLETS.put("/adminsvc", AdminServlet.class);
        SERVLETS.put("/itemsvc", ItemServlet.class);
        SERVLETS.put("/catalogsvc", CatalogServlet.class);
        SERVLETS.put("/profilesvc", ProfileServlet.class);
        SERVLETS.put("/membersvc", MemberServlet.class);
        SERVLETS.put("/groupsvc", GroupServlet.class);
        SERVLETS.put("/mailsvc", MailServlet.class);
        SERVLETS.put("/uploadsvc", UploadServlet.class);
        SERVLETS.put("/remixuploadsvc", UploadRemixMediaServlet.class);
        SERVLETS.put("/gamesvc", GameServlet.class);
        SERVLETS.put("/swiftlysvc", SwiftlyServlet.class);
        SERVLETS.put("/swiftlyuploadsvc", SwiftlyUploadServlet.class);
        SERVLETS.put("/facebook", FacebookServlet.class);
        SERVLETS.put("/scenethumbsvc", SceneThumbnailUploadServlet.class);
        SERVLETS.put("/snapshotsvc", SnapshotItemUploadServlet.class);
        SERVLETS.put("/commentsvc", CommentServlet.class);
        SERVLETS.put("/worldsvc", WorldServlet.class);
        SERVLETS.put("/forumsvc", ForumServlet.class);
        SERVLETS.put("/issuesvc", IssueServlet.class);
        SERVLETS.put("/undersvc", MsoyUnderwireServlet.class);
        SERVLETS.put("/gamestubsvc", GameStubServlet.class);
        SERVLETS.put("/embed/*", EmbedRouterServlet.class);
        SERVLETS.put("/status/*", StatusServlet.class);
        SERVLETS.put("/mystats/*", MyStatsServlet.class);
        SERVLETS.put("/gamelogs/*", GameTraceLogServlet.class);
        SERVLETS.put("/info/*", PublicInfoServlet.class);
        SERVLETS.put("/rss/*", RSSServlet.class);
        SERVLETS.put(DeploymentConfig.PROXY_PREFIX + "*", MediaProxyServlet.class);
        // if -Dthrottle=true is set, serve up files as if we were on a slow connection
        SERVLETS.put("/*", (Boolean.getBoolean("throttle") || Boolean.getBoolean("throttleMedia"))
            ? MsoyThrottleServlet.class
            : MsoyDefaultServlet.class);
    } // end: static initializer
}
