////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.treeClasses
{

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.utils.Dictionary;
import mx.collections.ICollectionView;
import mx.collections.IViewCursor;
import mx.collections.Sort;
import mx.collections.errors.ItemPendingError;
import mx.core.EventPriority;
import mx.events.CollectionEvent;
import mx.events.CollectionEventKind;
import mx.events.PropertyChangeEvent;
import mx.utils.UIDUtil;
import mx.collections.IList;

[ExcludeClass]

/**
 *  @private
 *  This class provides a hierarchical view of a standard collection.
 *  It is used by Tree to parse user data.
 */
public class HierarchicalCollectionView extends EventDispatcher
										implements ICollectionView
{
    include "../../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function HierarchicalCollectionView(
							model:ICollectionView,
							treeDataDescriptor:ITreeDataDescriptor,
							argOpenNodes:Object = null)
	{
		super();

		parentMap = {};

		childrenMap = new Dictionary(true);

		treeData = model;

		// listen for add/remove events from developer as weak reference
		treeData.addEventListener(CollectionEvent.COLLECTION_CHANGE,
								  collectionChangeHandler,
								  false,
								  EventPriority.DEFAULT_HANDLER,
								  true);
								  
		addEventListener(CollectionEvent.COLLECTION_CHANGE, 
								  expandEventHandler, 
								  false, 
								  0, 
								  true);
				
		dataDescriptor = treeDataDescriptor;
		openNodes = argOpenNodes;
		//calc initial length
		currentLength = calculateLength();
	}

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private var dataDescriptor:ITreeDataDescriptor;

	/**
	 *  @private
	 */
	private var treeData:ICollectionView;

	/**
	 *  @private
	 */
	private var cursor:HierarchicalViewCursor;

	/**
	 *  @private
	 *  The total number of nodes we know about.
	 */
	private var currentLength:int;

	/**
	 *  @private
	 */
	public var openNodes:Object;

	/**
	 *  @private
	 *  Mapping of UID to parents.  Must be maintained as things get removed/added
	 *  This map is created as objects are visited
	 */
	public var parentMap:Object;

	/**
	 *  @private
	 *  Mapping of nodes to children.  Used by getChildren.
	 */
	private var childrenMap:Dictionary;

    //----------------------------------
	//  filter
    //----------------------------------

    /**
     *  Not Supported in Tree.
     */
    public function get filterFunction():Function
    {
        return null;
    }

    /**
     *  Not Supported in Tree.
     */
    public function set filterFunction(value:Function):void
    {
        //No Impl.
    }

    //----------------------------------
	//  length
    //----------------------------------

    /**
     *  The length of the currently parsed collection.  This
     *  length only includes nodes that we know about.
     */
	public function get length():int
	{
		return currentLength;
	}

    //----------------------------------
	//  sort
    //----------------------------------
    /**
    *  @private
     *  Not Supported in Tree.
     */
    public function get sort():Sort
	{
	    return null;
	}

    /**
     *  @private
     *  Not Supported in Tree.
     */
    public function set sort(value:Sort):void
	{
	    //No Impl
	}

    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------

	/**
	 *  Returns the parent of a node.  Top level node's parent is null
	 *  If we don't know the parent we return undefined.
	 */
    public function getParentItem(node:Object):*
    {
		var uid:String = UIDUtil.getUID(node);
		if (parentMap.hasOwnProperty(uid))
			return parentMap[uid];

		return undefined;
	}
	
	/**
	 *  @private
	 *  Calculate the total length of the collection, but only count nodes
	 *  that we can reach.
	 */
	public function calculateLength(node:Object = null, parent:Object = null):int
	{
		var length:int = 0;
		var childNodes:ICollectionView;

		if (!node)
		{
			var modelOffset:int = 0;
			// special case counting the whole thing
			// watch for page faults
			var modelCursor:IViewCursor = treeData.createCursor();
			while (!modelCursor.afterLast)
			{
				node = modelCursor.current;
				length += calculateLength(node, null) + 1;
				modelOffset++;
				try
				{
					modelCursor.moveNext();
				}
				catch (e:ItemPendingError)
				{
					// just stop where we are, no sense paging
					// the whole thing just to get length. make a rough
					// guess assuming that all un-paged nodes are closed
					length += treeData.length - modelOffset;
					return length;
				}
			}
		}
		else
		{
			var uid:String = UIDUtil.getUID(node);
			parentMap[uid] = parent;
			if (node != null &&
				openNodes[uid] &&
				dataDescriptor.isBranch(node, treeData) &&
				dataDescriptor.hasChildren(node, treeData))
			{
				childNodes = getChildren(node);
				if (childNodes != null)
				{
					var numChildren:int = childNodes.length;
					for (var i:int = 0; i < numChildren; i++)
					{
						length += calculateLength(childNodes[i], node) + 1;
					}
				}
			}
		}
		return length;
	}

	/**
	 *  @private
	 *  This method is merely for ICollectionView interface compliance.
	 */
	public function describeData():Object
	{
		return null;
	}

    /**
	 *  Returns a new instance of a view iterator over the items in this view
	 *
     *  @see mx.utils.IViewCursor
     */
    public function createCursor():IViewCursor
	{
		return new HierarchicalViewCursor(
			this, treeData, dataDescriptor, openNodes);
	}

    /**
     *  Checks the collection for item using standard equality test.
     */
    public function contains(item:Object):Boolean
	{
		var cursor:IViewCursor = createCursor();
		var done:Boolean = false;
		while (!done)
		{
			if (cursor.current == item)
				return true;
			done = cursor.moveNext();
		}
		return false;
	}

    /**
     *  @private
     */
	public function disableAutoUpdate():void
	{
	    //no-op
    }

    /**
     *  @private
     */
    public function enableAutoUpdate():void
    {
        //no-op
    }

	/**
	 *  @private
	 */
	public function itemUpdated(item:Object, property:Object = null,
                                oldValue:Object = null,
                                newValue:Object = null):void
    {
	    var event:CollectionEvent =
			new CollectionEvent(CollectionEvent.COLLECTION_CHANGE);
	    event.kind = CollectionEventKind.UPDATE;

		var objEvent:PropertyChangeEvent =
			new PropertyChangeEvent(PropertyChangeEvent.PROPERTY_CHANGE);
	    objEvent.property = property;
	    objEvent.oldValue = oldValue;
	    objEvent.newValue = newValue;
	    event.items.push(objEvent);

		dispatchEvent(event);
    }

	/**
	 *  @private
	 */
	public function refresh():Boolean
	{
	    var event:CollectionEvent =
			new CollectionEvent(CollectionEvent.COLLECTION_CHANGE);
	    event.kind = CollectionEventKind.REFRESH;
	    dispatchEvent(event);

		return true;
    }
    
    /**
	 * @private
	 * delegate getchildren in order to add event listeners for nested collections
	 */
	private function getChildren(node:Object):ICollectionView
	{
		var children:ICollectionView = dataDescriptor.getChildren(node, treeData);
		var oldChildren:ICollectionView = childrenMap[node];
		if (oldChildren != children)
		{
		    if (oldChildren != null)
		    {
				oldChildren.removeEventListener(CollectionEvent.COLLECTION_CHANGE,
									  nestedCollectionChangeHandler);
			}
    		children.addEventListener(CollectionEvent.COLLECTION_CHANGE,
    									  nestedCollectionChangeHandler, false, 0, true);
		    childrenMap[node] = children;
		}
		return children;
	}
    
    /**
	 * @private
	 * Force a recalulation of length	
	 */
	 private function updateLength(node:Object=null, parent:Object = null):void
	 {
	 	currentLength = calculateLength();
	 }

	/**
	 * @private
	 *  Fill the node array with the node and all of its visible children
	 *  update the parentMap as you go.
	 */
	private function getVisibleNodes(node:Object, parent:Object, nodeArray:Array):void
	{
		var childNodes:ICollectionView;
		nodeArray.push(node);

		var uid:String = UIDUtil.getUID(node);
		parentMap[uid] = parent;
		if (openNodes[uid] &&
			dataDescriptor.isBranch(node, treeData) &&
			dataDescriptor.hasChildren(node, treeData))
		{
			childNodes = getChildren(node);
			if (childNodes != null)
			{
				var numChildren:int = childNodes.length;
				for (var i:int = 0; i < numChildren; i++)
				{
					getVisibleNodes(childNodes[i], node, nodeArray);
				}
			}
		}
	}

	/**
	 *  @private
	 *  Factor in the open children before this location in the model
	 */
	private function getVisibleLocation(oldLocation:int):int
	{
		var newLocation:int = 0;
		var modelCursor:IViewCursor = treeData.createCursor();
		for (var i:int = 0; i < oldLocation && !modelCursor.afterLast; i++)
		{
			newLocation += calculateLength(modelCursor.current, null) + 1;
			modelCursor.moveNext();
		}
		return newLocation;
	}

	/**
	 * @private
	 * factor in the open children before this location in a sub collection
	 */
	private function getVisibleLocationInSubCollection(parent:Object, oldLocation:int):int
	{
		var newLocation:int = oldLocation;
		var target:Object = parent;
		parent = getParentItem(parent);
		var children:ICollectionView;
		var cursor:IViewCursor;
		while (parent)
		{
			children = childrenMap[parent];
			cursor = children.createCursor();
			while (!cursor.afterLast)
			{
				if (cursor.current == target)
				{
					newLocation++;
					break;
				}
				newLocation += calculateLength(cursor.current, parent) + 1;
				cursor.moveNext();
			}
			target = parent;
			parent = getParentItem(parent);
		}
		cursor = treeData.createCursor();
		while (!cursor.afterLast)
		{
			if (cursor.current == target)
			{
				newLocation++;
				break;
			}
			newLocation += calculateLength(cursor.current, parent) + 1;
			cursor.moveNext();
		}
		return newLocation;
	}

    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	public function collectionChangeHandler(event:CollectionEvent):void
	{
		var i:int;
		var n:int;
		var location:int;
		var uid:String;
		var parent:Object;
		var node:Object;
		var items:Array;

		var convertedEvent:CollectionEvent;
		
		if (event is CollectionEvent)
        {
            var ce:CollectionEvent = CollectionEvent(event);
            if (ce.kind == CollectionEventKind.RESET)
            {
            	updateLength();
            	dispatchEvent(event);
            }
            else if (ce.kind == CollectionEventKind.ADD)
            {
				n = ce.items.length;
				convertedEvent = new CollectionEvent(
        								CollectionEvent.COLLECTION_CHANGE,
										false, 
										true,
										ce.kind);
				convertedEvent.location = getVisibleLocation(ce.location);
				for (i = 0; i < n; i++)
				{
					node = ce.items[i];
					getVisibleNodes(node, null, convertedEvent.items);
				}
				currentLength += convertedEvent.items.length;
            	dispatchEvent(convertedEvent);
            }
            else if (ce.kind == CollectionEventKind.REMOVE)
            {
				n = ce.items.length;
				convertedEvent = new CollectionEvent(
        								CollectionEvent.COLLECTION_CHANGE,
										false, 
										true,
										ce.kind);
				convertedEvent.location = getVisibleLocation(ce.location);
				for (i = 0; i < n; i++)
				{
					node = ce.items[i];
					getVisibleNodes(node, null, convertedEvent.items);
				}
				currentLength -= convertedEvent.items.length;
            	dispatchEvent(convertedEvent);
            }
            else if (ce.kind == CollectionEventKind.UPDATE)
            {
				// so far, nobody cares about the details so just
				// send it
            	dispatchEvent(event);
            }
            else if (ce.kind == CollectionEventKind.REPLACE)
            {
            	// someday handle case where node is marked as open
				// before it becomes the replacement.
				// for now, just pass on the data and remove
				// old visible rows
				n = ce.items.length;
				convertedEvent = new CollectionEvent(
        								CollectionEvent.COLLECTION_CHANGE,
										false, 
										true,
										CollectionEventKind.REMOVE);

				for (i = 0; i < n; i++)
				{
					node = ce.items[i].oldValue;
					getVisibleNodes(node, null, convertedEvent.items);
				}

				// prune the replacements from this list
				var j:int = 0;
				for (i = 0; i < n; i++)
				{
					node = ce.items[i].oldValue;
					while (convertedEvent.items[j] != node)
						j++;
					convertedEvent.items.splice(j, 1);
				}
				if (convertedEvent.items.length)
				{
					currentLength -= convertedEvent.items.length;
					// nobody cares about location yet.
            		dispatchEvent(convertedEvent);
				}
            	dispatchEvent(event);
            }
        }
	}

	/**
	 *  @private
	 */
	public function nestedCollectionChangeHandler(event:CollectionEvent):void
	{
		var i:int;
		var n:int;
		var location:int;
		var uid:String;
		var parent:Object;
		var node:Object;
		var items:Array;
		var convertedEvent:CollectionEvent;

		if (event is CollectionEvent)
        {
            var ce:CollectionEvent = CollectionEvent(event);
            if (ce.kind == CollectionEventKind.mx_internal::EXPAND)
            {
            	event.stopImmediatePropagation();
            }
            else if (ce.kind == CollectionEventKind.ADD)
            {
				// optimize someday.  We do a full tree walk so we can
				// not only count how many but find the parents of the
				// new nodes.  A better scheme would be to just
				// increment by the number of visible nodes, but we
				// don't have a good way to get the parents.
            	updateLength();
				n = ce.items.length;
				convertedEvent = new CollectionEvent(
        								CollectionEvent.COLLECTION_CHANGE,
										false, 
										true,
										ce.kind);
				for (i = 0; i < n; i++)
				{
					node = ce.items[i];
					parent = getParentItem(node);
					if (parent)
						getVisibleNodes(node, parent, convertedEvent.items);
				}
				convertedEvent.location = getVisibleLocationInSubCollection(parent, ce.location);
            	dispatchEvent(convertedEvent);
            }
            else if (ce.kind == CollectionEventKind.REMOVE)
            {
				n = ce.items.length;
				convertedEvent = new CollectionEvent(
        								CollectionEvent.COLLECTION_CHANGE,
										false, 
										true,
										ce.kind);
				for (i = 0; i < n; i++)
				{
					node = ce.items[i];
					parent = getParentItem(node);
					if (parent)
						getVisibleNodes(node, parent, convertedEvent.items);
				}
				convertedEvent.location = getVisibleLocationInSubCollection(parent, ce.location);
				currentLength -= convertedEvent.items.length;
            	dispatchEvent(convertedEvent);
            }
            else if (ce.kind == CollectionEventKind.UPDATE)
            {
				// so far, nobody cares about the details so just
				// send it
            	dispatchEvent(event);
            }
	        else if (ce.kind == CollectionEventKind.REPLACE)
            {
            	// someday handle case where node is marked as open
				// before it becomes the replacement.
				// for now, just pass on the data and remove
				// old visible rows
				n = ce.items.length;
				convertedEvent = new CollectionEvent(
        								CollectionEvent.COLLECTION_CHANGE,
										false, 
										true,
										CollectionEventKind.REMOVE);

				for (i = 0; i < n; i++)
				{
					node = ce.items[i].oldValue;
					parent = getParentItem(node);
					if (parent)
						getVisibleNodes(node, parent, convertedEvent.items);
				}

				// prune the replacements from this list
				var j:int = 0;
				for (i = 0; i < n; i++)
				{
					node = ce.items[i].oldValue;
					while (convertedEvent.items[j] != node)
						j++;
					convertedEvent.items.splice(j, 1);
				}
				if (convertedEvent.items.length)
				{
					currentLength -= convertedEvent.items.length;
					// nobody cares about location yet.
            		dispatchEvent(convertedEvent);
				}
            	dispatchEvent(event);
            }
			else if (ce.kind == CollectionEventKind.RESET)
			{
				// removeAll() sends a RESET.
				// when we get a reset we don't know what went away
				// and we don't know how many things went away, so
				// we just fake a refresh as if there was a filter
				// applied that filtered out whatever went away
            	updateLength();
				convertedEvent = new CollectionEvent(
        								CollectionEvent.COLLECTION_CHANGE,
										false, 
										true,
										CollectionEventKind.REFRESH);
           		dispatchEvent(convertedEvent);
			}
        }
	}
	
	/**
	 *  @private
	 */
	public function expandEventHandler(event:CollectionEvent):void
	{
		if (event is CollectionEvent)
        {
            var ce:CollectionEvent = CollectionEvent(event);
            if (ce.kind == CollectionEventKind.mx_internal::EXPAND)
            {
            	event.stopImmediatePropagation();
            	updateLength();  
            }
        }
	}
}

}
