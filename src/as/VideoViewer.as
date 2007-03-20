package {

import flash.display.Sprite;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.VideoDisplayer;

[SWF(width="320", height="240")]
public class VideoViewer extends Sprite
{
    public function VideoViewer ()
    {
        _vid = new VideoDisplayer();
        addChild(_vid);

        ParameterUtil.getParameters(this, gotParams);
    }

    /**
     * Our ParameterUtil callback.
     */
    protected function gotParams (params :Object) :void
    {
        _vid.setup(String(params["video"]));
    }

    /** Our video displayer component. */
    protected var _vid :VideoDisplayer;
}
}
