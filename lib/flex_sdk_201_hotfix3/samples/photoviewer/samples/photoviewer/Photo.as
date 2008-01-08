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
package samples.photoviewer
{
	[Bindable]
	public class Photo
	{
		public var name:String;
		public var description:String;
		public var source:String;
		 
		public function Photo(photo:Object=null)
		{
			if (photo != null)
			{
				fill(photo);
			}
		}
		
		public function fill(photo:Object):void 
		{
			this.name = photo.name;
			this.description = photo.description;
			this.source = photo.source;
		}
	}
}