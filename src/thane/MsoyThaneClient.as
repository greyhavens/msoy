//
// $Id$

import avmplus.System;
import com.whirled.bureau.client.WhirledBureauClient;
import com.threerings.msoy.client.DeploymentConfig;
import com.whirled.bureau.client.UserCode;
import com.whirled.bureau.client.UserCodeLoader;
import com.whirled.game.GameControl;
import flash.display.DisplayObject;

class MyServer
{
    public function MyServer ()
    {
        trace("Hello world, I am some sample user code");
    }
}

class MyUserCode implements UserCode
{
    public function createNewInstance () :Object
    {
        return new MyServer();
    }

    public function release () :void
    {
    }
}

class MyUserCodeLoader implements UserCodeLoader
{
    public function load (url :String, className :String, callback :Function) :void
    {
        callback(new MyUserCode());
    }
}

WhirledBureauClient.main(
    System.argv, DeploymentConfig.version, new MyUserCodeLoader());
