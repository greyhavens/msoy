//
// $Id$

package client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

import client.shell.CShell;

/**
 * Reports a callback error via {@link MsoyUI}.
 */
public abstract class MsoyCallback implements AsyncCallback
{
    // from AsyncCallback
    public void onFailure (Throwable cause)
    {
        MsoyUI.error(CShell.serverError(cause));
        CShell.log("Service request failed", cause);
    }
}
