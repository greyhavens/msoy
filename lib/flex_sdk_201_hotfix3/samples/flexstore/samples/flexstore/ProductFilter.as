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
public class ProductFilter
{
    public var count:int;
    public var series:String;
    public var minPrice:Number;
    public var maxPrice:Number;
    public var triband:Boolean;
    public var camera:Boolean;
    public var video:Boolean;
    
    public function ProductFilter()
    {
        super();
    }
    
    public function accept(product:Product):Boolean
    {
        //price is often the first test so let's fail fast if possible
        if (minPrice > product.price || maxPrice < product.price)
            return false;
        if (series != "All Series" && series != product.series)
            return false;
        if (triband && !product.triband)
            return false;
        if (camera && !product.camera)
            return false;
        if (video && !product.video)
            return false;
        
        return true;
    }
}

}