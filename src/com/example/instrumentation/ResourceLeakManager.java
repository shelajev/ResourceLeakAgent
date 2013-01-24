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

package com.example.instrumentation;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Manages references to resources.
 *   
 * @author Lauri Tulmin (lauri@zeroturnaround.com), Oleg Shelajev (oleg@zeroturnaround.com) 
 */
public class ResourceLeakManager {

  private static final Map<ResourceRef, LeakInfo> map = Collections.<ResourceRef, LeakInfo> synchronizedMap(new HashMap<ResourceRef, LeakInfo>());

  public static void register(Object obj, String msg, Throwable context) {
    map.put(new ResourceRef(obj, true), new LeakInfo(msg, context));
  }

  public static void handleClose(Object obj) {
    map.remove(new ResourceRef(obj));
  }

  private static void removeAndReport(ResourceRef r) {
    LeakInfo li = (LeakInfo) map.remove(r);
    if (li != null) {
      System.out.println(li.msg);
      for (StackTraceElement element : li.context) {
        System.out.println("\t" + element);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static class ResourceRef extends WeakReference {
    private static final ReferenceQueue rq = new ReferenceQueue();

    static {
      final Runnable r = new Runnable() {
        public void run() {
          try {
            while (true) {
              ResourceRef ref = (ResourceRef) rq.remove();
              removeAndReport(ref);
            }
          }
          catch (InterruptedException e) {
          }
        }
      };
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run() {
          new Thread(r) {
            {
              setDaemon(true);
              setContextClassLoader(null);
            }
          }.start();
          return null;
        }
      });
    }

    private final int hash;

    public ResourceRef(Object referent) {
      this(referent, false);
    }

    public ResourceRef(Object referent, boolean queue) {
      super(referent, queue ? rq : null);
      this.hash = referent.hashCode();
    }

    public int hashCode() {
      return hash;
    }

    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o instanceof ResourceRef) {
        return this.get() != null && this.get() == ((ResourceRef) o).get();
      }
      return false;
    }
  }

  private static class LeakInfo {
    final String msg;
    final StackTraceElement[] context;

    LeakInfo(String msg, Throwable context) {
      this.msg = msg;
      this.context = context.getStackTrace();
    }
  }
}
