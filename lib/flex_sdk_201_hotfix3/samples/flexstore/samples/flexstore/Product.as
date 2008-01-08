////////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
// All Rights Reserved.
// The following is Sample Code and is subject to all restrictions on such code
// as contained in the End User License Agreement accompanying this product.
// If you have received this file from a source other than Adobe,
// then your use, modification, or distribution of it requires
// the prior written permission of Adobe.
//
////////////////////////////////////////////////////////////////////////////////
package samples.flexstore
{

[Bindable]
public class Product
{

    public var productId:int;
    public var name:String;
    public var description:String;
    public var price:Number;
    public var image:String;
    public var series:String;
    public var triband:Boolean;
    public var camera:Boolean;
    public var video:Boolean;
    public var highlight1:String;
    public var highlight2:String;
    public var qty:int;

    public function Product()
    {

    }

    public function fill(obj:Object):void
    {
        for (var i:String in obj)
        {
            this[i] = obj[i];
        }
    }

    [Bindable(event="propertyChange")]
    public function get featureString():String
    {
    	var str:String = "";
    	if (triband)
    		str += "Tri-band ";

		if (camera)
			str += "Camera ";

		if (video)
			str += "Video";

		return str;
    }

}

}