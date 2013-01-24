/**
 *   Copyright [2013] [ZeroTurnaround]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.example.instrumentation.main;
import java.io.FileInputStream;
import java.io.IOException;

public class MainTest {
  public static void main(String[] args) throws InterruptedException, IOException {
    leakFileInputStream();
    System.out.println("Waiting for GC to collect that inputStream ");
    Runtime.getRuntime().gc();
    Thread.sleep(5000);
    System.out.println("That's all folks :)");
  }

  private static void leakFileInputStream() throws IOException {
    String filename = "/etc/hosts";
    // No! No-no-no! Only for science!
    FileInputStream is = new FileInputStream(filename);
    is.read();
  }
}
