package {

import com.threerings.io.*;

import org.cove.flade.*;
import org.cove.flade.util.*;
import org.cove.flade.surfaces.*;
import org.cove.flade.primitives.*;
import org.cove.flade.constraints.*;

import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;

// streams the entire state of the dynamics engine
// TODO: split into static and transient data
public class WorldStateStreamer implements Streamable
{
    protected var engine :DynamicsEngine;

    public function WorldStateStreamer (e :DynamicsEngine = null)
    {
        if (e == null) {
            // for unserialization
            e = new DynamicsEngine();
        }
        engine = e;
    }

    public function writeObject (out :ObjectOutputStream) :void
    {
        var i :int;

        writeVector(out, engine.gravity);
        out.writeFloat(engine.coeffRest);
        out.writeFloat(engine.coeffFric);
        out.writeFloat(engine.coeffDamp);
        out.writeInt(engine.primitives.length);
        for (i = 0; i < engine.primitives.length; i ++) {
            writePrimitive(out, engine.primitives[i]);
        }
        out.writeInt(engine.surfaces.length);
        for (i = 0; i < engine.surfaces.length; i ++) {
            writeSurface(out, engine.surfaces[i]);
        }
        out.writeInt(engine.constraints.length);
        for (i = 0; i < engine.constraints.length; i ++) {
            writeConstraint(out, engine.constraints[i]);
        }
    }

    public function readObject (ins :ObjectInputStream) :void
    {
        var i :int;

        engine.gravity = readVector(ins);
        engine.coeffRest = ins.readFloat();
        engine.coeffFric = ins.readFloat();
        engine.coeffDamp = ins.readFloat();

        var pNum:int = ins.readInt();
        engine.primitives = new Array();
        for (i = 0; i < pNum; i ++) {
            engine.primitives[i] = readPrimitive(ins);
        }
        var sNum:int = ins.readInt();
        engine.surfaces = new Array();
        for (i = 0; i < sNum; i ++) {
            engine.surfaces[i] = readSurface(ins);
        }
        var cNum:int = ins.readInt();
        engine.constraints = new Array();
        for (i = 0; i < cNum; i ++) {
            engine.constraints[i] = readConstraint(ins);
        }
    }

    protected function writeVector (out :ObjectOutputStream, v :Vector) :void
    {
        out.writeFloat(v.x);
        out.writeFloat(v.y);
    }

    protected function readVector (ins :ObjectInputStream) :Vector
    {
        var v :Vector = new Vector();
        v.x = ins.readFloat();
        v.y = ins.readFloat();
        return v;
    }

    protected function writePrimitive (
        out :ObjectOutputStream, p :Particle) :void
    {
        if (p is CircleParticle) {
            if (p is Wheel) {
                out.writeByte(P_WHEEL);
                writeVector(out, (p as Wheel).rp.curr);
                writeVector(out, (p as Wheel).rp.prev);
                out.writeFloat((p as Wheel).rp.speed);
                out.writeFloat((p as Wheel).rp.vs);
                out.writeFloat((p as Wheel).rp.wr);
                out.writeFloat((p as Wheel).rp.maxTorque);
                out.writeFloat((p as Wheel).coeffSlip);
            } else {
                out.writeByte(P_CIRCLE_PARTICLE);
            }
            out.writeFloat((p as CircleParticle).radius); // static
        } else if (p is RectangleParticle) {
            out.writeByte(P_RECTANGLE_PARTICLE);
        } else {
            throw new Error("Unknown primitive class :" + p);
        }
        out.writeFloat(p.width);
        out.writeFloat(p.height);
        writeVector(out, p.curr); // transient
        writeVector(out, p.prev); // transient
        writeVector(out, p.mtd); // TODO: I think this is not real state
        writeVector(out, p.extents); // static
        writeVector(out, p.init); // static
        out.writeBoolean(p.isVisible);
    }

    protected function readPrimitive(ins :ObjectInputStream) :Particle
    {
        var p :Particle;
        var pType :int = ins.readByte();
        switch(pType) {
        case P_WHEEL:
            p = new Wheel();
            (p as Wheel).rp.curr = readVector(ins);
            (p as Wheel).rp.prev = readVector(ins);
            (p as Wheel).rp.speed = ins.readFloat();
            (p as Wheel).rp.vs = ins.readFloat();
            (p as Wheel).rp.wr = ins.readFloat();
            (p as Wheel).rp.maxTorque = readFloat();
            (p as Wheel).rp.coeffSlip = readFloat();
            // fall through
        case P_CIRCLE_PARTICLE:
            if (p == null) {
                p = new CircleParticle();
            }
            (p as CircleParticle).radius = ins.readFloat();
            break;
        case P_RECTANGLE_PARTICLE:
            p = new RectangleParticle();
            break;
        default:
            throw new Error("Unknown primitive ID: " + pType);
        }
        p.width = ins.readFloat();
        p.height = ins.readFloat();
        p.curr = readVector(ins);
        p.prev = readVector(ins);
        p.mtd = readVector(ins);
        p.extents = readVector(ins);
        p.init = readVector(ins);
        p.isVisible = ins.readBoolean();
        return p;
    }

    protected function writeSurface (
        out :ObjectOutputStream, s: Surface) :void
    {
    }
    protected function readSurface(ins :ObjectInputStream) :Particle
    {
        return null;
    }

    protected function writeConstraint (
        out :ObjectOutputStream, c: Constraint) :void
    {
    }
    protected function readConstraint(ins :ObjectInputStream) :Particle
    {
        return null;
    }

}
}
