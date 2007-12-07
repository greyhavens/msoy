//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.msoy.client.ControlBackend;

public class MiniGameBackend extends ControlBackend
{
    /**
     * Set the container we're using.
     */
    public function setContainer (container :MiniGameContainer) :void
    {
        _container = container;
    }

    override public function shutdown () :void
    {
        super.shutdown();

        _container = null;
    }

    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["reportPerformance_v1"] = reportPerformance_v1;
    }

    /**
     * Called from usercode: report performance to the server.
     */
    protected function reportPerformance_v1 (score :Number, style :Number) :void
    {
        if (_container != null) {
            _container.recordPerformance(score, style);
        }
    }

    protected var _container :MiniGameContainer;
}

}
