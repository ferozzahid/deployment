package io.github.cloudiator.deployment.lance;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Option;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.OsCommand;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommand.Type;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.DockerCommandException;
import de.uniulm.omi.cloudiator.lance.lifecycle.language.EntireDockerCommands;
import io.github.cloudiator.deployment.domain.DockerInterface;
import io.github.cloudiator.deployment.domain.Job;
import io.github.cloudiator.deployment.domain.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class DockerComponentSupplier {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateLanceProcessStrategy.class);
  protected final Job job;
  protected final Task task;
  protected final DockerInterface dockerInterface;

  static final List<String> supportedDomains = new ArrayList<String>() {{
    add("de");
    add("pl");
    add("gr");
    add("no");
    add("cz");
    add("com");
    add("net");
    add("us");
    add("co.uk");
  }};

  public DockerComponentSupplier(Job job, Task task, DockerInterface dockerInterface) {
    checkNotNull(task, "task is null");
    checkArgument(job.tasks().contains(task), "Task %s is not member of job %s.", task, job);
    this.job = job;
    this.task = task;
    this.dockerInterface = dockerInterface;
  }

  protected EntireDockerCommands deriveEntireCommands() {
    Map<String, String> envMap = dockerInterface.environment();
    EntireDockerCommands.Builder cmdsBuilder = new EntireDockerCommands.Builder();
    try {
      Map<Option, List<String>> createOptions = createOptionMap(envMap);
      List<OsCommand> createOsCommands = createOsCommandList();
      List<String> createArgs = createArgsList();
      cmdsBuilder.setOptions(Type.CREATE, createOptions);
      cmdsBuilder.setCommand(Type.CREATE, createOsCommands);
      cmdsBuilder.setArgs(Type.CREATE, createArgs);

      Map<Option, List<String>> startOptions = startOptionMap();
      cmdsBuilder.setOptions(Type.START, startOptions);
    } catch (DockerCommandException ce) {
      LOGGER.error(
          "Error creating the docker cli commands for the Docker Component of task: " + task
              .name());
    }

    return cmdsBuilder.build();
  }

  protected String getHostName() {
    return getStaticHostName(dockerInterface);
  }

  protected int getPort() {
    String imageName = dockerInterface.dockerImage();
    String[] paths = imageName.split("/");

    if (paths.length == 0) {
      LOGGER.error("No image name set! Docker Component will not be deployed!");
      return -1;
    }

    if (paths.length == 1) {
      return -1;
    }

    List<String> pathList = new ArrayList<>(Arrays.asList(paths));

    return getPortNameFromString(pathList.get(0));
  }

  protected String getImageNameSpace() {
    String imageName = dockerInterface.dockerImage();
    String[] paths = imageName.split("/");

    if (paths.length == 0) {
      LOGGER.error("No image name set! Docker Component will not be deployed!");
      return "";
    }

    if (paths.length == 1) {
      return "";
    }

    List<String> pathList = new ArrayList<>(Arrays.asList(paths));

    if (getHostNameFromString(pathList.get(0)).equals("")) {
      pathList.remove(pathList.size() - 1);
      return buildPath(pathList);
    } else {
      pathList.remove(0);
      pathList.remove(pathList.size() - 1);
      return buildPath(pathList);
    }
  }

  protected String getActualImageName() {
    String imageName = dockerInterface.dockerImage();
    String[] paths = imageName.split("/");

    if (paths.length == 0) {
      LOGGER.error("No image name set! Docker Component will not be deployed!");
      return "";
    }

    List<String> pathList = new ArrayList<>(Arrays.asList(paths));

    return getImageNameNameFromString(pathList.get(pathList.size() - 1));
  }

  protected String getTagName() {
    String imageName = dockerInterface.dockerImage();
    String[] paths = imageName.split("/");

    if (paths.length == 0) {
      LOGGER.error("No image name set! Docker Component will not be deployed!");
      return "";
    }

    List<String> pathList = new ArrayList<>(Arrays.asList(paths));

    return getTagNameFromString(pathList.get(pathList.size() - 1));
  }

  protected DockerCredentials getCredentials() {
    Map<String, String> envMap = dockerInterface.environment();
    String username = "";
    String password = "";
    for (Map.Entry<String, String> entry : envMap.entrySet()) {
      if (entry.getKey().equals("username")) {
        checkDoubleSet("username", username);
        username = entry.getValue();
      }
      if (entry.getKey().equals("password")) {
        checkDoubleSet("password", password);
        password = entry.getValue();
      }
    }

    DockerCredentials creds = new DockerCredentials(username, password);
    return creds;
  }

  protected List<String> getMappedPorts() {
    Map<String, String> envMap = dockerInterface.environment();
    List<String> portsList = new ArrayList<>();
    for (Map.Entry<String, String> entry : envMap.entrySet()) {
      if (entry.getKey().equals("port")) {
        if (checkPortFormat(entry.getValue())) {
          String[] ports = entry.getValue().trim().split(",");
          portsList.addAll(new ArrayList<>(Arrays.asList(ports)));
        }
      }
    }

    return portsList;
  }

  protected String getVolume() {
    String volume = "";
    Map<String, String> envMap = dockerInterface.environment();
    for (Map.Entry<String, String> entry : envMap.entrySet()) {
      if (entry.getKey().equals("volume")) {
        //todo: might check correct format
        volume = entry.getValue().trim();
      }
    }
    return volume;
  }

  private static boolean checkPortFormat(String portsString) {
    String[] ports = portsString.trim().split(",");
    List<String> portsList = new ArrayList<>(Arrays.asList(ports));

    if (portsList.size() == 0) {
      LOGGER.error("Wrong port option format! No port number given.");
      return false;
    }

    for (String port : portsList) {
      String[] hostContPort = port.split(":");
      List<String> hostContPortList = new ArrayList<>(Arrays.asList(hostContPort));

      if (hostContPortList.size() > 2) {
        LOGGER.error("Wrong port option format! Cannot map more than two ports via \':\'");
        return false;
      }

      try {
        Integer.parseInt(hostContPortList.get(0));
      } catch (NumberFormatException ex) {
        LOGGER.error("Host port is not a number!");
        return false;
      }

      if (hostContPortList.size() == 2) {
        try {
          Integer.parseInt(hostContPortList.get(1));
        } catch (NumberFormatException ex) {
          LOGGER.error("Container port is not a number!");
          return false;
        }
      }
    }

    return true;
  }

  public static boolean usePrivateRegistry(DockerInterface dockerIface) {
    final String hostName = getStaticHostName(dockerIface);
    if (hostName.equals("")) {
      return false;
    }
    return true;
  }

  private static String getStaticHostName(DockerInterface dockerIface) {
    String imageName = dockerIface.dockerImage();
    String[] paths = imageName.split("/");

    if (paths.length == 0) {
      LOGGER.error("No image name set! Docker Component will not be deployed!");
      return "";
    }

    if (paths.length == 1) {
      return "";
    }

    List<String> pathList = new ArrayList<>(Arrays.asList(paths));

    return getHostNameFromString(pathList.get(0));
  }

  private static String getHostNameFromString(String name) {
    List<String> splitName = splitStringByColon(name);

    if (splitName.size() == 0) {
      LOGGER.error("Host Name of Docker Registry cannot be derived properly");
    }

    return splitName.get(0);
  }

  private Map<Option, List<String>> createOptionMap(Map<String, String> envMap) {
    Map<Option, List<String>> createOptionMap = new HashMap<>();
    List<String> setEnvVars = new ArrayList<>();
    Map<String, String> actualEnv = getActualEnv(envMap);

    for (Map.Entry<String, String> entry : actualEnv.entrySet()) {
      setEnvVars.add(entry.getKey() + "=" + entry.getValue());
    }
    createOptionMap.put(Option.ENVIRONMENT, setEnvVars);
    createOptionMap.put(Option.RESTART, new ArrayList<>(Arrays.asList("no")));
    createOptionMap.put(Option.INTERACTIVE, new ArrayList<>(Arrays.asList("")));

    List<String> mappedPorts = getMappedPorts();
    if (mappedPorts.size() > 0) {
      createOptionMap.put(Option.PORT, mappedPorts);
    }

    String volume = getVolume();
    if (!volume.equals("")) {
      createOptionMap.put(Option.VOLUME, new ArrayList<>(Arrays.asList(volume)));
    }

    return createOptionMap;
  }

  private static List<OsCommand> createOsCommandList() {
    List<OsCommand> createOsCommandList = new ArrayList<>();
    createOsCommandList.add(OsCommand.BASH);

    return createOsCommandList;
  }

  private static List<String> createArgsList() {
    List<String> createArgsList = new ArrayList<>();
    createArgsList.add("--noediting");

    return createArgsList;
  }

  private static Map<Option, List<String>> startOptionMap() {
    Map<Option, List<String>> startOptionMap = new HashMap<>();
    startOptionMap.put(Option.INTERACTIVE, new ArrayList<>(Arrays.asList("")));

    return startOptionMap;
  }

  private static Map<String, String> getActualEnv(Map<String, String> envMap) {
    Map<String, String> env = new HashMap<>();
    for (Map.Entry<String, String> entry : envMap.entrySet()) {
      if (entry.getKey().equals("username") || entry.getKey().equals("password")
          || entry.getKey().equals("port") || entry.getKey().equals("volume")) {
        continue;
      }
      env.put(entry.getKey(), entry.getValue());
    }

    return env;
  }

  private static void checkDoubleSet(String key, String val) {
    if (!val.equals("")) {
      LOGGER.warn(key + " double set in environment!");
    }
  }

  private int getPortNameFromString(String name) {
    List<String> splitName = splitStringByColon(name);

    if (splitName.size() == 0) {
      LOGGER.error(
          "Host Name of Docker Registry for Task: " + task.name() + "cannot be derived properly");
    }

    if (splitName.size() > 2) {
      LOGGER.warn("Bad Host port syntax for Docker Registry for Task: " + task.name());
    }

    if (splitName.size() > 1) {
      return Integer.parseInt(splitName.get(1));
    }

    return -1;
  }

  private String getImageNameNameFromString(String name) {
    List<String> splitName = new ArrayList<>(Arrays.asList(name.split(":")));

    if (splitName.size() == 0) {
      LOGGER.error("Docker Image Name for Task: " + task.name() + "cannot be derived properly");
    }

    return splitName.get(0);
  }

  private String getTagNameFromString(String name) {
    List<String> splitName = new ArrayList<>(Arrays.asList(name.split(":")));

    if (splitName.size() == 0) {
      LOGGER.error("Docker Image Name for Task: " + task.name() + "cannot be derived properly");
    }

    if (splitName.size() > 2) {
      LOGGER.warn("Bad Docker Image tag syntax for Task: " + task.name());
    }

    if (splitName.size() > 1) {
      return splitName.get(1);
    }

    return "latest";
  }

  private static List<String> splitStringByColon(String name) {
    for (String domain : supportedDomains) {
      if (name.trim().matches(".*\\." + domain + ":?[0-9]*")) {
        String[] parts = name.split(":");
        List<String> partList = new ArrayList<>(Arrays.asList(parts));
        return partList;
      }
    }

    return new ArrayList<>(Arrays.asList(""));
  }

  private static String buildPath(List<String> path) {
    StringBuilder builder = new StringBuilder();

    for (String str : path) {
      builder.append(str + "/");
    }

    //remove last slash
    if (builder.length() > 0) {
      builder.setLength(builder.length() - 1);
    }

    return builder.toString();
  }

  public static class DockerCredentials {

    public final String username;
    public final String password;

    private DockerCredentials(String username, String password) {
      this.username = username;
      this.password = password;
    }

    private boolean useCredentials() {
      return (!username.equals("") && !password.equals(""));
    }
  }
}
