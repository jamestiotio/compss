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
package es.bsc.compss.scheduler.fullGraphScheduler;

import es.bsc.compss.components.impl.ResourceScheduler;
import es.bsc.compss.scheduler.exceptions.BlockedActionException;
import es.bsc.compss.scheduler.exceptions.InvalidSchedulingException;
import es.bsc.compss.scheduler.exceptions.UnassignedActionException;
import es.bsc.compss.scheduler.fullGraphScheduler.FullGraphResourceScheduler;
import es.bsc.compss.scheduler.fullGraphScheduler.FullGraphScheduler;
import es.bsc.compss.scheduler.fullGraphScheduler.FullGraphSchedulingInformation;
import es.bsc.compss.scheduler.fullGraphScheduler.ScheduleOptimizer;
import es.bsc.compss.scheduler.fullGraphScheduler.utils.Verifiers;
import es.bsc.compss.scheduler.types.AllocatableAction;
import es.bsc.compss.scheduler.types.OptimizationWorker;
import es.bsc.compss.scheduler.types.PriorityActionSet;
import es.bsc.compss.scheduler.types.fake.FakeActionOrchestrator;
import es.bsc.compss.scheduler.types.fake.FakeAllocatableAction;
import es.bsc.compss.scheduler.types.fake.FakeImplementation;
import es.bsc.compss.scheduler.types.fake.FakeProfile;
import es.bsc.compss.scheduler.types.fake.FakeResourceDescription;
import es.bsc.compss.scheduler.types.fake.FakeResourceScheduler;
import es.bsc.compss.scheduler.types.fake.FakeWorker;
import es.bsc.compss.types.implementations.Implementation;
import es.bsc.compss.util.CoreManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.junit.BeforeClass;
import org.junit.Test;


public class OptimizationTest {

    private static FullGraphScheduler ds;
    private static FakeActionOrchestrator fao;
    private static FullGraphResourceScheduler<FakeResourceDescription> drs1;
    private static FullGraphResourceScheduler<FakeResourceDescription> drs2;

    public OptimizationTest() {
        ds = new FullGraphScheduler();
        fao = new FakeActionOrchestrator(ds);
        ds.setOrchestrator(fao);
    }

    @BeforeClass
    public static void setUpClass() {
        CoreManager.clear();
        CoreManager.registerNewCoreElement("fakeSignature00");
        CoreManager.registerNewCoreElement("fakeSignature10");
        CoreManager.registerNewCoreElement("fakeSignature20");
        CoreManager.registerNewCoreElement("fakeSignature30");
        CoreManager.registerNewCoreElement("fakeSignature40");
        CoreManager.registerNewCoreElement("fakeSignature50");
        CoreManager.registerNewCoreElement("fakeSignature60");
        CoreManager.registerNewCoreElement("fakeSignature70");

        FakeImplementation impl00 = new FakeImplementation(0, 0, "fakeSignature00", new FakeResourceDescription(2));
        List<Implementation> impls0 = new LinkedList<>();
        impls0.add(impl00);
        CoreManager.registerNewImplementations(0, impls0);

        FakeImplementation impl10 = new FakeImplementation(1, 0, "fakeSignature10", new FakeResourceDescription(3));
        List<Implementation> impls1 = new LinkedList<>();
        impls1.add(impl10);
        CoreManager.registerNewImplementations(1, impls1);

        FakeImplementation impl20 = new FakeImplementation(2, 0, "fakeSignature20", new FakeResourceDescription(1));
        List<Implementation> impls2 = new LinkedList<>();
        impls2.add(impl20);
        CoreManager.registerNewImplementations(2, impls2);

        FakeImplementation impl30 = new FakeImplementation(3, 0, "fakeSignature30", new FakeResourceDescription(4));
        List<Implementation> impls3 = new LinkedList<>();
        impls3.add(impl30);
        CoreManager.registerNewImplementations(3, impls3);

        FakeImplementation impl40 = new FakeImplementation(4, 0, "fakeSignature40", new FakeResourceDescription(2));
        List<Implementation> impls4 = new LinkedList<>();
        impls4.add(impl40);
        CoreManager.registerNewImplementations(4, impls4);

        FakeImplementation impl50 = new FakeImplementation(5, 0, "fakeSignature50", new FakeResourceDescription(1));
        List<Implementation> impls5 = new LinkedList<>();
        impls5.add(impl50);
        CoreManager.registerNewImplementations(5, impls5);

        FakeImplementation impl60 = new FakeImplementation(6, 0, "fakeSignature60", new FakeResourceDescription(3));
        List<Implementation> impls6 = new LinkedList<>();
        impls6.add(impl60);
        CoreManager.registerNewImplementations(6, impls6);

        int maxSlots = 4;
        FakeResourceDescription frd = new FakeResourceDescription(maxSlots);
        FakeWorker fw = new FakeWorker("worker1", frd, maxSlots);
        drs1 = new FullGraphResourceScheduler<>(fw, null, null, fao);

        FakeResourceDescription frd2 = new FakeResourceDescription(maxSlots);
        FakeWorker fw2 = new FakeWorker("worker2", frd2, maxSlots);
        drs2 = new FullGraphResourceScheduler<>(fw2, null, null, fao);

        drs1.profiledExecution(impl00, new FakeProfile(50));
        drs1.profiledExecution(impl10, new FakeProfile(50));
        drs1.profiledExecution(impl20, new FakeProfile(30));
        drs1.profiledExecution(impl30, new FakeProfile(50));
        drs1.profiledExecution(impl40, new FakeProfile(20));
        drs1.profiledExecution(impl50, new FakeProfile(10));
        drs1.profiledExecution(impl60, new FakeProfile(30));

        drs2.profiledExecution(impl00, new FakeProfile(50));
        drs2.profiledExecution(impl10, new FakeProfile(50));
        drs2.profiledExecution(impl20, new FakeProfile(30));
        // Faster than drs
        drs2.profiledExecution(impl30, new FakeProfile(30));
        drs2.profiledExecution(impl40, new FakeProfile(15));
        drs2.profiledExecution(impl50, new FakeProfile(10));
        drs2.profiledExecution(impl60, new FakeProfile(15));
    }

