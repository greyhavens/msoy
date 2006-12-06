//
// $Id$

package com.threerings.msoy.person.server;

import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.util.ResultListener;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.data.NeighborFriend;
import com.threerings.msoy.web.data.NeighborGroup;
import com.threerings.msoy.web.data.Neighborhood;

/**
 * Resolves a person's friend information.
 */
public class HoodResolver extends BlurbResolver
{
    @Override // from BlurbResolver
    protected void resolve ()
    {
        MsoyServer.memberMan.getNeighborhood(_memberId, new ResultListener<Neighborhood>() {
            public void requestCompleted (Neighborhood hood) {
                try {
                    resolutionCompleted(URLEncoder.encode(toJSON(hood).toString(), "UTF-8"));
                } catch (Exception e) {
                    resolutionFailed(e);
                }
            }
            public void requestFailed (Exception cause) {
                resolutionFailed(cause);
            }
        });
    }
    
    // handcrafted JSON serialization, to minimize the overhead
    protected JSONObject toJSON (Neighborhood hood)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("name", hood.member.toString());
        obj.put("id", hood.member.getMemberId());
        JSONArray jArr = new JSONArray();
        for (NeighborFriend friend : hood.neighborFriends) {
            jArr.put(toJSON(friend));
        }
        obj.put("friends", jArr);
        jArr = new JSONArray();
        for (NeighborGroup group : hood.neighborGroups) {
            jArr.put(toJSON(group));
        }
        obj.put("groups", jArr);
        return obj;
    }
    
    protected JSONObject toJSON (NeighborFriend friend)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("name", friend.member.toString());
        obj.put("id", friend.member.getMemberId());
        obj.put("isOnline", friend.isOnline);
        return obj;
    }

    protected JSONObject toJSON (NeighborGroup group)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("name", group.groupName);
        obj.put("id", group.groupId);
        obj.put("members", group.members);
        return obj;
    }
}
