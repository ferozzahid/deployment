/*
 * Copyright 2017 University of Ulm
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

package io.github.cloudiator.deployment.domain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.util.Objects;

class CloudiatorProcessImpl implements CloudiatorProcess {

  private final String id;
  private final String scheduleId;
  private final String taskName;
  private final String nodeId;
  private final CloudiatorProcess.State state;
  private final Type type;

  CloudiatorProcessImpl(String id, String scheduleId, String taskName, String nodeId,
      State state, Type type) {

    checkNotNull(id, "id is null");
    checkArgument(!id.isEmpty(), "id is empty");
    this.id = id;

    checkNotNull(scheduleId, "scheduleId is null");
    checkArgument(!scheduleId.isEmpty(), "scheduleId is empty");
    this.scheduleId = scheduleId;

    checkNotNull(taskName, "taskName is null");
    checkArgument(!taskName.isEmpty(), "taskName is empty");
    this.taskName = taskName;

    checkNotNull(nodeId, "nodeId is null");
    checkArgument(!nodeId.isEmpty(), "nodeId is empty");
    this.nodeId = nodeId;

    checkNotNull(state, "state is null");
    this.state = state;

    checkNotNull(type, "type is null");
    this.type = type;
  }

  @Override
  public String scheduleId() {
    return scheduleId;
  }

  @Override
  public String taskId() {
    return taskName;
  }

  @Override
  public State state() {
    return state;
  }

  @Override
  public String nodeId() {
    return nodeId;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloudiatorProcessImpl that = (CloudiatorProcessImpl) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(scheduleId, that.scheduleId) &&
        Objects.equals(taskName, that.taskName) &&
        Objects.equals(nodeId, that.nodeId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, scheduleId, taskName, nodeId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).add("scheduleId", scheduleId)
        .add("task", taskName)
        .add("nodeId", nodeId).add("state", state).toString();
  }
}
