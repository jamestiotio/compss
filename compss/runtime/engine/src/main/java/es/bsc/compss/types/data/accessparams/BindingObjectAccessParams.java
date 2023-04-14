/*
 *  Copyright 2002-2022 Barcelona Supercomputing Center (www.bsc.es)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package es.bsc.compss.types.data.accessparams;

import es.bsc.compss.comm.Comm;
import es.bsc.compss.types.Application;
import es.bsc.compss.types.BindingObject;
import es.bsc.compss.types.annotations.parameter.Direction;
import es.bsc.compss.types.data.DataAccessId;
import es.bsc.compss.types.data.DataInfo;
import es.bsc.compss.types.data.DataInstanceId;
import es.bsc.compss.types.data.DataParams.BindingObjectData;
import es.bsc.compss.types.data.DataVersion;
import es.bsc.compss.types.data.LogicalData;
import es.bsc.compss.types.data.accessid.RAccessId;
import es.bsc.compss.types.data.location.BindingObjectLocation;
import es.bsc.compss.types.data.location.DataLocation;
import es.bsc.compss.types.data.operation.BindingObjectTransferable;
import es.bsc.compss.types.data.operation.OneOpWithSemListener;
import es.bsc.compss.util.ErrorManager;
import java.util.concurrent.Semaphore;


public class BindingObjectAccessParams extends ObjectAccessParams<BindingObject, BindingObjectData> {

    /**
     * Serializable objects Version UID are 1L in all Runtime.
     */
    private static final long serialVersionUID = 1L;


    /**
     * Creates a new BindingObjectAccessParams instance.
     *
     * @param app Id of the application accessing the BindingObject.
     * @param dir operation performed.
     * @param bo Associated BindingObject.
     * @param hashCode Hashcode of the associated BindingObject.
     * @return new BindingObjectAccessParams instance
     */
    public static final BindingObjectAccessParams constructBOAP(Application app, Direction dir, BindingObject bo,
        int hashCode) {
        return new BindingObjectAccessParams(app, dir, bo, hashCode);
    }

    private BindingObjectAccessParams(Application app, Direction dir, BindingObject bo, int hashCode) {
        super(new BindingObjectData(app, hashCode), dir, bo);
    }

    /**
     * Returns the associated BindingObject.
     * 
     * @return The associated BindingObject.
     */
    public BindingObject getBindingObject() {
        return (BindingObject) this.getValue();
    }

    @Override
    public void registeredAsFirstVersionForData(DataInfo dInfo) {
        DataVersion dv = dInfo.getCurrentDataVersion();
        if (mode != AccessMode.W) {
            DataInstanceId lastDID = dv.getDataInstanceId();
            String renaming = lastDID.getRenaming();
            Comm.registerBindingObject(renaming, getBindingObject());
        } else {
            dv.invalidate();
        }
    }

    @Override
    public BindingObject fetchObject(DataAccessId daId) {
        LOGGER.debug("[AccessProcessor] Obtaining " + this.getDataDescription());

        // Get target information
        RAccessId raId = (RAccessId) daId;
        DataInstanceId diId = raId.getReadDataInstance();
        String targetName = diId.getRenaming();

        if (DEBUG) {
            LOGGER.debug("[DataInfoProvider] Requesting getting object " + targetName);
        }
        LogicalData srcData = diId.getData();
        if (DEBUG) {
            LOGGER.debug("[DataInfoProvider] Logical data for binding object is:" + srcData);
        }
        if (srcData == null) {
            ErrorManager.error("Unregistered data " + targetName);
            return null;
        }
        if (DEBUG) {
            LOGGER.debug("Requesting tranfers binding object " + targetName + " to " + Comm.getAppHost().getName());
        }

        BindingObject srcBO = BindingObject.generate(srcData.getURIs().get(0).getPath());
        BindingObject tgtBO = new BindingObject(targetName, srcBO.getType(), srcBO.getElements());
        LogicalData tgtLd = srcData;
        DataLocation targetLocation = new BindingObjectLocation(Comm.getAppHost(), tgtBO);
        BindingObjectTransferable transfer = new BindingObjectTransferable();
        Semaphore sem = new Semaphore(0);
        Comm.getAppHost().getData(srcData, targetLocation, tgtLd, transfer, new OneOpWithSemListener(sem));
        if (DEBUG) {
            LOGGER.debug(" Setting tgtName " + transfer.getDataTarget() + " in " + Comm.getAppHost().getName());
        }
        sem.acquireUninterruptibly();

        String boStr = transfer.getDataTarget();
        BindingObject bo = BindingObject.generate(boStr);
        return bo;
    }

}
