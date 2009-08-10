//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.MemberLogic;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import static com.threerings.msoy.Log.log;

/**
 * Handles a simple request to redirect: /go/[page_tokens_and_args]
 *
 * <p> Or a request to redirect with an optional assignment of affiliate:
 * /welcome/[affiliate]/[page_tokens_and_args]</p>
 * 
 * <p> Or a request to redirect with an optional assignment of affiliate and a friend request:
 * /friend/[affiliate]/[page_tokens_and_args]</p>
 */
public class GoServlet extends HttpServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String path = StringUtil.deNull(req.getPathInfo());
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        int affiliateId = 0;
        boolean autoFriend = req.getRequestURI().startsWith("/friend/");
        if (autoFriend || req.getRequestURI().startsWith("/welcome/")) {
            // the path will now either be "", "<affiliate>", or "<affiliate>/<token>".
            // <affiliate> may be 0 to indicate "no affiliate" (we just want the redirect).
            // NOTE: we allow negative affiliate ids here and the AffiliateCookie will eventually
            // convert these to "autoFriend" affiliates
            int nextSlash = path.indexOf("/");
            if (nextSlash == -1) {
                affiliateId = parseAffiliate(path);
                path = "";
            } else {
                affiliateId = parseAffiliate(path.substring(0, nextSlash));
                path = path.substring(nextSlash + 1);
            }
        }

        // after sorting out the actual page, see if we want to serve up something tricky
        if (_cloakedPageLogic.serveCloakedPage(req, rsp, path,
                StringUtil.deNull(req.getHeader("User-Agent")))) {
            return;
        }

        // if this user appears to be brand new...
        if (VisitorCookie.shouldCreate(req)) {
            // create a visitor info for them
            VisitorInfo info = VisitorCookie.createAndSet(rsp);

            // if the URL contains an entry vector, we extract it
            Tuple<String, String> bits = extractVector(path);
            path = bits.right;

            // note that we have a new visitor
            _memberLogic.noteNewVisitor(info, true, bits.left, req.getHeader("Referrer"));
        }

        // set their affiliate cookie if appropriate
        if (affiliateId > 0) {
            AffiliateCookie.set(rsp, affiliateId, autoFriend);
        }

        rsp.sendRedirect("/#" + path);
    }

    protected int parseAffiliate (String affiliate)
    {
        try {
            return Integer.parseInt(affiliate);
        } catch (Exception e) {
            log.info("Ignoring bogus affiliate", "aff", affiliate);
            return 0;
        }
    }

    protected static Tuple<String, String> extractVector (String path)
    {
        path = StringUtil.deNull(path);
        if (path.indexOf(ArgNames.VECTOR) != -1) {
            try {
                Pages page = Pages.fromHistory(path);
                Args args = Args.fromHistory(path);
                for (int ii = 0; ii < args.getArgCount(); ii++) {
                    if (args.get(ii, "").equals(ArgNames.VECTOR)) {
                        return Tuple.newTuple(args.get(ii+1, (String)null),
                                              page.makeToken(args.recomposeWithout(ii, 2)));
                    }
                }
            } catch (Exception e) {
                log.info("Failure looking for entry vector", "path", path, "error", e);
            }
        }

        // if we had no (or a bogus) vector, use the page URL instead
        return Tuple.newTuple(StringUtil.truncate("page." + path, 128), path);
    }

    // our dependencies
    @Inject protected MemberLogic _memberLogic;
    @Inject protected CloakedPageLogic _cloakedPageLogic;
}
