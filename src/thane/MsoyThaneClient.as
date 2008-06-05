//
// $Id$

import avmplus.System;
import com.whirled.bureau.client.WhirledBureauClient;
import com.threerings.msoy.client.DeploymentConfig;
import com.whirled.thane.HttpUserCodeLoader;

WhirledBureauClient.main(
    System.argv, 
    DeploymentConfig.version, 
    new HttpUserCodeLoader());
