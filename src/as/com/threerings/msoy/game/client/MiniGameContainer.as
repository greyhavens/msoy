package com.threerings.msoy.game.client {

import flash.display.Loader;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.item.data.all.MediaDesc;

public class MiniGameContainer extends MediaContainer
{
    /** A function you may assign to receive minigame performance from usercode.
     *
     * Signature: function (score :Number, style :Number) :void
     *
     * @param score between 0 - 1
     * @param style between 0 - 1 (optional)
     */
    public var performanceCallback :Function;

    /**
     * Create a MiniGameContainer.
     */
    public function MiniGameContainer ()
    {
        super(null);
    }

    public function setup (desc :MediaDesc) :void
    {
        setMedia(desc.getMediaPath());
    }

    override public function shutdown (completely :Boolean = true) :void
    {
        if (_backend != null) {
            _backend.shutdown();
            _backend = null;
        }

        super.shutdown(completely);
    }

    override protected function setupSwfOrImage (url :String) :void
    {
        super.setupSwfOrImage(url);

        _backend = new MiniGameBackend();
        _backend.init(Loader(_media));
        _backend.setContainer(this);
    }

    protected var _backend :MiniGameBackend;
}
}
