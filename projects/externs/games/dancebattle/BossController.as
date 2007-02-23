package {

/**
 * Mostly placeholder for now.
 */
public class BossController extends ControlBackend
{
    public function setWalking (walking :Boolean) :void
    {
        callUserCode("appearanceChanged_v1", [0, 0, 0], 0, walking);
    }
}
}