    // @Test
    @SuppressWarnings("unchecked")
    public void testDonorsAndReceivers() {
        ScheduleOptimizer so = new ScheduleOptimizer(ds);

        long[] expectedEndTimes = new long[]{35000, 20000, 15000, 50000, 40000, 1000};
        OptimizationWorker<?>[] optimizedWorkers = new OptimizationWorker[expectedEndTimes.length];
        for (int idx = 0; idx < expectedEndTimes.length; idx++) {
            int maxSlots = 1;
            FakeResourceDescription frd = new FakeResourceDescription(maxSlots);
            FakeWorker fw = new FakeWorker("worker" + idx, frd, maxSlots);
            FakeResourceScheduler frs = new FakeResourceScheduler(fw, null, null, fao, expectedEndTimes[idx]);
            optimizedWorkers[idx] = new OptimizationWorker<>(frs);
        }

        LinkedList<OptimizationWorker<?>> receivers = new LinkedList<>();
        LinkedList<OptimizationWorker<FakeResourceDescription>> receiversF = new LinkedList<>();
        OptimizationWorker<FakeResourceDescription> donor = (OptimizationWorker<FakeResourceDescription>) so
                .determineDonorAndReceivers(optimizedWorkers, receivers);

        LinkedList<OptimizationWorker<FakeResourceDescription>> donors = new LinkedList<>();
        donors.offer(donor);
        LinkedList<String> donorsNames = new LinkedList<>();
        donorsNames.add("worker3");

        LinkedList<String> receiversNames = new LinkedList<>();
        receiversNames.add("worker5");
        receiversNames.add("worker2");
        receiversNames.add("worker1");
        receiversNames.add("worker0");
        receiversNames.add("worker4");

        Verifiers.verifyWorkersPriority(donors, donorsNames);
        Verifiers.verifyWorkersPriority(receiversF, receiversNames);
    }

    // @Test
    public void globalOptimization() {
        ScheduleOptimizer so = new ScheduleOptimizer(ds);

        long updateId = System.currentTimeMillis();

        Collection<ResourceScheduler<?>> workers = new ArrayList<>();
        drs1.clear();
        drs2.clear();
        workers.add(drs1);

        FakeAllocatableAction action1 = new FakeAllocatableAction(fao, 1, 0, CoreManager.getCoreImplementations(4));
        action1.selectExecution(drs1, (FakeImplementation) action1.getImplementations()[0]);
        drs1.scheduleAction(action1);
        try {
            action1.tryToLaunch();
        } catch (Exception e) {
        }

        FakeAllocatableAction action2 = new FakeAllocatableAction(fao, 2, 0, CoreManager.getCoreImplementations(4));
        action2.selectExecution(drs1, (FakeImplementation) action2.getImplementations()[0]);
        drs1.scheduleAction(action2);
        try {
            action2.tryToLaunch();
        } catch (Exception e) {
        }

        FakeAllocatableAction action3 = new FakeAllocatableAction(fao, 3, 1, CoreManager.getCoreImplementations(4));
        action3.selectExecution(drs1, (FakeImplementation) action3.getImplementations()[0]);
        drs1.scheduleAction(action3);

        FakeAllocatableAction action4 = new FakeAllocatableAction(fao, 4, 0, CoreManager.getCoreImplementations(5));
        action4.selectExecution(drs1, (FakeImplementation) action4.getImplementations()[0]);
        drs1.scheduleAction(action4);

        FakeAllocatableAction action5 = new FakeAllocatableAction(fao, 5, 1, CoreManager.getCoreImplementations(4));
        action5.selectExecution(drs1, (FakeImplementation) action5.getImplementations()[0]);
        drs1.scheduleAction(action5);

        FakeAllocatableAction action6 = new FakeAllocatableAction(fao, 6, 1, CoreManager.getCoreImplementations(6));
        action6.selectExecution(drs1, (FakeImplementation) action6.getImplementations()[0]);
        drs1.scheduleAction(action6);

        workers.add(drs2);

        so.globalOptimization(updateId, workers);
    }

