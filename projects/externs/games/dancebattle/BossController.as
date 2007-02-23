package {

/**
 * Mostly placeholder for now.
 */
public class BossController extends ControlBackend
{
    public function toggleFacing () :void
    {
        _facingRight = !_facingRight;
        update();
    }

    public function setWalking (walking :Boolean) :void
    {
        _walking = walking;
        update();
    }

    protected function update () :void
    {
        callUserCode("appearanceChanged_v1", [0, 0, 0], _facingRight ? 90 : 270, _walking);
    }

    protected var _walking :Boolean;

    protected var _facingRight :Boolean;
}
}
