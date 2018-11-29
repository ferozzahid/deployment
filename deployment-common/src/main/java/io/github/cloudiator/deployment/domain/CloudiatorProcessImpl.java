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

abstract class CloudiatorProcessImpl implements CloudiatorProcess {

  protected final String id;
  protected final String scheduleId;
  protected final String taskName;
  protected final CloudiatorProcess.State state;
  protected final Type type;

  CloudiatorProcessImpl(String id, String scheduleId, String taskName, State state, Type type) {

    checkNotNull(id, "id is null");
    checkArgument(!id.isEmpty(), "id is empty");
    this.id = id;

    checkNotNull(scheduleId, "scheduleId is null");
    checkArgument(!scheduleId.isEmpty(), "scheduleId is empty");
    this.scheduleId = scheduleId;

    checkNotNull(taskName, "taskName is null");
    checkArgument(!taskName.isEmpty(), "taskName is empty");
    this.taskName = taskName;


    //todo implement state, currently we simply ignore it
    //checkNotNull(state, "state is null");
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
  public Type type() {
    return type;
  }

  @Override
  public String id() {
    return id;
  }




}
