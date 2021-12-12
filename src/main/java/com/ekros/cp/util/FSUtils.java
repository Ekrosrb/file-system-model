package com.ekros.cp.util;

import com.ekros.cp.model.FileSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FSUtils {

  public static FileSystem fileSystem;

  public static void mkfs(int n){
    fileSystem.format(n);
    update();
  }

  public static boolean mount() {
    fileSystem = deserialize(OsUtils.readFs());
    return !Objects.isNull(fileSystem);
  }

  public static boolean unmount() {
    update();
    fileSystem = null;
    return true;
  }

  public static boolean clear(){
    fileSystem = null;
    return OsUtils.clear();
  }

  public static boolean open(String name){
    boolean status = fileSystem.openFile(name);
    if(!status){
      Log.error("No such file [" + name + "].");
    }
    return status;
  }

  public static boolean close(int fd) {
    boolean status = fileSystem.closeFile(fd);
    if(status){
      Log.info("File with fd [" + fd + "] closed.");
    }else{
      Log.error("File with fd [" + fd + "] not found.");
    }
    return status;
  }

  public static String read(int fd, int offset, int size){
    String data = fileSystem.read(fd, offset, size);
    Log.info(data);
    return data;
  }

  public static boolean write(int fd, int offset, int size){
    boolean status = fileSystem.write(fd, offset, size);
    if(status){
      Log.info("Write status: " + true);
    }else{
      Log.error("Write status: " + false);
    }
    update();
    return status;
  }

  public static boolean truncate(String name, int size){
    boolean status = fileSystem.truncate(name, size);
    if(status){
      Log.info("Truncated [" + name + "] to " + size + " size.");
    }else{
      Log.error("Truncate failed.");
    }
    update();
    return status;
  }

  public static boolean link(String name1, String name2){
    boolean status = fileSystem.link(name1, name2);
    if(status){
      Log.info("Link [" + name1 + "] added.");
    }else{
      Log.error("Link [" + name2 + "] not found.");
    }
    update();
    return status;
  }

  public static boolean unlink(String name){
    boolean status = fileSystem.unlink(name);
    if(status){
      Log.info("Link [" + name + "] removed.");
    }else{
      Log.error("Link [" + name + "] not found.");
    }
    update();
    return status;
  }

  public static String fstat(int id){
    if(!fileSystem.getFileDescriptors().containsKey(id)){
      return "Descriptor " + id + " not found.";
    }
    return fileSystem.getFileDescriptors().get(id).toString();
  }

  public static void ls(){
    Log.info(fileSystem.getLinksInfo());
  }

  public static boolean create(String name){
    boolean status = fileSystem.addFile(name);
    update();
    return status;
  }

  private static void update() {
    if(fileSystem != null) {
      OsUtils.updateFs(serialize(fileSystem));
    }
  }

  private static byte[] serialize(FileSystem fileSystem) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ObjectOutputStream os = new ObjectOutputStream(out)) {
      os.writeObject(fileSystem);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return out.toByteArray();
  }

  private static FileSystem deserialize(byte[] data) {
    ByteArrayInputStream in = new ByteArrayInputStream(data);
    try (ObjectInputStream is = new ObjectInputStream(in)) {
      return (FileSystem) is.readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }
}
