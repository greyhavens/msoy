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
    import flash.geom.Point;

    import mx.core.Application;
    import mx.core.UIComponent;
    import mx.effects.Move;
    import mx.effects.Parallel;
    import mx.effects.Resize;

    public class Carousel extends UIComponent
    {

		private var picPos:Array;
		private var picDimension:Array;

        public function Carousel()
        {
            super();
        }

		override protected function createChildren():void
		{
		    super.createChildren();
		    for (var i:int = 0; i < 9; i++)
		    {
		        addChild(new CarouselImage());
		    }
		}

		private var _gallery:Gallery;

		[Bindable]
		public function get gallery():Gallery
		{
			return _gallery;
		}

		public function set gallery(value:Gallery):void
		{
			_gallery = value;
		}


		public function reset():void
		{
			var selected:int = gallery.selected;
			var appWidth:int = Application.application.width;
			var compHeight:int = (Application.application.height - 90)*.8;

			picDimension = new Array(9);
			picPos = new Array(9);

			picDimension[0] = appWidth*.05;
			picDimension[1] = appWidth*.05;
			picDimension[2] = appWidth*.10;
			picDimension[3] = appWidth*.15;
			picDimension[4] = appWidth*.4;
			picDimension[5] = appWidth*.15;
			picDimension[6] = appWidth*.10;
			picDimension[7] = appWidth*.05;
			picDimension[8] = appWidth*.05;

			picPos[0] = new Point(appWidth*(-.075) - 8, (compHeight - picDimension[0])/2);
			picPos[1] = new Point(appWidth*(-.025), (compHeight - picDimension[1])/2);
			picPos[2] = new Point(appWidth*.025 + 8, (compHeight - picDimension[2])/2);
			picPos[3] = new Point(appWidth*.125 + 16, (compHeight - picDimension[3])/2);
			picPos[4] = new Point(appWidth*.275 + 24, (compHeight - picDimension[4])/2);
			picPos[5] = new Point(appWidth*.675 + 32, (compHeight - picDimension[5])/2);
			picPos[6] = new Point(appWidth*.825 + 40, (compHeight - picDimension[6])/2);
			picPos[7] = new Point(appWidth - appWidth*.025, (compHeight - picDimension[7])/2);
			picPos[8] = new Point(appWidth + appWidth*.025 + 8, (compHeight - picDimension[8])/2);

			for (var i:int=0; i < 9; i++)
			{
			    var image:CarouselImage = getChildAt(i) as CarouselImage;
			    var pos:int = selected + i - 4;

			    if (pos >= 0 && pos < gallery.photos.length)
			    {
			        image.width = picDimension[i];
			        image.height = picDimension[i];
			        image.x = picPos[i].x;
			        image.y = picPos[i].y;
			        image.source = "galleries/" + gallery.photos.getItemAt(pos).source;
			        image.visible = true;
				}
				else
				{
				    image.visible = false;
				}
			}
		}

        /**
         * Add move and resize effects to the images specified.
         * @param direction +1 if you're rotating right, -1 if left
         * @param start where in the child list do you start playing effects (avoiding the offscreen one)
         * @param end where in the child list do you stop playing effects (avoiding the offscreen one)
         */
		private function playEffects(direction:int, start:int, end:int):void
		{
		    var parallel:Parallel = new Parallel();

		    for (var i:int=start; i < end; i++)
		    {
		        var image:CarouselImage = getChildAt(i) as CarouselImage;
		        if (image.visible)
		        {
		            var idx:int = i + direction;
		            var move:Move = new Move();
		            move.target = image;
		            move.duration = 1000;
		            move.xTo = picPos[idx].x;
		            move.yTo = picPos[idx].y;
		            parallel.addChild(move);

		            var resize:Resize = new Resize();
		            resize.target = image;
		            resize.duration = 1000;
		            resize.widthTo = picDimension[idx];
		            resize.heightTo = picDimension[idx];
		            parallel.addChild(resize);
		        }
		    }

			parallel.play();
		}

		public function rotateLeft():void
		{
			playEffects(-1, 1, 9);

			var offscreen:CarouselImage = getChildAt(0) as CarouselImage;
			this.removeChild(offscreen);
			//in an ideal world you'd re-use this image but we found
			//that if click the rotate buttons fast enough the offscreen
			//image will appear and fly across the screen because it was
			//playing in a previous effect
			//stopping the effect in the middle as soon as you know this
			//image is offscreen makes the image drop offscreen and
			//isn't a great visual effect
			//there are probably other ways to solve this like preventing
			//the rapid click of the next/prev buttons
			offscreen = new CarouselImage();
	        offscreen.move(picPos[8].x, picPos[8].y);
            offscreen.width = picDimension[8];
            offscreen.height = picDimension[8];
	        addChild(offscreen);
	        if (gallery.selected + 4 < gallery.photos.length)
	        {
	            offscreen.source = "galleries/" + gallery.photos.getItemAt(gallery.selected + 4).source;
	            offscreen.visible = true;
	        }
	        else
	        {
	            offscreen.source = "";
	            offscreen.visible = false;
	        }

		}

		public function rotateRight():void
		{
		    playEffects(1, 0, 8);

			var offscreen:CarouselImage = getChildAt(8) as CarouselImage;
			this.removeChild(offscreen);
			//in an ideal world you'd re-use this image but we found
			//that if click the rotate buttons fast enough the offscreen
			//image will appear and fly across the screen because it was
			//playing in a previous effect
			//stopping the effect in the middle as soon as you know this
			//image is offscreen makes the image drop offscreen and
			//isn't a great visual effect
			//there are probably other ways to solve this like preventing
			//the rapid click of the next/prev buttons
			offscreen = new CarouselImage();
	        offscreen.move(picPos[0].x, picPos[0].y);
            offscreen.width = picDimension[0];
            offscreen.height = picDimension[0];
	        addChildAt(offscreen, 0);

	        if (gallery.selected - 4 >= 0)
	        {
	            offscreen.source = "galleries/" + gallery.photos.getItemAt(gallery.selected - 4).source;
	            offscreen.visible = true;
	        }
	        else
	        {
	            offscreen.source = "";
	            offscreen.visible = false;
	        }
    	}
    }
}