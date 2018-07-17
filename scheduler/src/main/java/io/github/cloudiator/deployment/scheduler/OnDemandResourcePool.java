/*
 * Copyright 2018 University of Ulm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.cloudiator.deployment.scheduler;

import com.google.inject.Inject;
import io.github.cloudiator.domain.Node;
import io.github.cloudiator.messaging.NodeToNodeMessageConverter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.cloudiator.matchmaking.converters.RequirementConverter;
import org.cloudiator.matchmaking.domain.Requirement;
import org.cloudiator.messages.General.Error;
import org.cloudiator.messages.Node.NodeRequestMessage;
import org.cloudiator.messages.Node.NodeRequestResponse;
import org.cloudiator.messages.NodeEntities.NodeRequirements;
import org.cloudiator.messaging.ResponseCallback;
import org.cloudiator.messaging.services.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnDemandResourcePool implements ResourcePool {

  private final NodeService nodeService;
  private static final RequirementConverter REQUIREMENT_CONVERTER = RequirementConverter.INSTANCE;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnDemandResourcePool.class);
  private static final NodeToNodeMessageConverter nodeConverter = new NodeToNodeMessageConverter();

  @Inject
  public OnDemandResourcePool(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  @Override
  public Iterable<Node> allocate(String userId, Iterable<? extends Requirement> requirements) {

    final NodeRequirements nodeRequirements = NodeRequirements.newBuilder()
        .addAllRequirements(StreamSupport.stream(requirements.spliterator(), false)
            .map(REQUIREMENT_CONVERTER::applyBack).collect(
                Collectors.toList())).build();

    final NodeRequestMessage requestMessage = NodeRequestMessage.newBuilder().setGroupName("blub")
        .setUserId(userId)
        .setNodeRequest(nodeRequirements).build();

    CountDownLatch countDownLatch = new CountDownLatch(1);

    final Set<Node> nodes = new HashSet<>();
    IllegalStateException illegalStateException = null;

    nodeService.createNodesAsync(requestMessage,
        new ResponseCallback<NodeRequestResponse>() {
          @Override
          public void accept(@Nullable NodeRequestResponse content, @Nullable Error error) {
            if (content != null) {
              nodes.addAll(
                  content.getNodeGroup().getNodesList().stream().map(nodeConverter::applyBack)
                      .collect(Collectors.toSet()));
            } else if (error != null) {
              LOGGER.error(String
                  .format("Error while allocating nodes. Code: %s, Message: %s", error.getCode(),
                      error.getMessage()));
            }
            countDownLatch.countDown();
          }
        });

    try {
      countDownLatch.await();

      if (nodes.isEmpty()) {
        throw new IllegalStateException("Failed to allocate nodes.");
      }

      return nodes;
    } catch (InterruptedException e) {
      throw new IllegalStateException(
          "ResourcePool was interrupted while waiting for node allocation.", e);
    }
  }
}