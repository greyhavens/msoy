package {

import flash.display.Sprite;

import com.threerings.msoy.client.HeaderClient;

public class header extends Sprite
{
    public function header ()
    {
        new HeaderClient(stage);
    }
}
}
