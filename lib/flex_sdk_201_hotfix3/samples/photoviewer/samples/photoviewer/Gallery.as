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
	import mx.collections.ICollectionView;
	import mx.collections.ArrayCollection;
	import mx.collections.IViewCursor;
	
	[Bindable]
	public class Gallery
	{
		public var name:String;
		public var description:String;
		public var photos:ArrayCollection;
		public var selected:int;

		private var photo:Photo;
		
		public function Gallery(gallery:Object=null)
		{
			photos = new ArrayCollection();
			if (gallery != null)
			{
				fill(gallery);
			}
		}
		
		public function fill(gallery:Object):void
		{
			this.name = gallery.id;
			this.description = gallery.description;
			this.selected = 0;
			
			for (var i:int=0; i < gallery.photo.length; i++)
			{
				photo = new Photo(gallery.photo[i]);
				photos.addItem(photo);
			}
		}
	}
}