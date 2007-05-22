//
// $Id$

package com.threerings.msoy.swiftly.client.signed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.logging.Level;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.io.IOUtils;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

import static com.threerings.msoy.Log.log;

public class ResultExporter
{
    public ResultExporter (String resultURL, String outputName, SwiftlyContext ctx)
    {
        _outputName = outputName;
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(SwiftlyCodes.SWIFTLY_MSGS);

        try {
            _resultURL = new URL(resultURL);

        } catch (MalformedURLException e) {
            log.log(Level.WARNING, "Malformed results URL [url=" + resultURL + "].", e);
            _ctx.showErrorMessage(_msgs.get("e.export_failed"));
        }
    }

    public void exportResult ()
    {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws IOException {
                    File out = new File(System.getProperty("user.home") + File.separator +
                         "Desktop" + File.separator + _outputName);
                    FileOutputStream ostream = new FileOutputStream(out);
                    IOUtils.copy(_resultURL.openStream(), ostream);
                    ostream.close();
                    _ctx.showInfoMessage(_msgs.get("m.result_exported", out.getPath()));
                    return null;
                }
            });

        } catch (PrivilegedActionException e) {
            log.log(Level.WARNING, "Failed to save build results.", e.getException());
            _ctx.showErrorMessage(_msgs.get("e.export_failed"));
        }
    }

    protected URL _resultURL;
    protected String _outputName;
    protected SwiftlyContext _ctx;
    protected MessageBundle _msgs;
}
