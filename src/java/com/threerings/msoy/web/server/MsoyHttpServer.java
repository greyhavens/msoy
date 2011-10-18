//
// $Id$

package com.threerings.msoy.web.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.mortbay.io.Connection;
import org.mortbay.io.nio.SelectChannelEndPoint;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpException;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.ErrorHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.Lifecycle;

import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.servlet.util.ParameterUtil;

import com.threerings.admin.web.server.ConfigServlet;

import com.threerings.pulse.jetty.server.JettyPulseHttpServer;
import com.threerings.pulse.web.server.PulseFlotServlet;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.server.AdminServlet;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.server.AppServlet;
import com.threerings.msoy.comment.gwt.CommentService;
import com.threerings.msoy.comment.server.CommentServlet;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.edgame.gwt.EditGameService;
import com.threerings.msoy.edgame.server.EditGameServlet;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.server.FacebookCallbackServlet;
import com.threerings.msoy.facebook.server.FacebookInviteServlet;
import com.threerings.msoy.facebook.server.FacebookServlet;
import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.server.ForumServlet;
import com.threerings.msoy.fora.server.IssueServlet;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.server.GameServlet;
import com.threerings.msoy.game.server.GameTraceLogServlet;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.server.GroupServlet;
import com.threerings.msoy.imagechooser.gwt.ImageChooserService;
import com.threerings.msoy.imagechooser.server.ImageChooserServlet;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.server.CatalogServlet;
import com.threerings.msoy.item.server.ItemMediaUploadServlet;
import com.threerings.msoy.item.server.ItemServlet;
import com.threerings.msoy.mail.gwt.MailService;
import com.threerings.msoy.mail.server.MailServlet;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.server.MoneyServlet;
import com.threerings.msoy.person.gwt.GalleryService;
import com.threerings.msoy.person.gwt.InviteService;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.server.GalleryServlet;
import com.threerings.msoy.person.server.InviteServlet;
import com.threerings.msoy.person.server.MeServlet;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.profile.server.ProfileServlet;
import com.threerings.msoy.reminders.gwt.RemindersService;
import com.threerings.msoy.reminders.server.RemindersServlet;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.server.SnapshotItemUploadServlet;
import com.threerings.msoy.room.server.WebRoomServlet;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.server.StuffServlet;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.server.SurveyServlet;
import com.threerings.msoy.underwire.server.MsoyUnderwireServlet;
import com.threerings.msoy.web.gwt.CssUtil;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebUserService;

import static com.threerings.msoy.Log.log;

/**
 * Handles HTTP requests made of the Msoy server by the AJAX client and other entities.
 */
@Singleton
public class MsoyHttpServer extends JettyPulseHttpServer
{
    /**
     * Attaches the required privacy header for setting cookies inside an iframe contained in
     * another web site. This is required for browsers that implement privacy guards (ie7,
     * others?). The attached header describes how this web site uses and disseminates user
     * information, including cookies. This is presumed to protect users somehow.
     * @see http://www.w3.org/P3P/
     */
    public static void addPrivacyHeader (HttpServletResponse rsp)
    {
        rsp.addHeader(PRIVACY_HEADER_NAME, PRIVACY_HEADER);
    }

    @Inject public MsoyHttpServer (Injector injector, Lifecycle cycle)
    {
        super(8000, 20000);
        // turn our servlet classes into instances with fully resolved dependencies
        for (Map.Entry<String, Class<? extends HttpServlet>> entry : SERVLETS.entrySet()) {
            _servlets.put(entry.getKey(), injector.getInstance(entry.getValue()));
        }

        // stop our http server when the server shuts down
        cycle.addComponent(new Lifecycle.ShutdownComponent() {
            public void shutdown () {
                try {
                    stop();
                } catch (Exception e) {
                    log.warning("Failed to stop HTTP server.", e);
                }
            }
        });
    }

