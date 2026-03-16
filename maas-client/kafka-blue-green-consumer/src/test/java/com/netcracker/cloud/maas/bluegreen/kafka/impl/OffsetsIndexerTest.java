package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.netcracker.cloud.maas.bluegreen.kafka.TestUtil.*;

class OffsetsIndexerTest {

    @Test
    void testRemoveOldGroups() {
        AdminAdapter adminAdapter = adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 12)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 23)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 24))
        );
        OffsetsIndexer indexer = indexer(adminAdapter);
        indexer.removeOldGroups(2);
        Mockito.verify(adminAdapter).deleteConsumerGroups(Set.of("test", "test-v1-a_i-2023-07-07_10-30-00"));
    }

    @Test
    void testExists() {
        AdminAdapter adminAdapter = adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 12))
        );
        OffsetsIndexer indexer = indexer(adminAdapter);
        Assertions.assertTrue(indexer.exists(GroupId.parse("test-v1-a_i-2023-07-07_10-30-00")));
    }

    @Test
    void testActiveIdle_InitDomain() {
        AdminAdapter adminAdapter = adminAdapter(
                group("test", topicOffset(0, 1))
        );
        OffsetsIndexer indexer = indexer(adminAdapter);
        GroupId current = GroupId.parse("test-v1-a_i-2023-07-07_10-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test", topicOffset(0, 1))

        ), previousStageOffsets);
    }

    @Test
    void testActiveIdle_Commit() {
        AdminAdapter adminAdapter = adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4))
        );
        OffsetsIndexer indexer = indexer(adminAdapter);
        GroupId current = GroupId.parse("test-v1-a_i-2023-07-07_12-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                versionedGroupIdWithOffset("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4))
        ), previousStageOffsets);
    }

    @Test
    void testActiveIdle_Commit_NoPrev() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ));
        GroupId current = GroupId.parse("test-v1-a_i-2023-07-07_12-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Warmup() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_11-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Warmup_SiblingGroupExists() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 3))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_11-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Warmup_NoPrev() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_11-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test", topicOffset(0, 1))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Promote() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ));
        GroupId current = GroupId.parse("test-v3-a_l-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                versionedGroupIdWithOffset("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Promote_SiblingGroupExists() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 8))
        ));
        GroupId current = GroupId.parse("test-v3-a_l-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                versionedGroupIdWithOffset("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Promote_NoPrevActiveCandidate() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5))
        ));
        GroupId current = GroupId.parse("test-v3-a_l-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Promote_NoPrevCandidate() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6))
        ));
        GroupId current = GroupId.parse("test-v3-a_l-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Promote_NoPrevActive() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ));
        GroupId current = GroupId.parse("test-v3-a_l-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Rollback() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8)),
                group("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8)),
                versionedGroupIdWithOffset("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Rollback_SiblingGroupExists() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8)),
                group("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9)),
                group("test-v3-c_a-2023-07-07_15-30-00", topicOffset(0, 10))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8)),
                versionedGroupIdWithOffset("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Rollback_NoPrevActiveLegacy() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                versionedGroupIdWithOffset("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Rollback_NoPrevLegacy() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8))
        ), previousStageOffsets);
    }

    @Test
    void testActive_Rollback_NoPrevActive() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ), previousStageOffsets);
    }

    @Test
    void testCandidate_Warmup() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ));
        GroupId current = GroupId.parse("test-v2-c_a-2023-07-07_11-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ), previousStageOffsets);
    }

    @Test
    void testCandidate_Warmup_SiblingGroupExists() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3))
        ));
        GroupId current = GroupId.parse("test-v2-c_a-2023-07-07_11-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ), previousStageOffsets);
    }

    @Test
    void testCandidate_Warmup_NoPrev() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1))
        ));
        GroupId current = GroupId.parse("test-v2-c_a-2023-07-07_11-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test", topicOffset(0, 1))
        ), previousStageOffsets);
    }

    @Test
    void testCandidate_Rollback() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8)),
                group("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ));
        GroupId current = GroupId.parse("test-v3-c_a-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8)),
                versionedGroupIdWithOffset("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ), previousStageOffsets);
    }

    @Test
    void testCandidate_Rollback_SiblingGroupExists() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8)),
                group("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9)),
                group("test-v1-a_c-2023-07-07_15-30-00", topicOffset(0, 10))
        ));
        GroupId current = GroupId.parse("test-v3-c_a-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8)),
                versionedGroupIdWithOffset("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ), previousStageOffsets);
    }

    @Test
    void testCandidate_Rollback_NoPrevActiveLegacy() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ));
        GroupId current = GroupId.parse("test-v3-c_a-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                versionedGroupIdWithOffset("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ), previousStageOffsets);
    }

    @Test
    void testCandidate_Rollback_NoPrevLegacy() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ));
        GroupId current = GroupId.parse("test-v3-c_a-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-l_a-2023-07-07_14-30-00", topicOffset(0, 9))
        ), previousStageOffsets);
    }

    @Test
    void testCandidate_Rollback_NoPrevActive() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v3-a_c-2023-07-07_14-30-00", topicOffset(0, 8))
        ));
        GroupId current = GroupId.parse("test-v3-c_a-2023-07-07_15-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v3-a_c-2023-07-07_14-30-00", topicOffset(0, 8))
        ), previousStageOffsets);
    }

    @Test
    void testLegacy_Promote() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ));
        GroupId current = GroupId.parse("test-v1-l_a-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                versionedGroupIdWithOffset("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ), previousStageOffsets);
    }

    @Test
    void testLegacy_Promote_SiblingGroupExists() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7)),
                group("test-v3-a_l-2023-07-07_14-30-00", topicOffset(0, 8))
        ));
        GroupId current = GroupId.parse("test-v1-l_a-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6)),
                versionedGroupIdWithOffset("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ), previousStageOffsets);
    }

    @Test
    void testLegacy_Promote_NoPrevActiveCandidate() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5))
        ));
        GroupId current = GroupId.parse("test-v1-l_a-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5))
        ), previousStageOffsets);
    }

    @Test
    void testLegacy_Promote_NoPrevCandidate() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6))
        ));
        GroupId current = GroupId.parse("test-v1-l_a-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_c-2023-07-07_13-30-00", topicOffset(0, 6))
        ), previousStageOffsets);
    }

    @Test
    void testLegacy_Promote_NoPrevActive() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-v1-a_c-2023-07-07_11-30-00", topicOffset(0, 3)),
                group("test-v2-c_a-2023-07-07_11-30-00", topicOffset(0, 4)),
                group("test-v1-a_i-2023-07-07_12-30-00", topicOffset(0, 5)),
                group("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ));
        GroupId current = GroupId.parse("test-v1-l_a-2023-07-07_14-30-00");
        Set<GroupIdWithOffset> previousStageOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v3-c_a-2023-07-07_13-30-00", topicOffset(0, 7))
        ), previousStageOffsets);
    }

    @Test
    void testSimilarNamesOfGroups() {
        OffsetsIndexer indexer = indexer(adminAdapter(
                group("test", topicOffset(0, 1)),
                group("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2)),
                group("test-with-suffix", topicOffset(0, 3)),
                group("test-with-suffix-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 4))
        ));
        GroupId current = GroupId.parse("test-v1-a_c-2023-07-07_11-30-00");
        Set<GroupIdWithOffset> previousStateOffsets = indexer.findPreviousStateOffsets(current);
        Assertions.assertEquals(Set.of(
                versionedGroupIdWithOffset("test-v1-a_i-2023-07-07_10-30-00", topicOffset(0, 2))
        ), previousStateOffsets);
    }
}
