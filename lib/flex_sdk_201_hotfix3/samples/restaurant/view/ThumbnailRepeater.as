package view
{
	import mx.containers.VBox;
	import mx.collections.ArrayCollection;
	import flash.events.Event;
	import samples.restaurant.Review;
	import flash.events.FocusEvent;
	import flash.display.DisplayObject;
	import mx.managers.IFocusManager;
	import mx.managers.IFocusManagerComponent;
	import flash.events.KeyboardEvent;
	import flash.ui.Keyboard;
	import flash.accessibility.Accessibility;
	
	/*
	 * This is a custom repeater that makes a very nice custom component.
	 * 
	 * This component is intended to be very similar to a DataGrid except
	 * with custom rendering.  A DataGrid with a custom cell renderer can
	 * accomplish the same thing but cell renders do not work well with 
	 * accessibility.  
	 
	 * There is a fair amount of work to get it to have similar behavior 
	 * to a DataGrid.
	 */
	public class ThumbnailRepeater extends VBox 
										implements IFocusManagerComponent 
	{
		/* this is a constant for JAWS that indicates the component has been selected */
		private static const EVENT_OBJECT_SELECTION:uint = 0x8006;  
		
		public var restaurant:Object;
		
		private var _dataProvider:ArrayCollection;
		private var caretIndex:int = -1;
		
		/* the bottom and top pixels keep track of the top most and bottom most region of the repeater
		   that is shown.  It is used to determine scrolling*/
		private var _topPixel:int = 0;
		private var _bottomPixel:int = 0;
		
		private var _beginTabIndex:int;
		
		public function ThumbnailRepeater() 
		{
			super();
			setStyle("verticalGap", 0);
			this.tabEnabled = true;
			this.focusEnabled = true;	
		}		

		
		[Bindable]
		public function get beginTabIndex():int
		{
			return _beginTabIndex;
		}
		
		public function set beginTabIndex(value:int):void 
		{
			/* the tab index of the repeater is going to be greater than any of its children.
			   This gives nice cycles for tab order.  For example, in the new state, tabbing from 
			   the cancel button takes you back around to the top (where the rating stars are */
			_beginTabIndex = value + 1;
			this.tabIndex = value + 20;
		}
		/* since we need to boundary check so often, it's easiest to just write 
		   setters and getters */
		private function get topPixel():int
		{
			return _topPixel;
		}
		private function set topPixel(value:int):void 
		{
			if (value < 0)
				_topPixel = 0;
			else
				_topPixel = value;
		}
		private function get bottomPixel():int 
		{
			return _bottomPixel;
		}
		private function set bottomPixel(value:int):void
		{
			if (value > measuredHeight)
				_bottomPixel = measuredHeight;
			else
				_bottomPixel = value;
		}
		
		public function set dataProvider(dp:ArrayCollection):void 
		{
			_dataProvider = dp;
			
			this.removeAllChildren();
			
			for (var i:int = 0; i < _dataProvider.length; i++) 
			{
				var t:ReviewThumbnail = new ReviewThumbnail();
				t.percentWidth = 100;
				t.review = _dataProvider[i];
				t.review.restaurantName = restaurant.name;
				if (i % 2 == 1) 
					t.bgColor = ReviewThumbnail.DARK_BACKGROUND;
				else
					t.bgColor = ReviewThumbnail.LIGHT_BACKGROUND;
				t.currentState = "summary";
			
				this.addChild(t);
			}
			caretIndex = 0;
			_bottomPixel = _topPixel + height;
		}
		
		[Bindable]
		public function get dataProvider():ArrayCollection 
		{
			return _dataProvider;
		}
		
		public function cancelNewReview():void 
		{
			this.removeChildAt(0);
		}
		
		public function addNewReview():void 
		{
			/* the view should scroll up to the top */
			verticalScrollPosition = 0;
			
			if (restaurant != null && (numChildren == 0 || 
					ReviewThumbnail(getChildAt(0)).currentState != "new")) 
			{
				var t:ReviewThumbnail = new ReviewThumbnail();
          		t.percentWidth = 100;
          		var r:Review = new Review();
          		r.restaurantId = restaurant.restaurantId;
          		r.restaurantName = restaurant.name;
          		r.restaurant = restaurant;
          		t.review = r;
          		
          		if (numChildren > 0 && 
          					ReviewThumbnail(getChildAt(0)).bgColor == 
          									ReviewThumbnail.LIGHT_BACKGROUND)
          			t.bgColor = ReviewThumbnail.DARK_BACKGROUND;
          		else
          			t.bgColor = ReviewThumbnail.LIGHT_BACKGROUND;
          		
          		t.currentState = "new";
          		
          		this.addChildAt(t, 0);	
          		caretIndex = 0;         		
			}
		}
		
		/* helper method to determine if accessibility is turned on */
		private function hasAccessibility():Boolean
		{
			return this.accessibilityProperties != null;
		}
		
		/* event handlers */
		
		override protected function focusOutHandler(event:FocusEvent):void
		{
			if (hasAccessibility())
				this.accessibilityProperties.description = "";
			super.focusOutHandler(event);
		}
		
		/* when this component gains focus, it passes it onto one of its children.
		   It manages focus and keyboard navigation for its children*/
		override protected function focusInHandler(event:FocusEvent):void 
		{				
			/* If the custom repeater has no items, focus should skip to the
			   next component */
			if (this.numChildren == 0) 
			{
				focusManager.getNextFocusManagerComponent();
				return;
			}

			/* else focus should go to the selected item */
			if (isOurFocus(DisplayObject(event.target))) 
			{
				var fm:IFocusManager = focusManager;
				if (fm && fm.showFocusIndicator) 
				{
					if (ReviewThumbnail(this.getChildAt(caretIndex)) != null) 
					{
						selectCurrentIn();
						/* it seems the default behavior for mx:Box is to remove 
						   keyboard handlers if there is no scroll bar  - this is not what we want, make sure there
						   is a handler for this event */
						if (!this.hasEventListener(KeyboardEvent.KEY_DOWN))
							this.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
					}
				}
			}
		}
		
		/* override the keyDownHandler to get more data grid like behavior.  This
		   allows the repeater to be keyboard accessible. */
		override protected function keyDownHandler(event:KeyboardEvent):void 
		{	
			/* if it has no children ignore the event */		
			if (this.numChildren == 0) 
			{
				return;
			}
			
			/* Many of the keyboard events will cause scrolling.  These include
			   the down arrow, up arrow, page down, page up, home and end.  This is
			   down fairly easily by just setting the scrolling position. */
			switch (event.keyCode) 
			{
				case Keyboard.UP:
					moveSelectionUp();
					break;
				
				case Keyboard.DOWN:
					moveSelectionDown();
					break;
				
				case Keyboard.PAGE_UP:
					pageUp();
					break;
					
				case Keyboard.PAGE_DOWN:
					pageDown();
					break;

				case Keyboard.HOME:
					ReviewThumbnail(this.getChildAt(caretIndex)).selectedOut();
					caretIndex = 0;
					selectCurrentIn();
					verticalScrollPosition = 0;
					topPixel = 0;
					bottomPixel = topPixel + height;
					break;
				
				case Keyboard.END:	
					ReviewThumbnail(this.getChildAt(caretIndex)).selectedOut();
					caretIndex = _dataProvider.length - 1;
					selectCurrentIn();
					verticalScrollPosition = measuredHeight;
					bottomPixel = measuredHeight;
					topPixel = bottomPixel - height;
					break;
					 
				case Keyboard.SPACE:
					var t:ReviewThumbnail = ReviewThumbnail(this.getChildAt(caretIndex));
					t.toggle();
					if (t.currentState == "details")
						focusManager.setFocus(t.title);	
					else
						focusManager.setFocus(this);
					break;	
				
				case Keyboard.TAB:
					if (ReviewThumbnail(this.getChildAt(caretIndex)).currentState == "details")
						break;
					if (this.getChildAt(caretIndex))
						ReviewThumbnail(this.getChildAt(caretIndex)).selectedOut();
					break;
					
			}
		}
				
		/* page up and page down will have identical behavior to a data grid */
		private function pageUp():void
		{
			var t:ReviewThumbnail = ReviewThumbnail(this.getChildAt(caretIndex));
			var scrollAmount:int = 0;
			t.selectedOut();
			
			/* if we are already at the top of the pane, move one pane up - needs scrolling */
			if ( (t.y - t.height) < topPixel) {
				scrollAmount = height;
				topPixel -= scrollAmount;
			}
			
			/* select the top review of the pane */
			caretIndex = topPixel / t.height;
			if (caretIndex < 0)
				caretIndex = 0;
			selectCurrentIn();
			
			/* adjust the scroll so things fit nicely */
			t = ReviewThumbnail(this.getChildAt(caretIndex));
			var diff:int = topPixel - t.y;
			scrollAmount += diff;
			topPixel -= diff;
			bottomPixel = topPixel + height; 
			
			verticalScrollPosition -= scrollAmount;
		}
		
		private function pageDown():void
		{
			if (caretIndex == _dataProvider.length - 1)
				return;
				
			var t:ReviewThumbnail = ReviewThumbnail(this.getChildAt(caretIndex));
			var scrollAmount:int = 0;
			t.selectedOut();
			
			/* we are at the bottom entry of the pane, need to scroll down */
			if ( (t.y + t.height + t.height) > bottomPixel) 
			{
				scrollAmount = height;
				bottomPixel += scrollAmount;
				topPixel = bottomPixel - height;
			}
			
			/* select the right review - always pick the bottom most that fits */
			caretIndex = bottomPixel / t.height - 1;
			if (caretIndex > _dataProvider.length - 1) 
				caretIndex = _dataProvider.length - 1;
			selectCurrentIn();
			
			/* adjust the scroll amount to fit the entire review */
			t = ReviewThumbnail(this.getChildAt(caretIndex)); 
			var diff:int = (bottomPixel - (t.y + t.height));
			scrollAmount -= diff;
			topPixel -= diff;
			bottomPixel = topPixel + height;
			
			verticalScrollPosition += scrollAmount;
		}
		
		/* selects the next item down.  This method is called when the user 
		   hits the down key.  There is some simply boundary checking and 
		   making sure the focus ends up in the right spot 
		   
		   performs scrolling if necessary */
		private function moveSelectionDown():void 
		{
			//changes the selection
			if (caretIndex != _dataProvider.length - 1) 
			{
				ReviewThumbnail(this.getChildAt(caretIndex)).selectedOut();
				caretIndex++;
				selectCurrentIn();
			}
			
			//scrolling
			var t:ReviewThumbnail = ReviewThumbnail(this.getChildAt(caretIndex));
			if ( (t.y + t.height) > bottomPixel) 
			{
				var scrollSize:Number = t.height;
				topPixel += scrollSize;
				bottomPixel = topPixel + height;
				verticalScrollPosition += scrollSize;
			}
		}
		
		/* selects the next item up.  This method is called when the user 
		   hits the up key.  There is some simply boundary checking and 
		   making sure the focus ends up in the right spot 
		   
		   performs scrolling if necessary */
		private function moveSelectionUp():void 
		{
			//changes the selection
			if (caretIndex >= 1) 
			{
				ReviewThumbnail(this.getChildAt(caretIndex)).selectedOut();
				caretIndex--;
				selectCurrentIn();
			}
			
			//performs scrolling
			var t:ReviewThumbnail = ReviewThumbnail(this.getChildAt(caretIndex));
			if (t.y < topPixel) 
			{
				var scrollSize:Number = t.height;
				topPixel -= scrollSize;
				bottomPixel = topPixel + height;
				verticalScrollPosition -= scrollSize;
			}
		}
		
		/* selects the current item at caretIndex */
		private function selectCurrentIn():void 
		{
			var newThumb:ReviewThumbnail = ReviewThumbnail(this.getChildAt(caretIndex));
			newThumb.selectedIn(caretIndex);
			if (newThumb.currentState == "details") 
			{
				focusManager.setFocus(newThumb.title);
			}
			else if (newThumb.currentState == "new")
			{
				focusManager.setFocus(newThumb.newRating);
			}
			else if (hasAccessibility())
			{
				/* by moving the selection, a new review has been selected and
				 we would like JAWS to read the information for the new review */
				this.accessibilityProperties.description = newThumb.accessibleText;
				Accessibility.sendEvent(this, 0, EVENT_OBJECT_SELECTION);
				Accessibility.updateProperties();
			}
		}
}
}
