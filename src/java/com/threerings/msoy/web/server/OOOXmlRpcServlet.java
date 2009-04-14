//
// $Id$

package com.threerings.msoy.web.server;

import javax.servlet.ServletException;

import com.google.inject.Inject;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

import com.threerings.user.OOOXmlRpcService;

import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * A servlet that provides an XML-RPC interface to OOO user services.
 */
public class OOOXmlRpcServlet extends XmlRpcServlet
{
    public static class OOOXmlRpcServiceImpl implements OOOXmlRpcService
    {
        // from OOOXmlRpcService
        public boolean authUser (String username, String password)
        {
            try {
                // this will throw an exception if anything is wrong
                _staticAuth.authenticateSession(username, password);
                return true;
            } catch (ServiceException se) {
                return false;
            } catch (Exception e) {
                log.warning("Failed to auth user", "username", username, e);
                throw new RuntimeException("Internal error");
            }
        }

        /**
         * To integrate with Mediawiki in some halfway decent fashion, we authenticate using
         * permaname rather than email address. Members that don't yet have a permaname set can't
         * log into Mediawiki.
         */
        public boolean authUserForWiki (String username, String password)
        {
            try {
                // this will throw an exception if anything is wrong
                MemberRecord prec = _staticRepo.loadMemberByPermaname(username);
                if (prec == null) {
                    return false;
                }
                // this will throw an exception if logon fails
                _staticAuth.authenticateSession(prec.accountName, password);
                return true;
            } catch (ServiceException se) {
                return false;
            } catch (Exception e) {
                log.warning("Failed to auth user", "username", username, e);
                throw new RuntimeException("Internal error");
            }
        }
    }

    @Override // from XmlRpcServlet
    public XmlRpcHandlerMapping newXmlRpcHandlerMapping ()
        throws XmlRpcException
    {
        return new OOOHandlerMapping();
    }

    @Override // from HttpServlet
    public void init ()
        throws ServletException
    {
        super.init();

        // copy our injected dependency into our hacky static field
        _staticAuth = _auth;
        _staticRepo = _memberRepo;
    }

    protected static class OOOHandlerMapping extends PropertyHandlerMapping
    {
        public OOOHandlerMapping () throws XmlRpcException {
            addHandler("user", OOOXmlRpcServiceImpl.class);
        }
    }

    @Inject protected MsoyAuthenticator _auth;
    @Inject protected MemberRepository _memberRepo;

    // these are static so that we can provide them to our request handlers (which Apache's XML-RPC
    // library awesomely instantiates anew for each request and provides no non-total-backbending
    // way to initialize)
    protected static MsoyAuthenticator _staticAuth;
    protected static MemberRepository _staticRepo;
}