    // @Test
    public void testScan() {
        FakeAllocatableAction external10 = new FakeAllocatableAction(fao, 13, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external10.getSchedulingInfo()).setExpectedEnd(10);
        ((FullGraphSchedulingInformation) external10.getSchedulingInfo()).scheduled();
        external10.selectExecution(drs2, (FakeImplementation) external10.getImplementations()[0]);

        FakeAllocatableAction external20 = new FakeAllocatableAction(fao, 14, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external20.getSchedulingInfo()).setExpectedEnd(20);
        ((FullGraphSchedulingInformation) external20.getSchedulingInfo()).scheduled();
        external20.selectExecution(drs2, (FakeImplementation) external20.getImplementations()[0]);

        FakeAllocatableAction external90 = new FakeAllocatableAction(fao, 15, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external90.getSchedulingInfo()).setExpectedEnd(90);
        ((FullGraphSchedulingInformation) external90.getSchedulingInfo()).scheduled();
        external90.selectExecution(drs2, (FakeImplementation) external90.getImplementations()[0]);

        drs1.clear();
        drs2.clear();

        FakeAllocatableAction action1 = new FakeAllocatableAction(fao, 1, 0, CoreManager.getCoreImplementations(4));
        action1.selectExecution(drs1, (FakeImplementation) action1.getImplementations()[0]);
        drs1.scheduleAction(action1);
        try {
            action1.tryToLaunch();
        } catch (Exception e) {
        }

        FakeAllocatableAction action2 = new FakeAllocatableAction(fao, 2, 0, CoreManager.getCoreImplementations(4));
        action2.selectExecution(drs1, (FakeImplementation) action2.getImplementations()[0]);
        drs1.scheduleAction(action2);
        try {
            action2.tryToLaunch();
        } catch (Exception e) {
        }

        FakeAllocatableAction action3 = new FakeAllocatableAction(fao, 3, 1, CoreManager.getCoreImplementations(4));
        action3.addDataPredecessor(external90);
        action3.selectExecution(drs1, (FakeImplementation) action3.getImplementations()[0]);
        drs1.scheduleAction(action3);

        FakeAllocatableAction action4 = new FakeAllocatableAction(fao, 4, 0, CoreManager.getCoreImplementations(5));
        action4.selectExecution(drs1, (FakeImplementation) action4.getImplementations()[0]);
        drs1.scheduleAction(action4);

        FakeAllocatableAction action5 = new FakeAllocatableAction(fao, 5, 1, CoreManager.getCoreImplementations(4));
        action5.selectExecution(drs1, (FakeImplementation) action5.getImplementations()[0]);
        drs1.scheduleAction(action5);

        FakeAllocatableAction action6 = new FakeAllocatableAction(fao, 6, 1, CoreManager.getCoreImplementations(6));
        action6.selectExecution(drs1, (FakeImplementation) action6.getImplementations()[0]);
        drs1.scheduleAction(action6);

        FakeAllocatableAction action7 = new FakeAllocatableAction(fao, 7, 0, CoreManager.getCoreImplementations(5));
        action7.addDataPredecessor(external10);
        action7.selectExecution(drs1, (FakeImplementation) action7.getImplementations()[0]);
        drs1.scheduleAction(action7);

        FakeAllocatableAction action8 = new FakeAllocatableAction(fao, 8, 0, CoreManager.getCoreImplementations(5));
        action8.addDataPredecessor(external20);
        action8.selectExecution(drs1, (FakeImplementation) action8.getImplementations()[0]);
        drs1.scheduleAction(action8);

        FakeAllocatableAction action9 = new FakeAllocatableAction(fao, 9, 0, CoreManager.getCoreImplementations(4));
        action9.addDataPredecessor(external90);
        action9.selectExecution(drs1, (FakeImplementation) action9.getImplementations()[0]);
        drs1.scheduleAction(action9);

        FakeAllocatableAction action10 = new FakeAllocatableAction(fao, 10, 0, CoreManager.getCoreImplementations(4));
        action10.addDataPredecessor(action5);
        action10.selectExecution(drs1, (FakeImplementation) action10.getImplementations()[0]);
        drs1.scheduleAction(action10);

        FakeAllocatableAction action11 = new FakeAllocatableAction(fao, 11, 0, CoreManager.getCoreImplementations(4));
        action11.addDataPredecessor(action6);
        action11.selectExecution(drs1, (FakeImplementation) action11.getImplementations()[0]);
        drs1.scheduleAction(action11);

        FakeAllocatableAction action12 = new FakeAllocatableAction(fao, 12, 0, CoreManager.getCoreImplementations(4));
        action12.addDataPredecessor(action5);
        action12.addDataPredecessor(action6);
        action12.selectExecution(drs1, (FakeImplementation) action12.getImplementations()[0]);
        drs1.scheduleAction(action12);

        // Actions not depending on other actions scheduled on the same resource
        // Sorted by data dependencies release
        PriorityQueue<AllocatableAction> readyActions = new PriorityQueue<>(1,
                FullGraphResourceScheduler.getReadyComparator());

        // Actions that can be selected to be scheduled on the node
        // Sorted by data dependencies release
        PriorityActionSet selectableActions = new PriorityActionSet(FullGraphResourceScheduler.getScanComparator());

        drs1.scanActions(readyActions, selectableActions);

        HashMap<AllocatableAction, Long> expectedReady = new HashMap<>();
        expectedReady.put(action7, 10l);
        expectedReady.put(action8, 20l);
        expectedReady.put(action9, 90l);
        expectedReady.put(action3, 90l);
        Verifiers.verifyReadyActions(new PriorityQueue<>(readyActions), expectedReady);

        AllocatableAction[] expectedSelectable = new AllocatableAction[]{action5, action6, action4};
        Verifiers.verifyPriorityActions(new PriorityActionSet(selectableActions), expectedSelectable);
    }

