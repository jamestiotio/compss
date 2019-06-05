/*
 *  Copyright 2002-2019 Barcelona Supercomputing Center (www.bsc.es)
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
package es.bsc.compss.nio.commands;

import es.bsc.comm.Connection;
import es.bsc.comm.nio.NIONode;

import es.bsc.compss.nio.NIOAgent;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class CommandCheckWorker extends Command implements Externalizable {

    private String uuid;
    private String nodeName;


    /**
     * Creates a new CommandCheckWorker for externalization.
     */
    public CommandCheckWorker() {
        super();
    }

    /**
     * Creates a new CommandCheckWorker instance.
     * 
     * @param agent Associated NIOAgent.
     * @param uuid Associated application UUID.
     * @param nodeName Worker node name.
     */
    public CommandCheckWorker(NIOAgent agent, String uuid, String nodeName) {
        super(agent);
        this.uuid = uuid;
        this.nodeName = nodeName;
    }

    @Override
    public CommandType getType() {
        return CommandType.CHECK_WORKER;
    }

    @Override
    public void handle(Connection c) {
        if (this.agent.isMyUuid(this.uuid, this.nodeName)) {
            if (this.agent.getMaster() == null) {
                this.agent.setMaster((NIONode) c.getNode());
            }
            CommandCheckWorkerACK cmd = new CommandCheckWorkerACK(this.agent, this.uuid, this.nodeName);
            c.sendCommand(cmd);
        }

        c.finishConnection();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.uuid = (String) in.readUTF();
        this.nodeName = (String) in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.uuid);
        out.writeUTF(this.nodeName);
    }

    @Override
    public String toString() {
        return "CommandCheckWorker for deployment ID " + this.uuid + " on nodeName " + this.nodeName;
    }

}
