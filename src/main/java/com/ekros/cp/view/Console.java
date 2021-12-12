package com.ekros.cp.view;

import com.ekros.cp.model.Log;
import com.ekros.cp.util.FSUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import lombok.Getter;

public class Console {

  private static boolean isWork = true;

  public static void main(String[] args) {
    start();
  }

  private static void start() {
    Scanner scanner = new Scanner(System.in);
    while (isWork) {
      Command command = input(scanner);
      checkCommand(command);
    }
  }


  private static void checkCommand(Command command) {
    try {
      switch (command.getCommand()) {
        case "exit":
          isWork = false;
          break;
        case "mkfs":
          FSUtils.mkfs(toInt(command.args.get(0)));
          Log.info("Formatted.");
          break;
        case "mount":
          Log.info("Mount status: " + FSUtils.mount());
          break;
        case "unmount":
          Log.info("Unmount status: " + FSUtils.unmount());
          break;
        case "fstat":
          FSUtils.fstat(toInt(command.args.get(0)));
          break;
        case "ls":
          FSUtils.ls();
          break;
        case "create":
          Log.info("Create status: " + FSUtils.create(command.args.get(0)));
          break;
        case "open":
          Log.info("Open status: " + FSUtils.open(command.args.get(0)));
          break;
        case "close":
          FSUtils.close(toInt(command.args.get(0)));
          break;
        case "link":
          FSUtils.link(command.args.get(0), command.args.get(1));
          break;
        case "unlink":
          FSUtils.unlink(command.args.get(0));
          break;
        default:
          break;
      }
    }catch (Exception e){
      Log.error(e.getMessage());
    }
  }

  private static int toInt(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      Log.error(str + " - is not a number");
    }
    return -1;
  }

  private static Command input(Scanner scanner) {
    System.out.print("> ");
    return new Command(scanner.nextLine());
  }

  @Getter
  private static class Command {

    private String command;
    private List<String> args;

    public Command(String command) {
      init(command);
    }

    private void init(String command) {
      command = command.trim();
      if (command.isEmpty()) {
        this.command = "";
        args = new ArrayList<>();
        return;
      }
      String[] strings = command.split(" ");
      this.command = strings[0];
      if (strings.length > 1) {
        args = List.of(strings).subList(1, strings.length);
      } else {
        args = new ArrayList<>();
      }
    }

  }

}