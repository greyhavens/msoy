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
package samples.restaurant
{
import mx.formatters.DateFormatter;

[Bindable]
public class Review
{
    public function Review(source:Object=null)
    {
        super();
        if (source != null)
        {
            for (var i:String in source)
            {
                try
                {
                    if (i == "reviewDate" && source[i] is String)
                    {
                        // change the format of the Date string 
                        // from yyyy-mm-dd hh:mm:ss.sss to mm/dd/yyyy
                        // in order for the parser to understand
                        var df:DateFormatter = new DateFormatter();
                        this[i] = new Date(Date.parse(df.format(source[i])));
                    }
                    else
                    {                	
                        this[i] = source[i];                    
                    }
                }
                catch (e:Error)
                {
                    //ignore
                }
            }
        }
    }

    public var restaurantId:int;
    public var restaurantName:String;
    public var restaurant:Object;
    public var reviewDate:Date;
    public var reviewer:String;
    public var rating:Number;
    public var title:String;
    public var reviewText:String;
    public var email:String;

}

}