    public void printAction(AllocatableAction action) {
        System.out.println(action + " Core Element " + action.getCoreId() + " Implementation "
                + action.getAssignedImplementation().getImplementationId() + " (" + action.getAssignedImplementation()
                + ")");
        FullGraphSchedulingInformation dsi = (FullGraphSchedulingInformation) action.getSchedulingInfo();
        System.out.println("\t Optimization:" + dsi.isOnOptimization());
        System.out.println("\t StartTime:" + dsi.getExpectedStart());
        System.out.println("\t EndTime:" + dsi.getExpectedEnd());
        System.out.println("\t Locks:" + dsi.getLockCount());
        System.out.println("\t Predecessors:" + dsi.getPredecessors());
        System.out.println("\t Successors:" + dsi.getSuccessors());
        System.out.println("\t Optimization Successors:" + dsi.getOptimizingSuccessors());

    }

    // @Test
    public void testPendingActions() {
        LinkedList<AllocatableAction> pendingActions = new LinkedList<>();

        FakeAllocatableAction external10 = new FakeAllocatableAction(fao, 13, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external10.getSchedulingInfo()).setExpectedEnd(10);
        ((FullGraphSchedulingInformation) external10.getSchedulingInfo()).scheduled();
        external10.selectExecution(drs2, (FakeImplementation) external10.getImplementations()[0]);

        FakeAllocatableAction external20 = new FakeAllocatableAction(fao, 14, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external20.getSchedulingInfo()).setExpectedEnd(20);
        ((FullGraphSchedulingInformation) external20.getSchedulingInfo()).scheduled();
        external20.selectExecution(drs2, (FakeImplementation) external20.getImplementations()[0]);

        FakeAllocatableAction external90 = new FakeAllocatableAction(fao, 15, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external90.getSchedulingInfo()).setExpectedEnd(90);
        ((FullGraphSchedulingInformation) external90.getSchedulingInfo()).scheduled();
        external90.selectExecution(drs2, (FakeImplementation) external90.getImplementations()[0]);

        drs1.clear();
        drs2.clear();

        FakeAllocatableAction action1 = new FakeAllocatableAction(fao, 1, 0, CoreManager.getCoreImplementations(4));
        action1.selectExecution(drs1, (FakeImplementation) action1.getImplementations()[0]);
        drs1.scheduleAction(action1);
        try {
            action1.tryToLaunch();
        } catch (Exception e) {
        }

        FakeAllocatableAction action2 = new FakeAllocatableAction(fao, 2, 0, CoreManager.getCoreImplementations(4));
        action2.selectExecution(drs1, (FakeImplementation) action2.getImplementations()[0]);
        drs1.scheduleAction(action2);
        try {
            action2.tryToLaunch();
        } catch (Exception e) {
        }

        FakeAllocatableAction action3 = new FakeAllocatableAction(fao, 3, 1, CoreManager.getCoreImplementations(4));
        action3.addDataPredecessor(external90);
        action3.selectExecution(drs1, (FakeImplementation) action3.getImplementations()[0]);
        drs1.scheduleAction(action3);

        FakeAllocatableAction action4 = new FakeAllocatableAction(fao, 4, 0, CoreManager.getCoreImplementations(5));
        action4.selectExecution(drs1, (FakeImplementation) action4.getImplementations()[0]);
        drs1.scheduleAction(action4);

        FakeAllocatableAction action5 = new FakeAllocatableAction(fao, 5, 1, CoreManager.getCoreImplementations(4));
        action5.selectExecution(drs1, (FakeImplementation) action5.getImplementations()[0]);
        drs1.scheduleAction(action5);

        FakeAllocatableAction action6 = new FakeAllocatableAction(fao, 6, 1, CoreManager.getCoreImplementations(6));
        action6.selectExecution(drs1, (FakeImplementation) action6.getImplementations()[0]);
        drs1.scheduleAction(action6);

        FakeAllocatableAction action7 = new FakeAllocatableAction(fao, 7, 0, CoreManager.getCoreImplementations(5));
        action7.addDataPredecessor(external10);
        action7.selectExecution(drs1, (FakeImplementation) action7.getImplementations()[0]);
        pendingActions.add(action7);

        FakeAllocatableAction action8 = new FakeAllocatableAction(fao, 8, 0, CoreManager.getCoreImplementations(5));
        action8.addDataPredecessor(external20);
        action8.selectExecution(drs1, (FakeImplementation) action8.getImplementations()[0]);
        pendingActions.add(action8);

        FakeAllocatableAction action9 = new FakeAllocatableAction(fao, 9, 0, CoreManager.getCoreImplementations(4));
        action9.addDataPredecessor(external90);
        action9.selectExecution(drs1, (FakeImplementation) action9.getImplementations()[0]);
        pendingActions.add(action9);

        FakeAllocatableAction action10 = new FakeAllocatableAction(fao, 10, 0, CoreManager.getCoreImplementations(4));
        action10.addDataPredecessor(action5);
        action10.selectExecution(drs1, (FakeImplementation) action10.getImplementations()[0]);
        pendingActions.add(action10);

        FakeAllocatableAction action11 = new FakeAllocatableAction(fao, 11, 0, CoreManager.getCoreImplementations(4));
        action11.addDataPredecessor(action6);
        action11.selectExecution(drs1, (FakeImplementation) action11.getImplementations()[0]);
        pendingActions.add(action11);

        FakeAllocatableAction action12 = new FakeAllocatableAction(fao, 12, 0, CoreManager.getCoreImplementations(4));
        action12.addDataPredecessor(action5);
        action12.addDataPredecessor(action6);
        action12.selectExecution(drs1, (FakeImplementation) action12.getImplementations()[0]);
        pendingActions.add(action12);

        // Actions not depending on other actions scheduled on the same resource
        // Sorted by data dependencies release
        PriorityQueue<AllocatableAction> readyActions = new PriorityQueue<>(1,
                FullGraphResourceScheduler.getReadyComparator());

        // Actions that can be selected to be scheduled on the node
        // Sorted by data dependencies release
        PriorityActionSet selectableActions = new PriorityActionSet(FullGraphResourceScheduler.getScanComparator());

        drs1.scanActions(readyActions, selectableActions);
        drs1.classifyPendingSchedulings(pendingActions, readyActions, selectableActions,
                new LinkedList<AllocatableAction>());

        HashMap<AllocatableAction, Long> expectedReady = new HashMap<>();
        expectedReady.put(action7, 10l);
        expectedReady.put(action8, 20l);
        expectedReady.put(action9, 90l);
        expectedReady.put(action3, 90l);
        Verifiers.verifyReadyActions(new PriorityQueue<>(readyActions), expectedReady);

        AllocatableAction[] expectedSelectable = new AllocatableAction[]{action5, action6, action4};
        Verifiers.verifyPriorityActions(new PriorityActionSet(selectableActions), expectedSelectable);
    }

