package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This class minimizes api coverage field on flourished kafka Admin implementation.
 * It simplifies tests a lot.
 */
public class AdminAdapterImpl implements AdminAdapter {
	private final Admin admin;

	public AdminAdapterImpl(Supplier<Admin> adminSupplier) {
		this.admin = adminSupplier.get();
	}

	@Override
	@SneakyThrows
	public Collection<ConsumerGroupListing> listConsumerGroup() {
		return admin.listConsumerGroups().all().get();
	}

	@Override
	@SneakyThrows
	public Map<TopicPartition, OffsetAndMetadata> listConsumerGroupOffsets(String groupId) {
		return admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();
	}

	@Override
	@SneakyThrows
	public void alterConsumerGroupOffsets(GroupId groupId, Map<TopicPartition, OffsetAndMetadata> offsets) {
		admin.alterConsumerGroupOffsets(groupId.toString(), offsets).all().get();
	}

	@Override
	@SneakyThrows
	public void deleteConsumerGroupOffsets(GroupId groupId, Map<TopicPartition, OffsetAndMetadata> offsets) {
		admin.deleteConsumerGroupOffsets(groupId.toString(), offsets.keySet()).all().get();
	}
	@Override
	@SneakyThrows
	public void deleteConsumerGroups(Collection<String> groupIds) {
		admin.deleteConsumerGroups(groupIds).all().get();
	}

	@Override
	public void close() {
		this.admin.close();
	}
}