    /**
     * Prepares our HTTP server for operation but does not yet start listening on the HTTP port.
     */
    public void init (File logdir)
        throws IOException
    {
        // use a custom connector that works around some jetty non-awesomeness
        setConnectors(new Connector[] { new MsoyChannelConnector() });

        // add the PulseServlet, which can't be resolved earlier because it depends on
        // PulseModule to have been installed.
        _servlets.put("/pulse/*", _injector.getInstance(PulseFlotServlet.class));

        // wire up our various servlets
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        Context context = new Context(contexts, "/", Context.NO_SESSIONS);
        for (Map.Entry<String, HttpServlet> entry : _servlets.entrySet()) {
            context.addServlet(new JettyPulseServletHolder(entry.getValue()), entry.getKey());
        }

        // wire up serving of static content
        context.setWelcomeFiles(new String[] { "index.html" });
        context.setResourceBase(new File(ServerConfig.serverRoot, "pages").getPath());

        // deliver a static error page regardless of cause; errors are already logged
        context.setErrorHandler(new ErrorHandler() {
            protected void writeErrorPageHead (HttpServletRequest request, Writer writer, int code,
                                               String message) throws IOException {
                writer.write(ERROR_HEAD);
            }
            protected void writeErrorPageBody (HttpServletRequest request, Writer writer, int code,
                                               String message, boolean stacks) throws IOException {
                writer.write(ERROR_BODY);
            }
        });

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

    /**
     * Sends a short script that will redirect the user's browser window to the given URL (as
     * opposed to {@link HttpServletResponse#sendRedirect}, which only works for the frame making
     * the request.
     */
    public static void sendTopRedirect (HttpServletResponse rsp, String url)
        throws IOException
    {
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println("<html><head><script language=\"JavaScript\">");
            out.println("window.top.location = '" + url + "';");
            out.println("</script></head></html>");
        } finally {
            StreamUtil.close(out);
        }
    }

    public static void dumpParameters (HttpServletRequest req)
    {
        for (String pname : ParameterUtil.getParameterNames(req)) {
            for (String value : req.getParameterValues(pname)) {
                log.info("  param " + pname + " -> " + value);
            }
        }
    }

