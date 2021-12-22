package com.ekros.cp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ekros.cp.model.Block;
import com.ekros.cp.model.FileSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FSUtilsTest {

  @BeforeEach
  public void beforeEach() {
    FSUtils.clear();
    FSUtils.mount();
  }

  @Test
  public void testClearAndMount() {
    assertTrue(FSUtils.clear());
    assertTrue(FSUtils.mount());
  }

  @Test
  public void testUnmountAndMount() {
    assertTrue(FSUtils.unmount());
    assertTrue(FSUtils.mount());
  }

  @Test
  public void testCreateFiles() {
    assertTrue(FSUtils.create("test"));
    assertFalse(FSUtils.create("test"));
    assertTrue(FSUtils.create("test2"));
    assertFalse(FSUtils.create("test2"));
    for (int i = 3; i <= FileSystem.MAX_DESCRIPTORS; i++) {
      assertTrue(FSUtils.create("test" + i));
    }
    assertFalse(FSUtils.create("withMaxDescription"));
  }

  @Test
  public void testCreateFileIfBlocksFull(){
    FSUtils.create("test");
    assertTrue(FSUtils.truncate("test", FileSystem.MAX_BLOCKS * Block.MAX_BLOCK_SIZE));
    assertFalse(FSUtils.create("test2"));
  }

  @Test
  public void testCreateFileWithIncorrectName(){
    assertFalse(FSUtils.create(null));
    assertFalse(FSUtils.create("test".repeat(FileSystem.MAX_FILE_NAME_LENGTH)));
  }

  @Test
  public void testGetLinksInfo() {
    String out1 = FSUtils.ls();
    FSUtils.create("test");
    String out2 = FSUtils.ls();
    assertNotEquals(out1, out2);
  }

  @Test
  public void testFstat() {
    String out1 = FSUtils.fstat(0);
    FSUtils.create("test");
    String out2 = FSUtils.fstat(0);
    assertNotEquals(out1, out2);
  }

  @Test
  public void testLinks() {
    assertFalse(FSUtils.link("test", "test2"));
    FSUtils.create("test");
    assertFalse(FSUtils.link("test", "test"));
    FSUtils.create("test2");
    assertFalse(FSUtils.link("test", "test2"));
    assertTrue(FSUtils.link("test2", "test3"));
    assertTrue(FSUtils.unlink("test2"));
    assertTrue(FSUtils.unlink("test3"));
    assertFalse(FSUtils.unlink("test3"));
  }

  @Test
  public void testMkfs() {
    FSUtils.create("test");
    FSUtils.create("test2");
    FSUtils.create("test3");
    FSUtils.create("test4");
    FSUtils.mkfs();
    assertEquals(0, FSUtils.fileSystem.getDescriptors().size());
  }

  @Test
  public void testOpenClose() {
    assertFalse(FSUtils.open("test"));
    assertFalse(FSUtils.close(0));
    FSUtils.create("test");
    FSUtils.create("test2");
    FSUtils.link("test", "test3");
    assertTrue(FSUtils.open("test"));
    assertTrue(FSUtils.open("test2"));
    assertTrue(FSUtils.close(1));
    assertTrue(FSUtils.close(0));
    assertTrue(FSUtils.open("test3"));
    assertTrue(FSUtils.open("test"));
    assertTrue(FSUtils.close(0));
  }

  @Test
  public void testReadAndWrite() {
    FSUtils.create("test");
    assertEquals("", FSUtils.read(0, 1, 5));
    FSUtils.open("test");
    assertEquals("00000000", FSUtils.read(0, 0, 8));
    assertTrue(FSUtils.write(0, 3, 3));
    assertEquals("01110", FSUtils.read(0, 2, 5));
  }

  @Test
  public void testReadAndWriteIncorrectFd(){
    FSUtils.create("test");
    FSUtils.open("test");
    assertEquals("", FSUtils.read(999, 0, 5));
    assertFalse(FSUtils.write(999, 0, 5));
    assertEquals("", FSUtils.read(-5, 0, 5));
    assertFalse(FSUtils.write(-5, 0, 5));

  }

  @Test
  public void testIncorrectReadSize() {
    FSUtils.create("test");
    FSUtils.open("test");
    assertEquals("", FSUtils.read(0, 2, 100));
    assertEquals("", FSUtils.read(0, -2, -4));
    assertEquals("", FSUtils.read(0, 2, 0));
  }

  @Test
  public void testIncorrectWriteSize() {
    FSUtils.create("test");
    FSUtils.open("test");
    assertFalse(FSUtils.write(0, 2, 100));
    assertFalse(FSUtils.write(0, -2, -4));
    assertFalse(FSUtils.write(0, 2, 0));
  }

  @Test
  public void testWorkWithUnlinkedFile() {
    FSUtils.create("test");
    FSUtils.open("test");
    FSUtils.write(0, 2, 5);
    FSUtils.unlink("test");
    assertEquals("11111", FSUtils.read(0, 2, 5));
    FSUtils.close(0);
    assertFalse(FSUtils.open("test"));
  }

  @Test
  public void testTruncateAndWriteMultiBlocks() {
    FSUtils.create("test");
    FSUtils.create("test2");
    FSUtils.link("test", "test2");
    FSUtils.open("test2");
    assertFalse(FSUtils.truncate("test", 9999));
    assertFalse(FSUtils.truncate("test", -2));
    assertFalse(FSUtils.truncate("test", (FileSystem.MAX_BLOCKS)*Block.MAX_BLOCK_SIZE));
    assertTrue(FSUtils.truncate("test2", 20));
    assertTrue(FSUtils.truncate("test2",10));
    assertTrue(FSUtils.truncate("test2", 20));
    assertEquals("0".repeat(24), FSUtils.read(0, 0, 24));
    assertTrue(FSUtils.write(0, 10, 10));
    assertEquals("011111111110", FSUtils.read(0, 9, 12));
    assertTrue(FSUtils.truncate("test", 12));
    assertEquals("0111111", FSUtils.read(0, 9, 7));
  }
}