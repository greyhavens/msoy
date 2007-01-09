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
	
	import flash.display.Sprite;
	import flash.utils.getQualifiedClassName;
	
	
	
	/**
	 * The main engine class. All particles and constraints should be added and removed
	 * through this class.
	 * 
	 */
	public final class APEngine {

		public static const STANDARD:Number = 100;
		public static const SELECTIVE:Number = 200;
		public static const SIMPLE:Number = 300;
		
		/**@private */
		internal static var force:Vector;
		/**@private */
		internal static var masslessForce:Vector;
			
		private static var timeStep:Number;
		private static var particles:Array;
		private static var constraints:Array;
		
		private static var _damping:Number;
		private static var _defaultContainer:Sprite;
		private static var _collisionResponseMode:Number = STANDARD;
	
		
		/**
		 * Initializes the engine. You must call this method prior to adding
		 * any particles or constraints.
		 * 
		 * @param dt The delta time value for the engine. This parameter can be used -- in 
		 * conjunction with speed at which <code>APEngine.step()</code> is called -- to change the speed
		 * of the simulation. Typical values are 1/3 or 1/4. Lower values result in slower,
		 * but more accurate simulations, and higher ones result in faster, less accurate ones.
		 * Note that this only applies to the forces added to particles. If you do not add any
		 * forces, the <code>dt</code> value won't matter.
		 */
		public static function init(dt:Number):void {
			timeStep = dt * dt;
			particles = new Array();
			constraints = new Array();
			
			force = new Vector(0,0);
			masslessForce = new Vector(0,0);
			
			damping = 1;
		}


		/**
		 * The global damping. Values should be between 0 and 1. Higher numbers
		 * result in less damping. A value of 1 is no damping. A value of 0 will
		 * not allow any particles to move. The default is 1.
		 * 
		 * <p>
		 * Damping will slow down your simulation and make it more stable. If you find
		 * that your sim is "blowing up', try applying more damping. 
		 * </p>
		 * 
		 * @param d The damping value. Values should be >=0 and <=1.
		 */
		public static function get damping():Number {
			return _damping;
		}
		
		
		/**
		 * @private
		 */
		public static function set damping(d:Number):void {
			_damping = d;
		}
		
		
		/**
		 * The default container used by the default painting methods of the particles and
		 * constraints. If you wish to use to the built in painting methods you must set 
		 * this first.
		 * 
		 * <p>
		 * For simple prototyping, a default painting method is included in the engine. For
		 * any serious development, you should either subclass or make a composite of the constraints
		 * and particles, and write your own painting methods. If you do that, it is not necessary
		 * to call this function, although you can use it for you own painting methods if you need
		 * a container.
		 * </p>
		 * 
		 * @param s An instance of the Sprite class that will be used as the default container.
		 * 
		 */
		public static function get defaultContainer():Sprite {
			return _defaultContainer;
		}
			
		
		/**
		 * @private
		 */
		public static function set defaultContainer(s:Sprite):void {
			_defaultContainer = s;
		}
		
		
		/**
		 * The collision response mode for the engine. The engine has three different possible
		 * settings for the collisionResponseMode property. Valid values are APEngine.STANDARD, 
		 * APEngine.SELECTIVE, and APEngine.SIMPLE. Those settings go in order from slower and
		 * more accurate to faster and less accurate. In all cases it's worth experimenting to
		 * see what mode suits your sim best.  
		 *
		 * <ul>
		 * <li>
		 * <b>APEngine.STANDARD</b>&mdash;Particles are moved out of collision and then velocity is 
		 * applied. Momentum is conserved and the mass of the particles is properly calculated. This
		 * is the default and most physically accurate setting.<br/><br/>
		 * </li>
		 * 
		 * <li>
		 * <b>APEngine.SELECTIVE</b>&mdash;Similar to the APEngine.STANDARD setting, except only 
		 * previously non-colliding particles have their velocity set. In otherwords, if there are 
		 * multiple collisions on a particle, only the first collision on that particle causes a 
		 * change in its velocity. Both this and the APEngine.SIMPLE setting may give better results
		 * than APEngine.STANDARD when using a large number of colliding particles.<br/><br/>
		 * </li>
		 * 
		 * <li>
		 * <b>APEngine.SIMPLE</b>&mdash;Particles do not have their velocity set after colliding. This
		 * is faster than the other two modes but is the least accurate. Mass is not calculated, and 
		 * there is no conservation of momentum. <br/><br/>
		 * </li>
		 * </ul>
		 */
		public static function get collisionResponseMode():Number {
			return _collisionResponseMode;
		}
		
		
		/**
		 * @private
		 */			
		public static function set collisionResponseMode(m:Number):void {
			_collisionResponseMode = m;	
		}
		
			
		// NEED REMOVE FORCE METHODS, AND A WAY TO ALTER ADDED FORCES
		/**
		 * Adds a force to all particles in the system. The mass of the particle is taken into 
		 * account when using this method, so it is useful for adding forces that simulate effects
		 * like wind. Particles with larger masses will not be affected as greatly as those with
		 * smaller masses. Note that the size (not to be confused with mass) of the particle has
		 * no effect on its physical behavior.
		 * 
		 * @param f A Vector represeting the force added.
		 */ 
		public static function addForce(v:Vector):void {
			force.plusEquals(v);
		}
		
		/**
		 * Adds a 'massless' force to all particles in the system. The mass of the particle is 
		 * not taken into account when using this method, so it is useful for adding forces that
		 * simulate effects like gravity. Particles with larger masses will be affected the same
		 * as those with smaller masses. Note that the size (not to be confused with mass) of 
		 * the particle has no effect on its physical behavior.
		 * 
		 * @param f A Vector represeting the force added.
		 */ 	
		public static function addMasslessForce(v:Vector):void {
			masslessForce.plusEquals(v);
		}
		
		/**
		 * Adds a particle to the engine.
		 * 
		 * @param p The particle to be added.
		 */
		public static function addParticle(p:AbstractParticle):void {
			particles.push(p);
		}
		
		
		/**
		 * Removes a particle to the engine.
		 * 
		 * @param p The particle to be removed.
		 */
		public static function removeParticle(p:AbstractParticle):void {
			var ppos:int = particles.indexOf(p);
			if (ppos == -1) return;
			particles.splice(ppos, 1);
		}
		
		
		/**
		 * Adds a constraint to the engine.
		 * 
		 * @param c The constraint to be added.
		 */
		public static function addConstraint(c:AbstractConstraint):void {
			constraints.push(c);
		}


		/**
		 * Removes a constraint from the engine.
		 * 
		 * @param c The constraint to be removed.
		 */
		public static function removeConstraint(c:AbstractConstraint):void {
			var cpos:int = constraints.indexOf(c);
			if (cpos == -1) return;
			constraints.splice(cpos, 1);
		}
	
	
		/**
		 * Returns an array of every item added to the engine. This includes all particles and
		 * constraints.
		 */
		public static function getAll():Array {
			return particles.concat(constraints);
		}	
	
		
		/**
		 * Returns an array of every particle added to the engine.
		 */
		public static function getAllParticles():Array {
			return particles.slice();
		}	
		
	
		/**
		 * Returns an array of every custom particle added to the engine. A custom
		 * particle is defined as one that is not an instance of the included particle
		 * classes. If you create subclasses of any of the included particle classes, and
		 * add them to the engine using <code>addParticle(...)</code> then they will be
		 * returned by this method. This way you can keep a list of particles you have
		 * created, if you need to distinguish them from the built in particles.
		 */	
		public static function getCustomParticles():Array {
			var customParticles:Array = new Array();
			for (var i:int = 0; i < particles.length; i++) {
				var p:AbstractParticle = particles[i];
				if (isCustomParticle(p)) customParticles.push(p);		
			}
			return customParticles;
		}
		
		
		/**
		 * Returns an array of particles added to the engine whose type is one of the built-in
		 * particle types in the APE. This includes the CircleParticle, WheelParticle, and
		 * RectangleParticle.
		 */			
		public static function getAPEParticles():Array {
			var apeParticles:Array = new Array();
			for (var i:int = 0; i < particles.length; i++) {
				var p:AbstractParticle = particles[i];
				if (! isCustomParticle(p)) apeParticles.push(p);		
			}
			return apeParticles;
		}
		
	
		/**
		 * Returns an array of all the constraints added to the engine
		 */						
		public static function getAllConstraints():Array {
			return constraints.slice();
		}	
	

		/**
		 * The main step function of the engine. This method should be called
		 * continously to advance the simulation. The faster this method is 
		 * called, the faster the simulation will run. Usually you would call
		 * this in your main program loop. 
		 */			
		public static function step():void {
			integrate();
			satisfyConstraints();
			checkCollisions();
		}
		
		
		private static function isCustomParticle(p:AbstractParticle):Boolean {
			var className:String = getQualifiedClassName(p);
			var isWP:Boolean = (className == "org.cove.ape::WheelParticle");
			var isCP:Boolean = (className == "org.cove.ape::CircleParticle");
			var isRP:Boolean = (className == "org.cove.ape::RectangleParticle");
				
			if (! (isWP || isCP || isRP)) return true;
			return false;		
		}


		private static function integrate():void {
			for (var i:Number = 0; i < particles.length; i++) {
				particles[i].update(timeStep);	
			}
		}
	
		
		private static function satisfyConstraints():void {
			for (var n:Number = 0; n < constraints.length; n++) {
				constraints[n].resolve();
			}
		}
	
	
		/**
		 * Checks all collisions between particles and constraints. The following rules apply: 
		 * Particles vs Particles are tested unless either collidable property is set to false.
		 * Particles vs Constraints are not tested by default unless collidable is true.
		 * is called on a SpringConstraint. AngularConstraints are not tested for collision,
		 * but their component SpringConstraints are -- with the previous rule in effect. If
		 * a Particle is attached to a SpringConstraint it is never tested against that 
		 * SpringConstraint for collision
		 */
		private static function checkCollisions():void {
			for (var j:Number = 0; j < particles.length; j++) {
				
				var pa:AbstractParticle = particles[j];
				
				for (var i:Number = j + 1; i < particles.length; i++) {
					var pb:AbstractParticle = particles[i];
					if (pa.collidable && pb.collidable) {
						CollisionDetector.test(pa, pb);
					}
				}
				
				for (var n:Number = 0; n < constraints.length; n++) {
					if (constraints[n] is AngularConstraint) continue;
					var c:SpringConstraint = constraints[n];
					if (pa.collidable && c.collidable && ! c.isConnectedTo(pa)) {
						CollisionDetector.test(pa, c.getCollisionRect());
					}
				}
				pa.isColliding = false;	
			}
		}
	}	
}
