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

package io.github.cloudiator.persistance;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.cloudiator.deployment.domain.CloudiatorProcess;
import io.github.cloudiator.deployment.domain.CloudiatorProcess.ProcessState;
import io.github.cloudiator.deployment.domain.CloudiatorProcess.Type;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.ManyToOne;


/**
 * Created by daniel on 12.12.14.
 */
@Entity
@Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
abstract class ProcessModel extends Model {

  @Column(unique = true, nullable = false)
  private String domainId;

  private String originId;

  @ManyToOne(optional = false)
  private ScheduleModel schedule;

  @Column(nullable = false)
  private String task;

  @Enumerated(EnumType.STRING)
  private CloudiatorProcess.Type type;

  @Enumerated(EnumType.STRING)
  private CloudiatorProcess.ProcessState state;

  @ManyToOne
  @Nullable
  private ProcessGroupModel processGroupModel;

  /**
   * Empty constructor for hibernate
   */
  protected ProcessModel() {
  }

  public ProcessModel(String domainId, String orginId, ScheduleModel schedule, String task,
      CloudiatorProcess.ProcessState state, CloudiatorProcess.Type type,
      @Nullable ProcessGroupModel processGroupModel) {

    checkNotNull(domainId, "domainId is null");
    checkArgument(!domainId.isEmpty(), "domainId is empty");

    checkNotNull(schedule, "schedule is null");

    checkNotNull(task, "task is null");
    checkArgument(!task.isEmpty(), "task is empty");

    checkNotNull(type, "type is null");

    this.domainId = domainId;
    this.originId = orginId;
    this.schedule = schedule;
    this.task = task;
    this.state = state;
    this.type = type;
    this.processGroupModel = processGroupModel;

  }

  @Override
  protected ToStringHelper stringHelper() {
    return super.stringHelper().add("domainId", domainId).add("schedule", schedule)
        .add("task", task).add("state", state);
  }

  @Override
  public String toString() {
    return stringHelper().toString();
  }

  public ProcessModel assignGroup(ProcessGroupModel processGroupModel) {
    checkState(this.processGroupModel == null, "Process Group was already assigned.");
    this.processGroupModel = processGroupModel;
    return this;
  }

  public String getDomainId() {
    return domainId;
  }

  public ScheduleModel getSchedule() {
    return schedule;
  }

  public String getTask() {
    return task;
  }

  public ProcessState getState() {
    return state;
  }

  public Type getType() {
    return type;
  }

  public TenantModel getTenant() {
    return schedule.tenant();
  }

  public ProcessModel setState(
      ProcessState state) {
    this.state = state;
    return this;
  }
}
