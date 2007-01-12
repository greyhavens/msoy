package {

import Math;

/**
 * Simple 2d Elastic Collision Algorithm
 * taken from here: http://www.geocities.com/vobarian/2dcollisions/
 */
public class ElasticCollision {

    public function collide( 
        p1:Array, v1: Array, m1: Number,
        p2:Array, v2: Array, m2: Number) :Array
    {
        // step 1:
        const n:Array = new Array(p2[0] - p1[0], p2[1] - p1[1]);
        const un:Array = div(n, abs(n));
        const ut:Array = new Array(-un[1], un[0]);
        
        // step 2:
        //...already done
        
        // step 3:
        const v1n:Number = dot(un, v1);
        const v1t:Number = dot(ut, v1);
        const v2n:Number = dot(un, v2);
        const v2t:Number = dot(ut, v2);
        
        // step 4: - not needed -- see substitution in step 6
        //var v1tP:Number = v1t;
        //var v2tP:Number = v2t;
        
        // step 5:
        const v1nP:Number = ((v1n * (m1 - m2)) + (2 * m2 * v2n)) / (m1 + m2);
        const v2nP:Number = ((v2n * (m2 - m1)) + (2 * m1 * v1n)) / (m1 + m2);
        
        // step 6:
        const v1nP_:Array = mul(v1nP, un);
        const v1tP_:Array = mul(v1t, ut);
        const v2nP_:Array = mul(v2nP, un);
        const v2tP_:Array = mul(v2t, ut);
        
        // step 7:
        const v1P_:Array = add(v1nP_, v1tP_);
        const v2P_:Array = add(v2nP_, v2tP_);
        
        return new Array(v1P_, v2P_);
    }

    private function add(a:Array, b:Array) :Array
    {
        return new Array(a[0]+b[0], a[1]+b[1]);
    }

    /** return the scalar dot product of two vectors */
    private function dot(a:Array, b:Array) :Number
    {
        return (a[0]*b[0]) + (a[1]*b[1]);
    }

    /** multiply a vector by a scalar yielding a vector result */
    private function mul(scalar:Number, a:Array) :Array
    {
        return new Array(a[0]*scalar, a[1]*scalar);
    }

    /** divide a vector by a scalar yielding a vector result */
    private function div(a:Array, scalar:Number) :Array
    {
        return new Array(a[0]/scalar, a[1]/scalar);
    }

    /** return the absolute value of a vector */
    private function abs(a:Array) :Number
    {
        return Math.sqrt((a[0]*a[0]) + (a[1]*a[1]));
    }
}
}