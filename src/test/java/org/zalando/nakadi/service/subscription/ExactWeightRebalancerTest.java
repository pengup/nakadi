package org.zalando.nakadi.service.subscription;

import org.junit.Test;
import org.zalando.nakadi.domain.EventTypePartition;
import org.zalando.nakadi.service.subscription.model.Partition;
import org.zalando.nakadi.service.subscription.model.Session;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.zalando.nakadi.service.subscription.model.Partition.State.ASSIGNED;
import static org.zalando.nakadi.service.subscription.model.Partition.State.REASSIGNING;

public class ExactWeightRebalancerTest {

    @Test(expected = IllegalArgumentException.class)
    public void splitByWeightShouldAcceptOnlyCorrectData1() {
        ExactWeightRebalancer.splitByWeight(1, new int[]{1, 1});
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitByWeightShouldAcceptOnlyCorrectData2() {
        ExactWeightRebalancer.splitByWeight(2, new int[]{1, 0});
    }

    @Test
    public void splitByWeightMustCorrectlyWorkOnDifferentValues() {
        assertArrayEquals(
                new int[]{1, 1},
                ExactWeightRebalancer.splitByWeight(2, new int[]{1, 1}));
        assertArrayEquals(
                new int[]{2, 1},
                ExactWeightRebalancer.splitByWeight(3, new int[]{1, 1}));
        assertArrayEquals(
                new int[]{1, 2},
                ExactWeightRebalancer.splitByWeight(3, new int[]{1, 2}));
        assertArrayEquals(
                new int[]{34, 33, 33},
                ExactWeightRebalancer.splitByWeight(100, new int[]{1, 1, 1}));
        assertArrayEquals(
                new int[]{26, 25, 50},
                ExactWeightRebalancer.splitByWeight(101, new int[]{1, 1, 2}));
    }

    @Test
    public void rebalanceShouldHaveEmptyChangesetForBalancedData() {
        final ExactWeightRebalancer rebalancer = new ExactWeightRebalancer();

        // 1. Data contains only assigned
        final List<Session> sessions = Arrays.asList(new Session("0", 1), new Session("1", 1));
        assertThat(rebalancer.apply(sessions,
                new Partition[]{
                        new Partition("0", "0", "0", null, ASSIGNED),
                        new Partition("0", "1", "1", null, ASSIGNED),
                        new Partition("1", "0", "1", null, ASSIGNED),
                        new Partition("1", "1", "0", null, ASSIGNED)}),
                emptyArray());

        // 2. Data contains reassinging
        assertThat(rebalancer.apply(sessions,
                new Partition[]{
                        new Partition("0", "0", "0", null, ASSIGNED),
                        new Partition("0", "1", "0", "1", REASSIGNING),
                        new Partition("1", "0", "1", null, ASSIGNED),
                        new Partition("1", "1", "0", null, ASSIGNED)}),
                emptyArray());

        // 3. Data contains only reassinging
        assertThat(rebalancer.apply(sessions,
                new Partition[]{
                        new Partition("0", "0", "0", "1", REASSIGNING),
                        new Partition("0", "1", "0", "1", REASSIGNING),
                        new Partition("1", "0", "1", "0", REASSIGNING),
                        new Partition("1", "1", "1", "0", REASSIGNING)}),
                emptyArray());
    }

    @Test
    public void rebalanceShouldRemoveDeadSessions() {
        final Partition[] changeset = new ExactWeightRebalancer().apply(
                Arrays.asList(new Session("1", 1), new Session("2", 1)),
                new Partition[]{
                        new Partition("0", "0", "0", null, ASSIGNED),
                        new Partition("0", "1", "0", "1", REASSIGNING),
                        new Partition("1", "0", "1", null, ASSIGNED),
                        new Partition("1", "1", "0", null, ASSIGNED)});

        assertEquals(3, changeset.length);
        // All partitions must be in assigned state
        assertFalse(Stream.of(changeset).anyMatch(p -> p.getState() != ASSIGNED));
        // All partitions must not have nextSessionId
        assertFalse(Stream.of(changeset).anyMatch(p -> p.getNextSession() != null));

        assertFalse(Stream.of(changeset).anyMatch(p -> p.getKey().equals(new EventTypePartition("1", "0"))));
        assertEquals(1, Stream.of(changeset).filter(p -> p.getSession().equals("1")).count());
        assertEquals(2, Stream.of(changeset).filter(p -> p.getSession().equals("2")).count());
        assertFalse(Stream.of(changeset).anyMatch(p -> p.getState() != ASSIGNED));
    }

    @Test
    public void rebalanceShouldMoveToReassigningState() {
        final Partition[] changeset = new ExactWeightRebalancer().apply(
                Arrays.asList(new Session("1", 1), new Session("2", 1), new Session("3", 1)),
                new Partition[]{
                        new Partition("0", "0", "1", null, ASSIGNED),
                        new Partition("0", "1", "1", null, ASSIGNED),
                        new Partition("1", "0", "2", null, ASSIGNED),
                        new Partition("1", "1", "2", null, ASSIGNED)});
        assertEquals(1, changeset.length);
        final Partition changed = changeset[0];
        assertTrue(
                changed.getKey().equals(new EventTypePartition("1", "0")) ||
                        changed.getKey().equals(new EventTypePartition("1", "1")));
        assertEquals("2", changed.getSession());
        assertEquals("3", changed.getNextSession());
        assertEquals(REASSIGNING, changed.getState());
    }

    @Test
    public void rebalanceShouldTakeRebalancingPartitions() {
        final Partition[] changeset = new ExactWeightRebalancer().apply(
                Arrays.asList(new Session("1", 1), new Session("2", 1), new Session("3", 1)),
                new Partition[]{
                        new Partition("0", "0", "1", null, ASSIGNED),
                        new Partition("0", "1", "1", null, ASSIGNED),
                        new Partition("1", "0", "2", null, REASSIGNING),
                        new Partition("1", "1", "2", null, ASSIGNED)});
        assertEquals(1, changeset.length);
        final Partition changed = changeset[0];
        assertEquals(new EventTypePartition("1", "0"), changed.getKey());
        assertEquals("2", changed.getSession());
        assertEquals("3", changed.getNextSession());
        assertEquals(REASSIGNING, changed.getState());
    }
}
