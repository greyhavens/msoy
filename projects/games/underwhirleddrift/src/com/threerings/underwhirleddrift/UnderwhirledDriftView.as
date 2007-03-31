package com.threerings.underwhirleddrift {

import com.threerings.underwhirleddrift.kart.Kart;
import com.threerings.underwhirleddrift.scene.Bonus;
import com.threerings.underwhirleddrift.scene.Level;

public interface UnderwhirledDriftView
{
    function setKart (kart :Kart) :void;
    function setBonus (bonus :Bonus) :void;
    function clearBonus () :void;
    function setLevel (level :Level) :void;
    function startLightBoard () :void;
}
}
