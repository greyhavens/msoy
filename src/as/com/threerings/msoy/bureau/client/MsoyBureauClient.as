//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.bureau.client.BureauDirector;
import com.threerings.msoy.client.DeploymentConfig;
import com.whirled.bureau.client.UserCodeLoader;
import com.whirled.bureau.client.WhirledBureauClient;
import com.whirled.bureau.util.WhirledBureauContext;

/** Msoy-specific subclass. */
public class MsoyBureauClient extends WhirledBureauClient
{
    /** Launch a msoy bureau client from the command line. */
    public static function main (
        args :Array, 
        userCodeLoader :UserCodeLoader) :void
    {
        var bureauId :String = args[0];
        var token :String = args[1];
        var server :String = "localhost";
        var port :int = parseInt(args[2]);

        // create the client and log on
        var client :MsoyBureauClient = new MsoyBureauClient(
            token, bureauId, userCodeLoader);
        client.setVersion(DeploymentConfig.version);
        client.setServer(server, [port]);
        client.logon();
    }

    /** Creates a new client. */
    public function MsoyBureauClient (
        token :String, 
        bureauId :String,
        userCodeLoader :UserCodeLoader)
    {
        super(token, bureauId, userCodeLoader);
    }

    /** @inheritDoc */
    // from BureauClient
    protected override function createDirector () :BureauDirector
    {
        // create the msoy-specific subclass
        return new MsoyBureauDirector(_ctx as WhirledBureauContext);
    }
}

}
