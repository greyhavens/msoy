////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

import flash.display.LoaderInfo;

[ExcludeClass]

/**
 *  @private
 */
public class Singleton
{
    private static var classMap:Object = {};

	/**
	 *  @private
	 */
    public static function registerClass(name:String, clazz:Class):void
    {
        var c:Class = classMap[name];
        if (!c)
            classMap[name] = clazz;
    }

    /**
     *  @private
     */
    public static function getClass(name:String):Class
    {
        return classMap[name];
    }

    /**
     *  @private
     */
    public static function getInstance(name:String):Object
    {
        var clazz:Class = classMap[name];

        return Object(clazz).getInstance();
    }
}

}

