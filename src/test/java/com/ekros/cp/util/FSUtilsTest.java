package com.ekros.cp.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FSUtilsTest {

  @BeforeEach
  public void beforeEach(){
    FSUtils.clear();
    FSUtils.mount();
  }

  @Test
  public void testClearAndMount(){
    assertTrue(FSUtils.clear());
    assertTrue(FSUtils.mount());
  }

  @Test
  public void testUnmountAndMount(){
    assertTrue(FSUtils.unmount());
    assertTrue(FSUtils.mount());
  }

  @Test
  public void testCreateFiles(){
    assertTrue(FSUtils.create("test"));
    assertFalse(FSUtils.create("test"));
    assertTrue(FSUtils.create("test2"));
    assertFalse(FSUtils.create("test2"));
  }

  @Test
  public void testLinks(){
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
  public void testMkfs(){
    FSUtils.create("test");
    FSUtils.create("test2");
    FSUtils.create("test3");
    FSUtils.create("test4");
    FSUtils.mkfs(2);
    assertEquals(2, FSUtils.fileSystem.getFileDescriptors().size());
    assertTrue(FSUtils.create("test"));
    assertTrue(FSUtils.create("test2"));
    FSUtils.mkfs(0);
    assertEquals(4, FSUtils.fileSystem.getFileDescriptors().size());
    FSUtils.mkfs(4);
    assertEquals(0, FSUtils.fileSystem.getFileDescriptors().size());
  }

  @Test
  public void testOpenClose(){
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
  public void testReadAndWrite(){
    FSUtils.create("test");
    assertEquals("", FSUtils.read(0, 1, 5));
    FSUtils.open("test");
    assertEquals("00000000", FSUtils.read(0, 0, 8));
    assertTrue(FSUtils.write(0, 3, 3));
    assertEquals("01110", FSUtils.read(0, 2, 5));
  }

  @Test
  public void testTruncateAndWriteMultiBlocks() {
    FSUtils.create("test");
    FSUtils.link("test", "test2");
    FSUtils.open("test2");
    assertTrue(FSUtils.truncate("test2", 20));
    assertEquals("0".repeat(24), FSUtils.read(0, 0, 24));
    assertTrue(FSUtils.write(0, 10, 10));
    assertEquals("011111111110", FSUtils.read(0, 9, 12));
  }
}