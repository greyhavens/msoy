package com.threerings.msoy.comment.data.all
{
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.util.ByteEnum;

public class CommentType extends SimpleStreamableObject
{
    public function CommentType ()
    {
    }

    public function toByte () :int
    {
        return _type;
    }

    public function toItemType () :MsoyItemType
    {
        return MsoyItemType(ByteEnum.fromByte(MsoyItemType, _type));
    }


    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _type = ins.readByte();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeByte(_type);
    }

    protected var _type :int;
}
}
