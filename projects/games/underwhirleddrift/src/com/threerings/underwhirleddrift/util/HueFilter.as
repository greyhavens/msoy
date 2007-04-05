package com.threerings.underwhirleddrift.util {

import flash.filters.ColorMatrixFilter

public class HueFilter
{
    public static function getFilter (hue :int) :ColorMatrixFilter
    {
        return new ColorMatrixFilter();
    }
}
}
