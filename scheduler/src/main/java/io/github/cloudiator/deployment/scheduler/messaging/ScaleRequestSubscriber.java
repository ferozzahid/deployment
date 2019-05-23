package io.github.cloudiator.deployment.scheduler.messaging;

import com.google.inject.Inject;
import io.github.cloudiator.deployment.domain.Job;
import io.github.cloudiator.deployment.domain.Schedule;
import io.github.cloudiator.deployment.domain.Task;
import io.github.cloudiator.deployment.messaging.JobMessageRepository;
import io.github.cloudiator.deployment.messaging.ScheduleMessageRepository;
import io.github.cloudiator.deployment.scheduler.scaling.ScalingEngine;
import io.github.cloudiator.domain.Node;
import io.github.cloudiator.messaging.NodeToNodeMessageConverter;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.cloudiator.messages.General.Error;
import org.cloudiator.messages.Process.ScaleResponse;
import org.cloudiator.messaging.MessageInterface;
import org.cloudiator.messaging.services.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Daniel Seybold on 23.05.2019.
 */
public class ScaleRequestSubscriber implements Runnable {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ScaleRequestSubscriber.class);


  private final ProcessService processService;
  private final MessageInterface messageInterface;

  private final ScalingEngine scalingEngine;

  private final ScheduleMessageRepository scheduleMessageRepository;
  private final JobMessageRepository jobMessageRepository;

  private static final NodeToNodeMessageConverter NODE_MESSAGE_CONVERTER = NodeToNodeMessageConverter.INSTANCE;

  @Inject
  public ScaleRequestSubscriber(
      ProcessService processService,
      MessageInterface messageInterface,
      ScheduleMessageRepository scheduleMessageRepository,
      JobMessageRepository jobMessageRepository,
      ScalingEngine scalingEngine) {
    this.processService = processService;
    this.messageInterface = messageInterface;
    this.scheduleMessageRepository = scheduleMessageRepository;
    this.jobMessageRepository = jobMessageRepository;
    this.scalingEngine = scalingEngine;


  }

  @Override
  public void run() {


    processService.subscribeScaleRequest(
        (id, content) -> {

          try {

            final String userId = content.getUserId();
            final String scheduleId = content.getScheduleId();
            final String taskId = content.getTaskId();
            Schedule schedule = scheduleMessageRepository
                .getById(content.getUserId(), scheduleId);


            Job job = jobMessageRepository
                .getById(userId, schedule.job());

            Task task = job
                .getTask(taskId).get();

            ArrayList<Node> coll = content.getNodes().getNodesList().stream()
                .map(NODE_MESSAGE_CONVERTER::applyBack).collect(
                    Collectors.toCollection(ArrayList::new));

            scalingEngine.scale(schedule,job,task,coll);

            final ScaleResponse scaleResponse = ScaleResponse.newBuilder().build();

            messageInterface.reply(id,scaleResponse);

          } catch (Exception e) {
            final String errorMessage = String
                .format("Exception %s while processing request %s with id %s.", e.getMessage(),
                    content, id);

            LOGGER.error(errorMessage, e);

            messageInterface.reply(ScaleResponse.class, id,
                Error.newBuilder().setMessage(errorMessage).setCode(500).build());

          }


        });
  }

}