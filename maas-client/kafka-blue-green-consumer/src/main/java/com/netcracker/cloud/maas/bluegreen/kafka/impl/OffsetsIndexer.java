package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Index all offsets ignoring group.id. So, place to this class only offsets for one group.id
 */
@Slf4j
public class OffsetsIndexer {
    private final AdminAdapter admin;
    private final Map<GroupId, Map<TopicPartition, OffsetAndMetadata>> index = new TreeMap<>(comp);

    private static Comparator<GroupId> comp = (vg1, vg2) -> {
        int order = Objects.compare(vg1, vg2, Comparator.<GroupId, OffsetDateTime>comparing(v -> v instanceof VersionedGroupId vg ?
                vg.getUpdated() : v instanceof BG1VersionedGroupId ovg ? ovg.getTime() : OffsetDateTime.MIN).reversed());
        if (order == 0) {
            return Objects.compare(vg1, vg2, Comparator.comparing(GroupId::toString).reversed());
        }
        return order;
    };

    public OffsetsIndexer(String groupIdPrefix, AdminAdapter admin) {
        this.admin = admin;
        log.info("Index existing group id offsets for: {}", groupIdPrefix);

        List<GroupId> groupList = admin.listConsumerGroup().stream()
                .filter(cgl -> cgl.groupId().startsWith(groupIdPrefix)) // use this filter for performance purpose
                .map(cgl -> GroupId.parse(cgl.groupId()))
                .filter(groupId -> Objects.equals(groupIdPrefix, groupId.getGroupIdPrefix()))
                .sorted(Comparator.comparing(GroupId::toString))
                .toList();

        for (GroupId groupId : groupList) {
            String groupName = groupId.toString();
            log.debug("Found consumer group: {}", groupName);
            Map<TopicPartition, OffsetAndMetadata> offsets = admin.listConsumerGroupOffsets(groupName);
            index.put(groupId, offsets);
        }
    }

    void removeOldGroups(int historySize) {
        if (index.size() > historySize) {
            List<GroupId> groupsToDelete = index.keySet().stream().sorted(comp.reversed()).toList().subList(0, index.size() - historySize);
            log.info("Removing {} old consumer groups", groupsToDelete.size());
            Set<String> groupNamesToDelete = groupsToDelete.stream().map(GroupId::toString).collect(Collectors.toSet());
            removeGroups(groupNamesToDelete);
        }
    }

    void removeGroups(Collection<String> groupNames) {
        log.info("Deleting consumer groups: {}", groupNames);
        admin.deleteConsumerGroups(groupNames);
    }

    public boolean exists(GroupId search) {
        return index.containsKey(search);
    }

    public boolean bg1VersionsExist() {
        return index.keySet().stream().anyMatch(g -> g instanceof BG1VersionedGroupId);
    }

    public boolean bg1VersionsMigrated() {
        return index.keySet().stream().anyMatch(g -> g instanceof BG1VersionedGroupId bg1v &&
                Objects.equals(bg1v.getStage(), BG1VersionedGroupId.MIGRATED_STAGE));
    }

    void createMigrationDoneFromBg1MarkerGroup() {
        index.keySet().stream().filter(g -> g instanceof BG1VersionedGroupId bg1vg &&
                        Objects.equals(BG1VersionedGroupId.ACTIVE_STAGE, bg1vg.getStage()))
                .map(g -> (BG1VersionedGroupId) g)
                .min(comp).ifPresentOrElse(bg1g -> {
                    BG1VersionedGroupId migratedMarkerGroup = new BG1VersionedGroupId(bg1g.getGroupIdPrefix(), bg1g.getVersion(),
                            bg1g.getBlueGreenVersion(), BG1VersionedGroupId.MIGRATED_STAGE, OffsetDateTime.now());
                    log.info("Creating migrated from BG1 marker Group: '{}'", migratedMarkerGroup);
                    admin.alterConsumerGroupOffsets(migratedMarkerGroup, index.get(bg1g));
                }, () -> log.warn("Did not create 'migrated from BG1 marker Group' because no bg1 active group was found"));
    }

    /**
     * See bg2-states-kafka-offsets.md for details about state transitions
     *
     * @param current
     * @return
     */
    public Set<GroupIdWithOffset> findPreviousStateOffsets(GroupId current) {
        log.info("Search the ancestor for: {}", current);
        Set<GroupIdWithOffset> result = index.entrySet().stream()
                .filter(entry -> !Objects.equals(current, entry.getKey()))
                // filter out offsets of the same update time (already created in sibling ns)
                .filter(entry -> current instanceof VersionedGroupId vg1 && entry.getKey() instanceof VersionedGroupId vg2 ?
                        (vg2.getUpdated().isBefore(vg1.getUpdated())) : true)
                // find latest groupId
                .min(Map.Entry.comparingByKey(comp))
                .map(Map.Entry::getKey).map(g1 -> {
                    // if latest groupId is versioned - find all groupIds of the same updateTime
                    if (g1 instanceof VersionedGroupId vg1) {
                        return index.keySet().stream()
                                .filter(g2 -> g2 instanceof VersionedGroupId vg2 && Objects.equals(vg2.getUpdated(), vg1.getUpdated()))
                                .toList();
                    } else if (g1 instanceof BG1VersionedGroupId ovg1) {
                        // old blue-green groupId, we are interested only in active groupId
                        return index.keySet().stream()
                                .filter(g2 -> g2 instanceof BG1VersionedGroupId ovg2 &&
                                        Objects.equals(ovg2.getTime(), ovg1.getTime()) &&
                                        Objects.equals(ovg2.getStage(), BG1VersionedGroupId.ACTIVE_STAGE))
                                .toList();
                    } else {
                        return List.of(g1);
                    }
                })
                .stream().flatMap(Collection::stream).map(g -> new GroupIdWithOffset(g, index.get(g))).collect(Collectors.toSet());
        if (result.isEmpty()) {
            log.info("There are no ancestors for: '{}'", current);
        } else {
            log.info("Ancestors for: '{}' are: {}", current, result);
        }
        return result;
    }

    public void dump(BiConsumer<GroupId, Map<TopicPartition, OffsetAndMetadata>> acceptor) {
        index.forEach(acceptor);
    }

}