    @SuppressWarnings("static-access")
    @Test
    public void testLocalOptimization() {

        drs1.clear();
        drs2.clear();

        FakeAllocatableAction external10 = new FakeAllocatableAction(fao, 13, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external10.getSchedulingInfo()).setExpectedEnd(10);
        ((FullGraphSchedulingInformation) external10.getSchedulingInfo()).scheduled();
        external10.selectExecution(drs2, (FakeImplementation) external10.getImplementations()[0]);

        FakeAllocatableAction external20 = new FakeAllocatableAction(fao, 14, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external20.getSchedulingInfo()).setExpectedEnd(20);
        ((FullGraphSchedulingInformation) external20.getSchedulingInfo()).scheduled();
        external20.selectExecution(drs2, (FakeImplementation) external20.getImplementations()[0]);

        FakeAllocatableAction external90 = new FakeAllocatableAction(fao, 15, 0, CoreManager.getCoreImplementations(4));
        ((FullGraphSchedulingInformation) external90.getSchedulingInfo()).setExpectedEnd(90);
        ((FullGraphSchedulingInformation) external90.getSchedulingInfo()).scheduled();
        external90.selectExecution(drs2, (FakeImplementation) external90.getImplementations()[0]);

        FakeAllocatableAction action1 = new FakeAllocatableAction(fao, 1, 0, CoreManager.getCoreImplementations(4));
        action1.selectExecution(drs1, (FakeImplementation) action1.getImplementations()[0]);
        drs1.scheduleAction(action1);
        try {
            action1.tryToLaunch();
        } catch (Exception e) {
        }

        FakeAllocatableAction action2 = new FakeAllocatableAction(fao, 2, 0, CoreManager.getCoreImplementations(4));
        action2.selectExecution(drs1, (FakeImplementation) action2.getImplementations()[0]);
        drs1.scheduleAction(action2);
        try {
            action2.tryToLaunch();
        } catch (Exception e) {
        }

        FakeAllocatableAction action3 = new FakeAllocatableAction(fao, 3, 1, CoreManager.getCoreImplementations(4));
        action3.addDataPredecessor(external90);
        action3.selectExecution(drs1, (FakeImplementation) action3.getImplementations()[0]);
        drs1.scheduleAction(action3);

        FakeAllocatableAction action4 = new FakeAllocatableAction(fao, 4, 0, CoreManager.getCoreImplementations(5));
        action4.selectExecution(drs1, (FakeImplementation) action4.getImplementations()[0]);
        drs1.scheduleAction(action4);

        FakeAllocatableAction action5 = new FakeAllocatableAction(fao, 5, 1, CoreManager.getCoreImplementations(4));
        action5.selectExecution(drs1, (FakeImplementation) action5.getImplementations()[0]);
        drs1.scheduleAction(action5);

        FakeAllocatableAction action6 = new FakeAllocatableAction(fao, 6, 1, CoreManager.getCoreImplementations(6));
        action6.selectExecution(drs1, (FakeImplementation) action6.getImplementations()[0]);
        drs1.scheduleAction(action6);

        FakeAllocatableAction action7 = new FakeAllocatableAction(fao, 7, 0, CoreManager.getCoreImplementations(5));
        action7.addDataPredecessor(external10);
        action7.selectExecution(drs1, (FakeImplementation) action7.getImplementations()[0]);
        drs1.scheduleAction(action7);

        FakeAllocatableAction action8 = new FakeAllocatableAction(fao, 8, 0, CoreManager.getCoreImplementations(5));
        action8.addDataPredecessor(external20);
        action8.selectExecution(drs1, (FakeImplementation) action8.getImplementations()[0]);
        drs1.scheduleAction(action8);

        FakeAllocatableAction action9 = new FakeAllocatableAction(fao, 9, 0, CoreManager.getCoreImplementations(4));
        action9.addDataPredecessor(external90);
        action9.selectExecution(drs1, (FakeImplementation) action9.getImplementations()[0]);
        drs1.scheduleAction(action9);

        FakeAllocatableAction action10 = new FakeAllocatableAction(fao, 10, 0, CoreManager.getCoreImplementations(4));
        action10.addDataPredecessor(action5);
        action10.selectExecution(drs1, (FakeImplementation) action10.getImplementations()[0]);
        drs1.scheduleAction(action10);

        FakeAllocatableAction action11 = new FakeAllocatableAction(fao, 11, 0, CoreManager.getCoreImplementations(4));
        action11.addDataPredecessor(action6);
        action11.selectExecution(drs1, (FakeImplementation) action11.getImplementations()[0]);
        drs1.scheduleAction(action11);

        FakeAllocatableAction action12 = new FakeAllocatableAction(fao, 12, 0, CoreManager.getCoreImplementations(4));
        action12.addDataPredecessor(action5);
        action12.addDataPredecessor(action6);
        action12.selectExecution(drs1, (FakeImplementation) action12.getImplementations()[0]);
        drs1.scheduleAction(action12);

        // Simulate Scan results
        LinkedList<AllocatableAction> runningActions = new LinkedList<>();
        PriorityQueue<AllocatableAction> readyActions = new PriorityQueue<>(1, drs1.getReadyComparator());
        PriorityActionSet selectableActions = new PriorityActionSet(ScheduleOptimizer.getSelectionComparator());

        long updateId = System.currentTimeMillis();

        ((FullGraphSchedulingInformation) action1.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action1.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action1.getSchedulingInfo()).setExpectedStart(0);
        ((FullGraphSchedulingInformation) action1.getSchedulingInfo()).lock();
        runningActions.add(action1);

        ((FullGraphSchedulingInformation) action2.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action2.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action2.getSchedulingInfo()).setExpectedStart(0);
        ((FullGraphSchedulingInformation) action2.getSchedulingInfo()).lock();
        runningActions.add(action2);

        ((FullGraphSchedulingInformation) action3.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action3.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action3.getSchedulingInfo()).setExpectedStart(90);
        readyActions.offer(action3);

        ((FullGraphSchedulingInformation) action4.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action4.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action4.getSchedulingInfo()).setExpectedStart(0);
        selectableActions.offer(action4);

        ((FullGraphSchedulingInformation) action5.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action5.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action5.getSchedulingInfo()).setExpectedStart(0);
        ((FullGraphSchedulingInformation) action5.getSchedulingInfo()).optimizingSuccessor(action12);
        ((FullGraphSchedulingInformation) action5.getSchedulingInfo()).optimizingSuccessor(action10);
        selectableActions.offer(action5);

        ((FullGraphSchedulingInformation) action6.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action6.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action6.getSchedulingInfo()).setExpectedStart(0);
        ((FullGraphSchedulingInformation) action6.getSchedulingInfo()).optimizingSuccessor(action12);
        ((FullGraphSchedulingInformation) action6.getSchedulingInfo()).optimizingSuccessor(action11);
        selectableActions.offer(action6);

        ((FullGraphSchedulingInformation) action7.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action7.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action7.getSchedulingInfo()).setExpectedStart(10);
        readyActions.offer(action7);

        ((FullGraphSchedulingInformation) action8.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action8.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action8.getSchedulingInfo()).setExpectedStart(20);
        readyActions.offer(action8);

        ((FullGraphSchedulingInformation) action9.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action9.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action9.getSchedulingInfo()).setExpectedStart(90);
        readyActions.offer(action9);

        ((FullGraphSchedulingInformation) action10.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action10.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action10.getSchedulingInfo()).setExpectedStart(0);

        ((FullGraphSchedulingInformation) action11.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action11.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action11.getSchedulingInfo()).setExpectedStart(0);

        ((FullGraphSchedulingInformation) action12.getSchedulingInfo()).setOnOptimization(true);
        ((FullGraphSchedulingInformation) action12.getSchedulingInfo()).setToReschedule(true);
        ((FullGraphSchedulingInformation) action12.getSchedulingInfo()).setExpectedStart(0);

        PriorityQueue<AllocatableAction> donationActions = new PriorityQueue<>(1,
                ScheduleOptimizer.getDonationComparator());

        drs1.rescheduleTasks(updateId, readyActions, selectableActions, runningActions, donationActions);
    }