    public static void dumpCookies (HttpServletRequest req)
    {
        if (req.getCookies() == null) {
            log.info("  null cookies");
            return;
        }
        for (Cookie cookie : req.getCookies()) {
            log.info("  cookie " + cookie.getName() + " -> " + cookie.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public static void dumpHeaders (HttpServletRequest req)
    {
        Enumeration<String> names = req.getHeaderNames();
        if (names == null) {
            log.info("  null headers");
            return;
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Enumeration<String> values = req.getHeaders(name);
            while (values.hasMoreElements()) {
                String value = values.nextElement();
                log.info("  header " + name + " -> " + value);
            }
        }
    }

    @Inject protected Injector _injector;

    protected static class MsoyChannelConnector extends SelectChannelConnector
    {
        public MsoyChannelConnector () {
            setPort(ServerConfig.httpPort);
        }

        @Override // from SelectChannelConnector
        protected Connection newConnection (SocketChannel chan, SelectChannelEndPoint ep) {
            return new HttpConnection(this, ep, getServer()) {
                @Override public void handle () throws IOException {
                    try {
                        super.handle();
                    } catch (NumberFormatException nfe) {
                        // TODO: demote this to log.info in a week or two
                        log.warning("Failing invalid HTTP request", "uri", _uri, "error", nfe);
                        throw new HttpException(400); // bad request
                    } catch (IOException ioe) {
                        if (ioe.getClass() == IOException.class) { // fucking stupid fucking jetty
                            log.warning("Failing invalid HTTP request", "uri", _uri, "error", ioe);
                            throw new HttpException(400); // bad request
                        } else {
                            throw ioe;
                        }
                    }
                }
            };
        }
    }

    protected static class MsoyConfigServlet extends ConfigServlet
    {
        @Override // from ConfigServlet
        protected void requireAdminUser ()
			throws ServiceException
        {
			MemberRecord mrec =  _mhelper.getAuthedUser(
				CookieUtil.getCookieValue(getThreadLocalRequest(), WebCreds.credsCookie()));

			if (mrec == null) {
				throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
			}
			if (!mrec.isAdmin()) {
				throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
			}
        }

		@Inject protected MemberHelper _mhelper;
    }

    /** Populated during {@link #preInit} with dependency resolved servlet instances. */
    protected Map<String, HttpServlet> _servlets = Maps.newHashMap();

    protected static final Map<String, Class<? extends HttpServlet>> SERVLETS =
        new ImmutableMap.Builder<String, Class<? extends HttpServlet>>()
        .put(AdminService.ENTRY_POINT, AdminServlet.class)
        .put(CatalogService.ENTRY_POINT, CatalogServlet.class)
        .put(CommentService.ENTRY_POINT, CommentServlet.class)
        .put(ForumService.ENTRY_POINT, ForumServlet.class)
        .put(GalleryService.ENTRY_POINT, GalleryServlet.class)
        .put(GameService.ENTRY_POINT, GameServlet.class)
        .put(EditGameService.ENTRY_POINT, EditGameServlet.class)
        .put(GroupService.ENTRY_POINT, GroupServlet.class)
        .put(InviteService.ENTRY_POINT, InviteServlet.class)
        .put(IssueService.ENTRY_POINT, IssueServlet.class)
        .put(ItemService.ENTRY_POINT, ItemServlet.class)
        .put(ImageChooserService.ENTRY_POINT, ImageChooserServlet.class)
        .put(MailService.ENTRY_POINT, MailServlet.class)
        .put(MeService.ENTRY_POINT, MeServlet.class)
        .put(MoneyService.ENTRY_POINT, MoneyServlet.class)
        .put(WebMemberService.ENTRY_POINT, MemberServlet.class)
        .put(ProfileService.ENTRY_POINT, ProfileServlet.class)
        .put(StuffService.ENTRY_POINT, StuffServlet.class)
        .put(WebUserService.ENTRY_POINT, WebUserServlet.class)
        .put(WebRoomService.ENTRY_POINT, WebRoomServlet.class)
        .put(SurveyService.ENTRY_POINT, SurveyServlet.class)
        .put(FacebookService.ENTRY_POINT, FacebookServlet.class)
        .put(RemindersService.ENTRY_POINT, RemindersServlet.class)
        .put(AppService.ENTRY_POINT, AppServlet.class)
        .put("/configsvc", MsoyConfigServlet.class)
        .put("/facebook/*", FacebookCallbackServlet.class)
        .put("/ooo", OOOXmlRpcServlet.class)
        .put("/remixuploadsvc", UploadRemixMediaServlet.class)
        .put("/scenethumbsvc", SceneThumbnailUploadServlet.class)
        .put("/snapshotsvc", SnapshotItemUploadServlet.class)
        .put("/undersvc", MsoyUnderwireServlet.class)
        .put("/uploadsvc", ItemMediaUploadServlet.class)
        .put("/stubdlsvc", StubDownloadServlet.class)
        .put("/loadersvc", LoaderServlet.class)
        .put("/swizzle/*", SwizzleServlet.class)
        .put("/embed/*", EmbedRouterServlet.class)
        .put("/status/*", StatusServlet.class)
        .put("/json/*", JSONServlet.class)
        .put("/mystats/*", MyStatsServlet.class)
        .put("/gamelogs/*", GameTraceLogServlet.class)
        .put("/info/*", PublicInfoServlet.class)
        .put("/rss/*", RSSServlet.class)
        .put("/go/*", GoServlet.class)
        .put("/welcome/*", GoServlet.class)
        .put("/friend/*", GoServlet.class)
        .put("/fbinvite/*", FacebookInviteServlet.class)
        .put("/gameframe/*", GameFrameServlet.class)
        .put("/js/facebook.js", AppInserterServlet.class)
        .put("/themed/*", ThemedTemplateServlet.class)
        .put("/custom.css", CustomCssServlet.class)
        .put(DeploymentConfig.PROXY_PREFIX + "*", MediaProxyServlet.class)
        // if -Dthrottle=true is set, serve up files as if we were on a slow connection
        .put("/*", (Boolean.getBoolean("throttle") || Boolean.getBoolean("throttleMedia"))
            ? MsoyThrottleServlet.class
            : MsoyDefaultServlet.class)
        .build();

    protected static final String ERROR_HEAD =
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\n" +
        "<title>Oh noez!</title>\n" +
        "<link rel=\"stylesheet\" href=\"" + CssUtil.GLOBAL_PATH + "\" type=\"text/css\"/>\n" +
        "<style type=\"text/css\">\n" +
        ".kansas { width: 100%; min-height: 100%; " +
        "    background: #61ABD0 url(/images/whirled/bg_gradient_long_blue.png) repeat-x;\n" +
        "    color: #FFFFFF; }\n" +
        ".kansas h2 { padding-top: 50px; }\n" +
        ".kansas a, .kansas a:visited { color: #FFFFFF; }\n" +
        "</style>\n";

    protected static final String ERROR_BODY =
        "<div class=\"kansas\">" +
        "<center><h2>We're not in Kansas any more Toto!</h2>\n" +
        "Your browser clings to data whose time has passed.<br/>\n" +
        "Click your heels together three times and click below to rejoin the Whirled.<br/>\n" +
        "<br/><br/><a href=\"/\" target=\"_top\">Reload Whirled</a><br/><br/><br/>\n" +
        "(If the link doesn't work, press CTRL+F5 on Windows or CMD+SHIFT+R on Mac to force " +
        "a refresh)\n" +
        "</center>\n"+
        "</div>\n";

    protected static final String PRIVACY_HEADER_NAME = "P3P";

    // Copied from puzzle pirates web site
    protected static final String PRIVACY_HEADER = "CP=\"CAO DSP COR CURa ADMa DEVa TAIa PSAa " +
        "PSDa CONo OUR IND PHY ONL UNI PUR COM NAV INT DEM\"";
}
