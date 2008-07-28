//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.bureau.client.BureauDirector;
import com.threerings.bureau.util.BureauContext;
import com.threerings.msoy.bureau.data.ThaneCodes;
import com.threerings.msoy.bureau.util.MsoyBureauContext;
import com.threerings.msoy.client.DeploymentConfig;
import com.whirled.bureau.client.UserCodeLoader;
import com.whirled.bureau.client.WhirledBureauClient;

/** Msoy-specific subclass. */
public class MsoyBureauClient extends WhirledBureauClient
{
    /** Launch a msoy bureau client from the command line. */
    public static function main (
        args :Array, userCodeLoader :UserCodeLoader, cleanup :Function) :void
    {
        if (args.length != 4) {
            throw new Error("Expected exactly 4 arguments: (bureauId) (token) (server) (port)");
        }

        var bureauId :String = args[0];
        var token :String = args[1];
        var server :String = args[2];
        var port :int = parseInt(args[3]);

        // create the client and log on
        var client :MsoyBureauClient = new MsoyBureauClient(
            token, bureauId, userCodeLoader, cleanup);
        client.setVersion(DeploymentConfig.version);
        client.setServer(server, [port]);
        client.logon();
    }

    /** Creates a new client. */
    public function MsoyBureauClient (
        token :String, bureauId :String, userCodeLoader :UserCodeLoader, cleanup :Function)
    {
        super(token, bureauId, userCodeLoader, cleanup);
    }

    /** Access this client's window director. */
    public function getWindowDirector () :WindowDirector
    {
        var enableWindowDirector :Boolean = false;
        var thaneWorldServiceProvided :Boolean = false;

        if (enableWindowDirector && _windowDirector == null) {
            _windowDirector = new WindowDirector(_bureauId);
            // TODO: implement thane world service on the server and uncomment this
            if (thaneWorldServiceProvided) {
                _windowDirector.addServiceGroup(ThaneCodes.THANE_GROUP);
            }
        }

        return _windowDirector;
    }

    /** @inheritDoc */
    // from BureauClient
    protected override function createDirector () :BureauDirector
    {
        // create the msoy-specific subclass
        return new MsoyBureauDirector(_ctx as MsoyBureauContext);
    }

    /** @inheritDoc */
    // from BureauClient
    protected override function createContext () :BureauContext
    {
        // create the msoy-specific subclass
        return new ContextImpl(this);
    }

    protected var _windowDirector :WindowDirector;
}

}

import com.threerings.bureau.client.BureauDirector;
import com.threerings.msoy.bureau.client.MsoyBureauClient;
import com.threerings.msoy.bureau.client.WindowDirector;
import com.threerings.msoy.bureau.util.MsoyBureauContext;
import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;
import com.whirled.bureau.client.UserCodeLoader;

class ContextImpl
    implements MsoyBureauContext
{
    public function ContextImpl (client :MsoyBureauClient)
    {
        _client = client;
    }

    public function getBureauDirector () :BureauDirector
    {
        return _client.getBureauDirector();
    }

    public function getDObjectManager () :DObjectManager
    {
        return _client.getDObjectManager();
    }

    public function getClient () :Client
    {
        return _client;
    }

    public function getBureauId () :String
    {
        return _client.getBureauId();
    }

    public function getUserCodeLoader () :UserCodeLoader
    {
        return _client.getUserCodeLoader();
    }

    public function getWindowDirector () :WindowDirector
    {
        return _client.getWindowDirector();
    }

    protected var _client :MsoyBureauClient;
}