    // @Test
    @SuppressWarnings("unchecked")
    public void testNoDataDependencies()
            throws BlockedActionException, UnassignedActionException, InvalidSchedulingException, InterruptedException {

        // Build graph
        /*
         * 1 --> 3 --> 5 -->6 --> 8 -->9 ----->11 -->12 --> 13 2 --> 4 ┘ └->7 ┘ └->10 ---| └-----┘ | |
         * ------------------------------------------------------- 14┘ 15┘
         */
        drs1.clear();

        FakeAllocatableAction action1 = new FakeAllocatableAction(fao, 1, 0, CoreManager.getCoreImplementations(0));
        FakeAllocatableAction action2 = new FakeAllocatableAction(fao, 2, 0, CoreManager.getCoreImplementations(0));
        FakeAllocatableAction action3 = new FakeAllocatableAction(fao, 3, 0, CoreManager.getCoreImplementations(0));
        FakeAllocatableAction action4 = new FakeAllocatableAction(fao, 4, 0, CoreManager.getCoreImplementations(0));
        FakeAllocatableAction action5 = new FakeAllocatableAction(fao, 5, 0, CoreManager.getCoreImplementations(1));
        FakeAllocatableAction action6 = new FakeAllocatableAction(fao, 6, 0, CoreManager.getCoreImplementations(0));
        FakeAllocatableAction action7 = new FakeAllocatableAction(fao, 7, 0, CoreManager.getCoreImplementations(2));
        FakeAllocatableAction action8 = new FakeAllocatableAction(fao, 8, 0, CoreManager.getCoreImplementations(3));
        FakeAllocatableAction action9 = new FakeAllocatableAction(fao, 9, 0, CoreManager.getCoreImplementations(0));
        FakeAllocatableAction action10 = new FakeAllocatableAction(fao, 10, 0, CoreManager.getCoreImplementations(2));
        FakeAllocatableAction action11 = new FakeAllocatableAction(fao, 11, 0, CoreManager.getCoreImplementations(3));
        FakeAllocatableAction action12 = new FakeAllocatableAction(fao, 12, 0, CoreManager.getCoreImplementations(0));
        FakeAllocatableAction action13 = new FakeAllocatableAction(fao, 13, 0, CoreManager.getCoreImplementations(1));

        FakeAllocatableAction action14 = new FakeAllocatableAction(fao, 14, 0, CoreManager.getCoreImplementations(0));
        action14.selectExecution(drs2, (FakeImplementation) action14.getImplementations()[0]);
        FullGraphSchedulingInformation dsi14 = (FullGraphSchedulingInformation) action14.getSchedulingInfo();
        dsi14.setExpectedEnd(10_000);

        FakeAllocatableAction action15 = new FakeAllocatableAction(fao, 15, 0, CoreManager.getCoreImplementations(0));
        action15.selectExecution(drs2, (FakeImplementation) action15.getImplementations()[0]);
        FullGraphSchedulingInformation dsi15 = (FullGraphSchedulingInformation) action15.getSchedulingInfo();
        dsi15.setExpectedEnd(12_000);

        action1.selectExecution(drs1, (FakeImplementation) action1.getImplementations()[0]);
        action1.tryToLaunch();

        action2.selectExecution(drs1, (FakeImplementation) action2.getImplementations()[0]);
        action2.tryToLaunch();

        action3.selectExecution(drs1, (FakeImplementation) action3.getImplementations()[0]);
        addSchedulingDependency(action1, action3);

        action4.selectExecution(drs1, (FakeImplementation) action4.getImplementations()[0]);
        addSchedulingDependency(action2, action4);

        action5.selectExecution(drs1, (FakeImplementation) action5.getImplementations()[0]);
        action5.addDataPredecessor(action2);
        addSchedulingDependency(action3, action5);
        addSchedulingDependency(action4, action5);

        action6.selectExecution(drs1, (FakeImplementation) action6.getImplementations()[0]);
        action6.addDataPredecessor(action2);
        addSchedulingDependency(action5, action6);

        action7.selectExecution(drs1, (FakeImplementation) action7.getImplementations()[0]);
        action7.addDataPredecessor(action2);
        addSchedulingDependency(action5, action7);

        action8.selectExecution(drs1, (FakeImplementation) action8.getImplementations()[0]);
        action8.addDataPredecessor(action5);
        addSchedulingDependency(action6, action8);
        addSchedulingDependency(action7, action8);

        action9.selectExecution(drs1, (FakeImplementation) action9.getImplementations()[0]);
        addSchedulingDependency(action8, action9);
        action9.addDataPredecessor(action5);

        action10.selectExecution(drs1, (FakeImplementation) action10.getImplementations()[0]);
        addSchedulingDependency(action8, action10);

        action11.selectExecution(drs1, (FakeImplementation) action11.getImplementations()[0]);
        addSchedulingDependency(action9, action11);
        addSchedulingDependency(action10, action11);
        action11.addDataPredecessor(action14);

        action12.selectExecution(drs1, (FakeImplementation) action12.getImplementations()[0]);
        addSchedulingDependency(action11, action12);

        action13.selectExecution(drs1, (FakeImplementation) action13.getImplementations()[0]);
        addSchedulingDependency(action11, action13);
        addSchedulingDependency(action12, action13);
        action13.addDataPredecessor(action15);

        // debugActions(action1, action2, action3, action4, action5, action6, action7, action8, action9, action10,
        // action11, action12, action13 );
        LinkedList<AllocatableAction>[] actions = new LinkedList[CoreManager.getCoreCount()];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new LinkedList<>();
        }

