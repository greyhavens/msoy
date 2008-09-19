//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
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

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.server.AdminServlet;
import com.threerings.msoy.comment.gwt.CommentService;
import com.threerings.msoy.comment.server.CommentServlet;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.server.ForumServlet;
import com.threerings.msoy.fora.server.IssueServlet;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.server.GameServlet;
import com.threerings.msoy.game.server.GameStubServlet;
import com.threerings.msoy.game.server.GameTraceLogServlet;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.server.GroupServlet;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.server.CatalogServlet;
import com.threerings.msoy.item.server.ItemMediaUploadServlet;
import com.threerings.msoy.item.server.ItemServlet;
import com.threerings.msoy.landing.gwt.LandingService;
import com.threerings.msoy.landing.server.LandingServlet;
import com.threerings.msoy.mail.gwt.MailService;
import com.threerings.msoy.mail.server.MailServlet;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.server.GalleryServlet;
import com.threerings.msoy.person.server.InviteServlet;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.server.MeServlet;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.server.MoneyServlet;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.server.ProfileServlet;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.server.StuffServlet;
import com.threerings.msoy.swiftly.gwt.SwiftlyService;
import com.threerings.msoy.swiftly.server.SwiftlyServlet;
import com.threerings.msoy.swiftly.server.SwiftlyUploadServlet;
import com.threerings.msoy.underwire.server.MsoyUnderwireServlet;

import com.threerings.msoy.web.client.WebMemberService;
import com.threerings.msoy.web.client.WebUserService;

import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.server.SnapshotItemUploadServlet;
import com.threerings.msoy.room.server.WebRoomServlet;

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
            checkCookies(req, rsp); // we want to check cookies even before we do the redirect

            // TODO: handle this for more than just world-client.swf?
            if (req.getRequestURI().equals("/clients/world-client.swf")) {
                rsp.setContentLength(0);
                rsp.sendRedirect("/clients/" + DeploymentConfig.version + "/world-client.swf");
            } else {
                super.doGet(req, rsp);
            }
        }

        protected void checkCookies (HttpServletRequest req, HttpServletResponse rsp)
        {
            AffiliateCookie.check(req, rsp);
            addReferralCookie(req, rsp);
        }

        /**
         * Adds a session cookie, with which we'll communicate the original http referer
         * over to the GWT side (which will then use it to construct the ReferralInfo)
         */
        protected void addReferralCookie (HttpServletRequest req, HttpServletResponse rsp) {
            // do we already know the referer? if so, we're done
            if (HttpReferrerCookie.exists(req)) {
                return;
            }
            // otherwise, this is the first time we got loaded - store the HTTP referer
            Object ref = req.getHeader("Referer");
            if (ref instanceof String) {
                try {
                    URL url = new URL((String) ref);
                    HttpReferrerCookie.set(rsp, url.getHost());
                } catch (MalformedURLException mue) {
                    // ignore - we just won't add this cookie
                }
            }
        }
    } // end: MsoyDefaultServlet

    /**
     * Used only for testing, this fakey slows-down the transmission of data to clients.
     * It can be configured with -Dthrottle=true or -DthrottleMedia=true.
     */
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
                    // We explicitly mirror our parent class' deprecation of these methods to
                    // prevent the compiler from complaining.
                    @Deprecated
                    public String encodeRedirectUrl (String arg0) {
                        return super.encodeRedirectUrl(arg0);
                    }
                    @Deprecated
                    public String encodeUrl (String arg0) {
                        return super.encodeUrl(arg0);
                    }
                    @Deprecated
                    public void setStatus (int arg0, String arg1) {
                        super.setStatus(arg0, arg1);
                    }
                    protected ServletOutputStream _out;
                };
            }
            super.doGet(req, rsp);
        }
    } // end: MsoyThrottleServlet

    /**
     * Caaaaan youuuuuu heeeeaaaaaaaaarrrrrrrrrr mmmmmeeeee??
     */
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
        SERVLETS.put(AdminService.ENTRY_POINT, AdminServlet.class);
        SERVLETS.put(CatalogService.ENTRY_POINT, CatalogServlet.class);
        SERVLETS.put(CommentService.ENTRY_POINT, CommentServlet.class);
        SERVLETS.put(ForumService.ENTRY_POINT, ForumServlet.class);
        SERVLETS.put(GalleryService.ENTRY_POINT, GalleryServlet.class);
        SERVLETS.put(GameService.ENTRY_POINT, GameServlet.class);
        SERVLETS.put(GroupService.ENTRY_POINT, GroupServlet.class);
        SERVLETS.put(InviteService.ENTRY_POINT, InviteServlet.class);
        SERVLETS.put(IssueService.ENTRY_POINT, IssueServlet.class);
        SERVLETS.put(ItemService.ENTRY_POINT, ItemServlet.class);
        SERVLETS.put(LandingService.ENTRY_POINT, LandingServlet.class);
        SERVLETS.put(MailService.ENTRY_POINT, MailServlet.class);
        SERVLETS.put(MeService.ENTRY_POINT, MeServlet.class);
        SERVLETS.put(MoneyService.ENTRY_POINT, MoneyServlet.class);
        SERVLETS.put(WebMemberService.ENTRY_POINT, MemberServlet.class);
        SERVLETS.put(ProfileService.ENTRY_POINT, ProfileServlet.class);
        SERVLETS.put(StuffService.ENTRY_POINT, StuffServlet.class);
        SERVLETS.put(SwiftlyService.ENTRY_POINT, SwiftlyServlet.class);
        SERVLETS.put(WebUserService.ENTRY_POINT, WebUserServlet.class);
        SERVLETS.put(WebRoomService.ENTRY_POINT, WebRoomServlet.class);
        SERVLETS.put("/facebook", FacebookServlet.class);
        SERVLETS.put("/gamestubsvc", GameStubServlet.class);
        SERVLETS.put("/remixuploadsvc", UploadRemixMediaServlet.class);
        SERVLETS.put("/scenethumbsvc", SceneThumbnailUploadServlet.class);
        SERVLETS.put("/snapshotsvc", SnapshotItemUploadServlet.class);
        SERVLETS.put("/swiftlyuploadsvc", SwiftlyUploadServlet.class);
        SERVLETS.put("/undersvc", MsoyUnderwireServlet.class);
        SERVLETS.put("/uploadsvc", ItemMediaUploadServlet.class);
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
