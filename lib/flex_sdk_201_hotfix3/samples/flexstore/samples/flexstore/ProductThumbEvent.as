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
    
import flash.events.Event;

public class ProductThumbEvent extends Event
{
    public static const PURCHASE:String = "purchase";
    public static const COMPARE:String = "compare";
    public static const DETAILS:String = "details";
    public static const BROWSE:String = "browse";
    
    public var product:Product;
    
    public function ProductThumbEvent(type:String, product:Product)
    {
        super(type);
        this.product = product;
    }
    
    override public function clone():Event
    {
        return new ProductThumbEvent(type, product);
    }
}

}