        // Actions not depending on other actions scheduled on the same resource
        // Sorted by data dependencies release
        PriorityQueue<AllocatableAction> readyActions = new PriorityQueue<>(1,
                FullGraphResourceScheduler.getReadyComparator());

        // Actions that can be selected to be scheduled on the node
        // Sorted by data dependencies release
        PriorityActionSet selectableActions = new PriorityActionSet(FullGraphResourceScheduler.getScanComparator());

        LinkedList<AllocatableAction> runningActions = drs1.scanActions(readyActions, selectableActions);

        HashMap<AllocatableAction, Long> expectedReady = new HashMap<>();
        expectedReady.put(action11, 10_000l);
        expectedReady.put(action13, 12_000l);
        Verifiers.verifyReadyActions(new PriorityQueue<>(readyActions), expectedReady);
        AllocatableAction[] expectedSelectable = new AllocatableAction[]{action3, action4, action10, action12};
        Verifiers.verifyPriorityActions(new PriorityActionSet(selectableActions), expectedSelectable);

        PriorityQueue<AllocatableAction> donationActions = new PriorityQueue<>(1,
                ScheduleOptimizer.getDonationComparator());
        drs1.rescheduleTasks(System.currentTimeMillis(), readyActions, selectableActions, runningActions,
                donationActions);

