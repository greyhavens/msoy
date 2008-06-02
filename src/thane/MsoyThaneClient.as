//
// $Id$

import avmplus.System;
import com.whirled.bureau.client.WhirledBureauClient;
import com.threerings.msoy.client.DeploymentConfig;
import com.whirled.bureau.client.UserCodeLoader;


class MyServer
{
    public function MyServer ()
    {
        trace("Hello world, I am some sample user code");
    }
}

class MyUserCodeLoader implements UserCodeLoader
{
    public function load (url :String, className :String, callback :Function) :void
    {
        callback(MyServer);
    }

    public function unload (clazz :Class) :void
    {
        // ok
    }
}

WhirledBureauClient.main(
    System.argv, DeploymentConfig.version, new MyUserCodeLoader());
