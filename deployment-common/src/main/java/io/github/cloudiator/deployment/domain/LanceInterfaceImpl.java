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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import io.github.cloudiator.deployment.security.VariableContext;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

class LanceInterfaceImpl implements LanceInterface {

  private final LanceContainerType lanceContainerType;
  @Nullable
  private final String init;
  @Nullable
  private final String preInstall;
  @Nullable
  private final String install;
  @Nullable
  private final String postInstall;
  @Nullable
  private final String preStart;
  private final String start;
  @Nullable
  private final String startDetection;
  @Nullable
  private final String stopDetection;
  @Nullable
  private final String postStart;
  @Nullable
  private final String preStop;
  @Nullable
  private final String stop;
  @Nullable
  private final String postStop;
  @Nullable
  private final String shutdown;
  @Nullable
  private final String portUpdateAction;

  LanceInterfaceImpl(LanceContainerType containerType, @Nullable String init,
      @Nullable String preInstall,
      @Nullable String install,
      @Nullable String postInstall, @Nullable String preStart, String start,
      @Nullable String startDetection,
      @Nullable String stopDetection, @Nullable String postStart, @Nullable String preStop,
      @Nullable String stop,
      @Nullable String postStop, @Nullable String shutdown, @Nullable String portUpdateAction) {

    checkNotNull(containerType, "containerType is null");
    this.lanceContainerType = containerType;

    this.init = init;
    this.preInstall = preInstall;
    this.install = install;
    this.postInstall = postInstall;
    this.preStart = preStart;

    checkNotNull(start, "start is null");
    this.start = start;

    this.startDetection = startDetection;
    this.stopDetection = stopDetection;
    this.postStart = postStart;
    this.preStop = preStop;
    this.stop = stop;
    this.postStop = postStop;
    this.shutdown = shutdown;
    this.portUpdateAction = portUpdateAction;
  }

  @Override
  public LanceContainerType containerType() {
    return lanceContainerType;
  }

  @Override
  public Optional<String> init() {
    return Optional.ofNullable(init);
  }

  @Override
  public Optional<String> preInstall() {
    return Optional.ofNullable(preInstall);
  }

  @Override
  public Optional<String> install() {
    return Optional.ofNullable(install);
  }

  @Override
  public Optional<String> postInstall() {

    return Optional.ofNullable(postInstall);
  }

  @Override
  public Optional<String> preStart() {
    return Optional.ofNullable(preStart);
  }

  @Override
  public String start() {
    return start;
  }

  @Override
  public Optional<String> startDetection() {
    return Optional.ofNullable(startDetection);
  }

  @Override
  public Optional<String> stopDetection() {
    return Optional.ofNullable(stopDetection);
  }

  @Override
  public Optional<String> postStart() {
    return Optional.ofNullable(postStart);
  }

  @Override
  public Optional<String> preStop() {
    return Optional.ofNullable(preStop);
  }

  @Override
  public Optional<String> stop() {
    return Optional.ofNullable(stop);
  }

  @Override
  public Optional<String> postStop() {
    return Optional.ofNullable(postStop);
  }

  @Override
  public Optional<String> shutdown() {
    return Optional.ofNullable(shutdown);
  }

  @Override
  public Optional<String> portUpdateAction() {
    return Optional.ofNullable(portUpdateAction);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LanceInterfaceImpl that = (LanceInterfaceImpl) o;
    return lanceContainerType == that.lanceContainerType &&
        Objects.equals(init, that.init) &&
        Objects.equals(preInstall, that.preInstall) &&
        Objects.equals(install, that.install) &&
        Objects.equals(postInstall, that.postInstall) &&
        Objects.equals(preStart, that.preStart) &&
        Objects.equals(start, that.start) &&
        Objects.equals(startDetection, that.startDetection) &&
        Objects.equals(stopDetection, that.stopDetection) &&
        Objects.equals(postStart, that.postStart) &&
        Objects.equals(preStop, that.preStop) &&
        Objects.equals(stop, that.stop) &&
        Objects.equals(postStop, that.postStop) &&
        Objects.equals(shutdown, that.shutdown) &&
        Objects.equals(portUpdateAction, that.portUpdateAction);
  }

  @Override
  public int hashCode() {

    return Objects.hash(lanceContainerType, init, preInstall, install, postInstall, preStart, start,
        startDetection, stopDetection, postStart, preStop, stop, postStop, shutdown,
        portUpdateAction);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("lanceContainerType", lanceContainerType)
        .add("init", init)
        .add("preInstall", preInstall)
        .add("install", install)
        .add("postInstall", postInstall)
        .add("preStart", preStart)
        .add("start", start)
        .add("startDetection", startDetection)
        .add("stopDetection", stopDetection)
        .add("postStart", postStart)
        .add("preStop", preStop)
        .add("stop", stop)
        .add("postStop", postStop)
        .add("shutdown", shutdown)
        .add("portUpdateAction", portUpdateAction)
        .toString();
  }

  @Override
  public ProcessMapping processMapping() {
    return ProcessMapping.SINGLE;
  }

  @Override
  public boolean isStaticallyConfigured() {
    return !portUpdateAction().isPresent();
  }

  @Override
  public boolean requiresManualWait(TaskInterface dependency) {
    return false;
  }

  @Override
  public TaskInterface decorateEnvironment(Environment environment) {
    return this;
  }

  @Override
  public TaskInterface decorateVariables(VariableContext variableContext) {
    return LanceInterfaceBuilder.of(this).decorate(variableContext).build();
  }
}
