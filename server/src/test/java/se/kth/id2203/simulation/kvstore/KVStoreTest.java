/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.simulation.kvstore;

import junit.framework.Assert;
import org.junit.Test;
import se.kth.id2203.kvstore.KVEntry;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.HashMap;
import java.util.StringTokenizer;

import static org.junit.Assert.fail;

/**
 * @author Lars Kroll <lkroll@kth.se>
 */
public class KVStoreTest {

    private static final int NUM_MESSAGES_OK = 3;
    private static final int NUM_MESSAGES_NOT_FOUND = 7;
    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    /*** CAS
     * TODO :
     * * concurrent write and cas - one read
     * * concurrent cas and read
     */

    // WRITE AND READ

   @Test
    public void simpleOpsTest() {
        // Initialization of the store
        HashMap<Integer, KVEntry> store = new HashMap<>();
        store.put("test0".hashCode(), new KVEntry("test0".hashCode(), 41));
        store.put("test1".hashCode(), new KVEntry("test1".hashCode(), 40));
        store.put("test2".hashCode(), new KVEntry("test2".hashCode(), 39));

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.oneClient(6);
        res.put("messages", NUM_MESSAGES_OK + NUM_MESSAGES_NOT_FOUND);
        res.put("testNum", 1);
        simpleBootScenario.simulate(LauncherComp.class);
        for (int i = 0; i < NUM_MESSAGES_OK; i++) {
            String k = "test" + i;
            Integer kvEntry = res.get(k, Integer.class);
            Assert.assertEquals(store.get(k.hashCode()).getValue(), kvEntry);
        }
        for (int j = NUM_MESSAGES_OK; j < NUM_MESSAGES_NOT_FOUND + NUM_MESSAGES_OK; j++) {
            String k = "test" + j;
            Assert.assertEquals("NOT_FOUND", res.get(k, String.class));
        }
    }

    @Test
    public void twoWritesAndRead() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.twoClientsSameOperation(6);
        res.put("testNum", 2);
        simpleBootScenario.simulate(LauncherComp.class);
        Assert.assertEquals(new Integer(1), res.get("client1", Integer.class));
        Assert.assertEquals(new Integer(2), res.get("client2", Integer.class));
    }

    @Test
    public void twoWritesAndReadDifferentClient() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.twoClientsAlternate(6);
        res.put("testNum", 3);
        simpleBootScenario.simulate(LauncherComp.class);
        Assert.assertEquals(new Integer(2), res.get("client1", Integer.class));
        Assert.assertEquals(new Integer(1), res.get("client2", Integer.class));
    }

    @Test
    public void concurrentOperation() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.threeClientsConcurrentOperation(6);
        res.put("testNum", 4);
        simpleBootScenario.simulate(LauncherComp.class);
        Object res1 = res.get("client2", Object.class);
        System.out.println(res1);
        if (res1 instanceof String) {
            if (res1.equals("NOT_FOUND")) {
                res1 = null;
            }
        }
        Assert.assertTrue((res1 == null) || (((Integer)res1) == 2));
        Assert.assertEquals(new Integer(2), res.get("client3", Integer.class));
    }

    @Test
    public void failedWrite() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.failure(6);
        res.put("testNum", 5);
        simpleBootScenario.simulate(LauncherComp.class);
        Object res1 = res.get("client2", Object.class);
        if (res1 instanceof String) {
            if (res1.equals("NOT_FOUND")) {
                res1 = null;
            }
        }
        Assert.assertTrue((res1 == null) || (((Integer)res1) == 2));
    }

    // CAS Tests

    @Test
    public void simpleCASandRead() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.simpleCas(6);
        res.put("testNum", 6);
        simpleBootScenario.simulate(LauncherComp.class);
        Assert.assertEquals(new Integer(1), res.get("entry6", Integer.class));
    }

    @Test
    public void failedCASandRead() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.simpleCas(6);
        res.put("testNum", 7);
        simpleBootScenario.simulate(LauncherComp.class);
        Assert.assertEquals(new Integer(0), res.get("entry7", Integer.class));
    }

    @Test
    public void concurrentPutAndCAS() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.concurrentPutAndCas(6);
        res.put("testNum", 8);
        simpleBootScenario.simulate(LauncherComp.class);
        Integer res1 = res.get("client2", Integer.class);
        Integer res2 = res.get("client3", Integer.class);
        Assert.assertTrue((res1 == 10) || (res1 == 20));
        Assert.assertEquals(res1, res2);
    }

    @Test
    public void concurrentTwoCAS() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.concurrentTwoCas(6);
        res.put("testNum", 9);
        simpleBootScenario.simulate(LauncherComp.class);
        Integer res1 = res.get("client3", Integer.class);
        Integer res2 = res.get("client4", Integer.class);
        Assert.assertTrue((res1 == 10) || (res1 == 20));
        Assert.assertEquals(res1, res2);
    }

    @Test
    public void failedCas() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.failure(6);
        res.put("testNum", 10);
        simpleBootScenario.simulate(LauncherComp.class);
        Integer res1 = res.get("client2", Integer.class);
        Assert.assertTrue((res1 == 39) || (res1 == 2));
    }

    @Test
    public void casOnNull() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.failure(6);
        res.put("testNum", 11);
        simpleBootScenario.simulate(LauncherComp.class);
        String res1 = res.get("client2", String.class);
        Assert.assertEquals("NOT_FOUND", res1);
    }

}
