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
package com.example.instrumentation.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Dumb IO util to copy streams
 * @author shelajev
 *
 */
public class IOUtil {
  private static final int BUFFER_SIZE = 4096;

  public static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int read;

    while ((read = in.read(buffer)) != -1) {
      out.write(buffer, 0, read);
    }
  }

  public static byte[] getBytes(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(in, out);
    return out.toByteArray();
  }

  public static byte[] getBytesAndClose(InputStream in) {
    try {
      return getBytes(in);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      try {
        in.close();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
