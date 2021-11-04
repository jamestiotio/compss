/*
 *  Copyright 2002-2021 Barcelona Supercomputing Center (www.bsc.es)
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
package es.bsc.compss.types;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class MPIProgram implements Externalizable {

    public static final int NUM_OF_PARAMS = 3;
    private String binary;
    private String params;
    private int processes;

    /**
     * Default Constructor.
     */
    public MPIProgram() {
        this.binary = "";
        this.params = "";
        this.processes = -1;
    }

    public MPIProgram(String binary, String params, int processes) {
        this.binary = binary;
        this.params = params;
        this.processes = processes;
    }

    public String getBinary() {
        return binary;
    }

    public String getParams() {
        return params;
    }

    public int getProcesses() {
        return processes;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.binary = (String) in.readObject();
        this.params = (String) in.readObject();
        this.processes = in.readInt();

    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.binary);
        out.writeObject(this.params);
        out.writeInt(this.processes);
    }

    /**
     * Check if it is an empty MPI program definition.
     * 
     * @return True only if binary is not provided
     */
    public boolean isEmpty() {
        return this.binary==null || this.binary.isEmpty();
    }

    @Override
    public String toString() {
        return "MPIProgram{" +
                "binary='" + binary + '\'' +
                ", params='" + params + '\'' +
                ", processes=" + processes +
                '}';
    }
}
