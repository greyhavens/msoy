/*
APE (Actionscript Physics Engine) is an AS3 open source 2D physics engine
Copyright 2006, Alec Cove 

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Contact: ape@cove.org
*/
package org.cove.ape {
	
	internal final class CollisionDetector {	
		
		/**
		 * Tests the collision between two objects. If there is a collision it is passed off
		 * to the CollisionResolver class to resolve the collision.
		 */	
		internal static function test(objA:AbstractParticle, objB:AbstractParticle):void {	
			
			if (objA.fixed && objB.fixed) return;
		
			// rectangle to rectangle
			if (objA is RectangleParticle && objB is RectangleParticle) {
				testOBBvsOBB(RectangleParticle(objA), RectangleParticle(objB));
			
			// circle to circle
			} else if (objA is CircleParticle && objB is CircleParticle) {
				testCirclevsCircle(CircleParticle(objA), CircleParticle(objB));
	
			// rectangle to circle - two ways
			} else if (objA is RectangleParticle && objB is CircleParticle) {
				testOBBvsCircle(RectangleParticle(objA), CircleParticle(objB));
				
			} else if (objA is CircleParticle && objB is RectangleParticle)  {
				testOBBvsCircle(RectangleParticle(objB), CircleParticle(objA));		
			}
		}
	
	
		/**
		 * Tests the collision between two RectangleParticles (aka OBBs). If there is a collision it
		 * determines its axis and depth, and then passes it off to the CollisionResolver for handling.
		 */
		private static function testOBBvsOBB(ra:RectangleParticle, rb:RectangleParticle):void {
			
			var collisionNormal:Vector;
			var collisionDepth:Number = Number.POSITIVE_INFINITY;
			
			for (var i:int = 0; i < 2; i++) {
		
			    var axisA:Vector = ra.axes[i];
			    var depthA:Number = testIntervals(ra.getProjection(axisA), rb.getProjection(axisA));
			    if (depthA == 0) return;
				
			    var axisB:Vector = rb.axes[i];
			    var depthB:Number = testIntervals(ra.getProjection(axisB), rb.getProjection(axisB));
			    if (depthB == 0) return;
			    
			    var absA:Number = Math.abs(depthA);
			    var absB:Number = Math.abs(depthB);
			    
			    if (absA < Math.abs(collisionDepth) || absB < Math.abs(collisionDepth)) {
			    	var altb:Boolean = absA < absB;
			    	collisionNormal = altb ? axisA : axisB;
			    	collisionDepth = altb ? depthA : depthB;
			    }
			}
			CollisionResolver.resolveParticleParticle(ra, rb, collisionNormal, collisionDepth);
		}		
	
	
		/**
		 * Tests the collision between a RectangleParticle (aka an OBB) and a CircleParticle. 
		 * If there is a collision it determines its axis and depth, and then passes it off 
		 * to the CollisionResolver for handling.
		 */
		private static function testOBBvsCircle(ra:RectangleParticle, ca:CircleParticle):void {
			
			var collisionNormal:Vector;
			var collisionDepth:Number = Number.POSITIVE_INFINITY;
			var depths:Array = new Array(2);
			
			// first go through the axes of the rectangle
			for (var i:int = 0; i < 2; i++) {
	
				var boxAxis:Vector = ra.axes[i];
				var depth:Number = testIntervals(ra.getProjection(boxAxis), ca.getProjection(boxAxis));
				if (depth == 0) return;
	
				if (Math.abs(depth) < Math.abs(collisionDepth)) {
					collisionNormal = boxAxis;
					collisionDepth = depth;
				}
				depths[i] = depth;
			}	
			
			// determine if the circle's center is in a vertex region
			var r:Number = ca.radius;
			if (Math.abs(depths[0]) < r && Math.abs(depths[1]) < r) {
	
				var vertex:Vector = closestVertexOnOBB(ca.curr, ra);
	
				// get the distance from the closest vertex on rect to circle center
				collisionNormal = vertex.minus(ca.curr);
				var mag:Number = collisionNormal.magnitude();
				collisionDepth = r - mag;
	
				if (collisionDepth > 0) {
					// there is a collision in one of the vertex regions
					collisionNormal.divEquals(mag);
				} else {
					// ra is in vertex region, but is not colliding
					return;
				}
			}
			CollisionResolver.resolveParticleParticle(ra, ca, collisionNormal, collisionDepth);
		}
	
	
		/**
		 * Tests the collision between two CircleParticles. If there is a collision it 
		 * determines its axis and depth, and then passes it off to the CollisionResolver
		 * for handling.
		 */	
		private static function testCirclevsCircle(ca:CircleParticle, cb:CircleParticle):void {
			
			var depthX:Number = testIntervals(ca.getIntervalX(), cb.getIntervalX());
			if (depthX == 0) return;
			
			var depthY:Number = testIntervals(ca.getIntervalY(), cb.getIntervalY());
			if (depthY == 0) return;
			
			var collisionNormal:Vector = ca.curr.minus(cb.curr);
			var mag:Number = collisionNormal.magnitude();
			var collisionDepth:Number = (ca.radius + cb.radius) - mag;
			
			if (collisionDepth > 0) {
				collisionNormal.divEquals(mag);
				CollisionResolver.resolveParticleParticle(ca, cb, collisionNormal, collisionDepth);
			}
		}
	
	
		/**
		 * Returns 0 if intervals do not overlap. Returns smallest depth if they do.
		 */
		private static function testIntervals(intervalA:Interval, intervalB:Interval):Number {
			
			if (intervalA.max < intervalB.min) return 0;
			if (intervalB.max < intervalA.min) return 0;
			
			var lenA:Number = intervalB.max - intervalA.min;
			var lenB:Number = intervalB.min - intervalA.max;
			
			return (Math.abs(lenA) < Math.abs(lenB)) ? lenA : lenB;
		}
		
		
		/**
		 * Returns the location of the closest vertex on r to point p
		 */
	 	private static function closestVertexOnOBB(p:Vector, r:RectangleParticle):Vector {
	
			var d:Vector = p.minus(r.curr);
			var q:Vector = new Vector(r.curr.x, r.curr.y);
	
			for (var i:int = 0; i < 2; i++) {
				var dist:Number = d.dot(r.axes[i]);
	
				if (dist >= 0) dist = r.extents[i];
				else if (dist < 0) dist = -r.extents[i];
	
				q.plusEquals(r.axes[i].mult(dist));
			}
			return q;
		}
	}
}
