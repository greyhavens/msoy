//
// $Id$

package com.threerings.msoy.swiftly.server;

import java.io.IOException;

import com.samskivert.depot.DatabaseException;
import com.threerings.presents.client.InvocationService.ResultListener;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.GenericUploadFile;
import com.threerings.msoy.web.server.UploadFile;
import com.threerings.msoy.web.server.UploadUtil;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.item.server.persist.FurnitureRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.PetRecord;

import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.server.build.BuildArtifact;
import com.threerings.msoy.swiftly.server.persist.SwiftlyCollaboratorsRecord;
import com.threerings.msoy.swiftly.server.persist.SwiftlyRepository;

/** Handles a request to build our project. */
public class BuildAndExportTask extends AbstractBuildTask
{
    public BuildAndExportTask (ProjectRoomManager manager, SwiftlyRepository swiftlyRepo,
                               ItemLogic itemLogic, MemberName member, ResultListener listener)
    {
        super(manager, member, listener);

        _swiftlyRepo = swiftlyRepo;
        _itemLogic = itemLogic;

        // snapshot the project type and name while we are on the dobject thread
        _projectName = manager.getRoomObj().project.projectName;
        _itemType = (byte)manager.getRoomObj().project.projectType;

        // lookup the cached result item id while on the dobject thread.
        _resultId = _manager.getResultItems().get(member);
    }

    @Override // from CommonBuildTask
    public void publishResult (final BuildResult result)
    {
        // inform the item system of the updated item if the build succeeded
        if (result.buildSuccessful()) {
            _itemLogic.itemUpdated(null, _record);
        }

        _omgr.postRunnable(new Runnable() {
            public void run() {
                // update the build result id cache
                _manager.getResultItems().put(_member, _resultId);
                _listener.requestProcessed(result);
            }
        });
    }

    @Override // from CommonBuildTask
    public void processArtifact (final BuildArtifact artifact)
        throws IOException
    {
        // First, publish the results into the media store
        UploadFile uploadFile = new GenericUploadFile(artifact.getOutputFile());
        UploadUtil.publishUploadFile(uploadFile);

        // load the correct item repository
        ItemRepository<ItemRecord> repo = null;
        try {
            repo = _itemLogic.getRepository(_itemType);
        } catch (ServiceException se) {
            throw new RuntimeException("Unable to find repository for Swiftly project item type." +
                " Aborting build process. [itemType=" + _itemType + "].");
        }

        // if the build result id was not in the room cache, load the record
        if (_resultId == null) {
            SwiftlyCollaboratorsRecord sRec = _swiftlyRepo.loadCollaborator(
                _projectId, _member.getMemberId());
            if (sRec == null) {
                throw new DatabaseException("No collaborator record found when expected. " +
                    "[projectId=" + _projectId + ", memberId=" +
                    _member.getMemberId() + "].");
            }
            _resultId = sRec.buildResultItemId;
        }

        // if the user already has an item, look it up
        if (_resultId > 0) {
            _record = repo.loadItem(_resultId);
        }

        // if the item was null [meaning they never had one or it was deleted] or
        // the user is no longer the owner, then create a new item.
        if (_record == null || _record.ownerId != _member.getMemberId()) {
            Item item = null;
            // can't use switch since Item.* are not constants
            if (_itemType == Item.AVATAR) {
                Avatar avatar = new Avatar();
                avatar.avatarMedia = new MediaDesc(MediaDesc.stringToHash(uploadFile.getHash()),
                                                   uploadFile.getMimeType());
                item = avatar;

            } else if (_itemType == Item.GAME) {
                Game game = new Game();
                game.gameMedia = new MediaDesc(MediaDesc.stringToHash(uploadFile.getHash()),
                                               uploadFile.getMimeType());
                // game.config cannot be null so just set it to blank and the user can
                // tweak the config settings through the item editor
                game.config = "";
                item = game;

            } else if (_itemType == Item.FURNITURE) {
                Furniture furniture = new Furniture();
                furniture.setFurniMedia(new MediaDesc(MediaDesc.stringToHash(uploadFile.getHash()),
                                                      uploadFile.getMimeType()));
                item = furniture;

            } else if (_itemType == Item.PET) {
                Pet pet = new Pet();
                pet.setFurniMedia(new MediaDesc(MediaDesc.stringToHash(uploadFile.getHash()),
                                                uploadFile.getMimeType()));
                item = pet;

            } else {
                throw new RuntimeException(
                    "Unsupported itemType encountered during Swiftly item exporting.");
            }

            // setup the rest of the generic item fields
            item.name = _projectName + " Swiftly Result"; // TODO: i18n this string
            item.description = ""; // description cannot be NULL

            // create the new item
            try {
                _record = _itemLogic.createItem(_member.getMemberId(), item);
            } catch (ServiceException se) {
                throw new RuntimeException("Item creation failed.", se);
            }

            // update the collaborator record with the new itemId
            _resultId = _record.itemId;
            _swiftlyRepo.updateBuildResultItem(_projectId, _member.getMemberId(), _record.itemId);

        // otherwise, update the existing item
        } else {
            // can't use switch since Item.* are not constants
            ItemRecord updateRecord = null;
            if (_itemType == Item.AVATAR) {
                AvatarRecord avatarRecord = (AvatarRecord) _record;
                avatarRecord.avatarMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                updateRecord = avatarRecord;

            } else if (_itemType == Item.GAME) {
                GameRecord gameRecord = (GameRecord) _record;
                gameRecord.gameMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                updateRecord = gameRecord;

            } else if (_itemType == Item.FURNITURE) {
                FurnitureRecord furnitureRecord = (FurnitureRecord) _record;
                furnitureRecord.furniMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                updateRecord = furnitureRecord;

            } else if (_itemType == Item.PET) {
                PetRecord petRecord = (PetRecord) _record;
                petRecord.furniMediaHash = MediaDesc.stringToHash(uploadFile.getHash());
                updateRecord = petRecord;

            } else {
                throw new RuntimeException(
                    "Unsupported itemType encountered during Swiftly item exporting.");
            }

            repo.updateOriginalItem(updateRecord);
        }
    }

    protected Integer _resultId;
    protected ItemRecord _record;

    protected final byte _itemType;
    protected final String _projectName;
    protected final ItemLogic _itemLogic;
    protected final SwiftlyRepository _swiftlyRepo;
}
