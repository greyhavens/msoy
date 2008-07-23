//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;

import com.threerings.presents.server.PresentsDObjectMgr;

/**
 * The invoker used for Swiftly operations.
 */
@Singleton
public class SwiftlyInvoker extends Invoker
{
    @Inject public SwiftlyInvoker (PresentsDObjectMgr omgr)
    {
        super("swiftly.Invoker", omgr);
        setDaemon(true);
        start();
    }
}
