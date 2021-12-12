package com.ekros.cp.util;

import com.ekros.cp.model.FileSystem;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class OsUtils {

  private static final String path = System.getProperty("user.dir") + "/filesystem";

  private OsUtils(){}

  public static byte[] readFs(){
      if(Files.notExists(Path.of(path))){
        createFs();
      }
      try (FileInputStream is = new FileInputStream(path)) {
        return is.readAllBytes();
      } catch (IOException e) {
        e.printStackTrace();
      }

    return new byte[0];
  }

  public static void updateFs(byte[] fs){
    try (FileOutputStream fos = new FileOutputStream(path)) {
      fos.write(fs);
      fos.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void createFs(){
    FileSystem fileSystem = new FileSystem();
    try {
      Files.createFile(Path.of(path));
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
      oos.writeObject(fileSystem);
      oos.flush();
      oos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static boolean clear() {
    try {
      return Files.deleteIfExists(Path.of(path));
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
}