        /*
         * drs.seekGaps(System.currentTimeMillis(), gaps, actions);
         * 
         * long[][][] times = { new long[][]{//CORE 0 new long[]{0, CORE0}, //1 new long[]{0, CORE0}, //2 new
         * long[]{CORE0, 2 * CORE0}, //3 new long[]{CORE0, 2 * CORE0}, //4 new long[]{2 * CORE0 + CORE1, 3 * CORE0 +
         * CORE1}, //6 new long[]{3 * CORE0 + CORE1 + CORE3, 4 * CORE0 + CORE1 + CORE3}, //9 new long[]{10_000 + CORE3,
         * 10_000 + CORE3 + CORE0}, //12 }, new long[][]{//CORE 1 new long[]{2 * CORE0, 2 * CORE0 + CORE1}, //5 new
         * long[]{12_000, 12_000 + CORE1}, //13 }, new long[][]{//CORE 2 new long[]{2 * CORE0 + CORE1, 2 * CORE0 + CORE1
         * + CORE2}, //7 new long[]{3 * CORE0 + CORE1 + CORE3, 3 * CORE0 + CORE1 + CORE2 + CORE3}, //10 }, new
         * long[][]{//CORE 3 new long[]{3 * CORE0 + CORE1, 3 * CORE0 + CORE1 + CORE3}, //8 new long[]{10_000, 10_000 +
         * CORE3}, //11 },}; Verifiers.verifyUpdate(actions, times);
         * 
         * Gap[] expectedGaps = { new Gap(2 * CORE0, 3 * CORE0 + CORE1, action3, new FakeResourceDescription(1), 0), new
         * Gap(2 * CORE0 + CORE1 + CORE2, 3 * CORE0 + CORE1, action7, new FakeResourceDescription(1), 0), new Gap(3 *
         * CORE0 + CORE1 + CORE3, 10_000, action8, new FakeResourceDescription(1), 0), new Gap(3 * CORE0 + CORE1 + CORE2
         * + CORE3, 10_000, action10, new FakeResourceDescription(1), 0), new Gap(4 * CORE0 + CORE1 + CORE3, 10_000,
         * action9, new FakeResourceDescription(2), 0), new Gap(10_000 + CORE3 + CORE0, 12_000, action12, new
         * FakeResourceDescription(2), 0), new Gap(10_000 + CORE3, 12_000, action11, new FakeResourceDescription(1), 0),
         * new Gap(10_000 + CORE3, Long.MAX_VALUE, action11, new FakeResourceDescription(1), 0), new Gap(12_000 + CORE1,
         * Long.MAX_VALUE, action13, new FakeResourceDescription(3), 0),}; Verifiers.verifyGaps(gaps, expectedGaps);
         */
    }

    private void addSchedulingDependency(FakeAllocatableAction pred, FakeAllocatableAction succ) {
        FullGraphSchedulingInformation predDSI = (FullGraphSchedulingInformation) pred.getSchedulingInfo();
        predDSI.lock();
        FullGraphSchedulingInformation succDSI = (FullGraphSchedulingInformation) succ.getSchedulingInfo();
        succDSI.lock();
        if (pred.isPending()) {
            predDSI.addSuccessor(succ);
            succDSI.addPredecessor(pred);
        }
    }

}
