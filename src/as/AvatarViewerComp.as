package {

import mx.binding.utils.BindingUtils;

import mx.containers.Canvas;
import mx.controls.CheckBox;
import mx.controls.HSlider;
import mx.controls.Label;

import com.threerings.msoy.ui.Grid;

public class AvatarViewerComp extends Canvas
{
    public function AvatarViewerComp ()
    {
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var params :Object = this.root.loaderInfo.parameters;
        _avatar = new ViewerAvatarSprite(String(params["avatar"]));

        var rotation :HSlider = new HSlider();
        rotation.minimum = 0;
        rotation.maximum = 360;
        rotation.snapInterval = 1;
        rotation.liveDragging = true;

        var walking :CheckBox = new CheckBox();

        var rotLabel :Label = new Label();
        rotLabel.text = "Facing angle:";

        var walkLabel :Label = new Label();
        walkLabel.text = "Walking:";

        var grid :Grid = new Grid();

        grid.addRow(walkLabel, walking);
        grid.addRow(rotLabel, rotation);
        grid.addRow(_avatar, [2, 1]);

        addChild(grid);


        BindingUtils.bindSetter(function (val :Number) :void {
            _avatar.setOrientation(int(val));
        }, rotation, "value");

        BindingUtils.bindSetter(function (val :Boolean) :void {
            _avatar.setMoving(val);
        }, walking, "selected");
    }

    protected var _avatar :ViewerAvatarSprite;
}
}

import com.threerings.msoy.world.client.BaseAvatarSprite;

class ViewerAvatarSprite extends BaseAvatarSprite
{
    public function ViewerAvatarSprite (url :String)
    {
        super(null);
        setMedia(url);
    }

    public function setMoving (moving :Boolean) :void
    {
        _moving = moving;
        stanceDidChange();
    }

    override public function setOrientation (orient :int) :void
    {
        super.setOrientation(orient);
        stanceDidChange();
    }

    override public function isMoving () :Boolean
    {
        return _moving;
    }

    protected var _moving :Boolean = false;
}
