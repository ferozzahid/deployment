package io.github.cloudiator.deployment.lance;

import de.uniulm.omi.cloudiator.lance.application.component.OutPort;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties;
import de.uniulm.omi.cloudiator.lance.application.component.PortProperties.PortType;
import de.uniulm.omi.cloudiator.lance.client.DeploymentHelper;
import de.uniulm.omi.cloudiator.lance.container.spec.os.OperatingSystem;
import de.uniulm.omi.cloudiator.lance.lifecycle.bash.BashBasedHandlerBuilder;
import de.uniulm.omi.cloudiator.lance.lifecycle.detector.PortUpdateHandler;
import io.github.cloudiator.deployment.domain.Job;
import io.github.cloudiator.deployment.domain.PortProvided;
import io.github.cloudiator.deployment.domain.PortRequired;

public class ComponentSupplierUtils {

  private ComponentSupplierUtils(){}

  public static PortType derivePortType(Job job, PortProvided provided) {
    if (job.attachedCommunications(provided).isEmpty()) {
      return PortProperties.PortType.PUBLIC_PORT;
    } else {
      // todo should be internal, but for the time being we use public here
      // todo facilitates the security group handling
      // todo portType = PortProperties.PortType.INTERNAL_PORT;
      return PortProperties.PortType.PUBLIC_PORT;
    }
  }

  public static int deriveMinSinks(PortRequired portRequired) {
    if (portRequired.isMandatory()) {
      return 1;
    }
    return OutPort.NO_SINKS;
  }

  public static PortUpdateHandler portUpdateHandler(PortRequired portRequired) {
    if (!portRequired.updateAction().isPresent()) {
      return DeploymentHelper.getEmptyPortUpdateHandler();
    }

    //todo this is inconsistent. multiple PortUpdateHandler should be allowed here so
    //todo it is possible to set one per operating system!
    BashBasedHandlerBuilder portUpdateBuilder = new BashBasedHandlerBuilder();
    portUpdateBuilder.setOperatingSystem(OperatingSystem.UBUNTU_14_04);
    portUpdateBuilder.addCommand(portRequired.updateAction().get());
    return portUpdateBuilder.buildPortUpdateHandler();
  }
}