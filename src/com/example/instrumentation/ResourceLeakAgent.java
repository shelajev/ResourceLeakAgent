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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;

import com.example.instrumentation.util.IOUtil;

/**
 * Premain class that redefines FileInput/OutputStreams to print where
 * they were created if they are not closed at GC time. 
 *  
 * @author Lauri Tulmin (lauri@zeroturnaround.com), Oleg Shelajev (oleg@zeroturnaround.com) 
 */
public class ResourceLeakAgent {

  public static void premain(String agentArgument, Instrumentation instrumentation) {
    try {
      ClassDefinition[] classDefinitions = getClassDefinitions(
          FileInputStream.class,
          FileOutputStream.class,
          ZipFile.class
          );
      instrumentation.redefineClasses(classDefinitions);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static ClassDefinition[] getClassDefinitions(Class<?>... classes) throws IOException, CannotCompileException,
      NotFoundException {
    List<ClassDefinition> defs = new ArrayList<ClassDefinition>();
    for (Class<?> clazz : classes) {
      String classFile = clazz.getName().replace('.', '/') + ".class";
      InputStream in = ClassLoader.getSystemResourceAsStream(classFile);
      if (in == null)
        continue;
      byte[] bytes = IOUtil.getBytesAndClose(new BufferedInputStream(in));
      defs.add(new ClassDefinition(clazz, transformClassBytecode(clazz.getName(), bytes)));
    }
    return defs.toArray(new ClassDefinition[0]);
  }

  private static byte[] transformClassBytecode(String classname, byte[] bytes) throws IOException, CannotCompileException,
      NotFoundException {
    ClassPool cp = ClassPool.getDefault();
    cp.appendClassPath(new ByteArrayClassPath(classname, bytes));
    CtClass cc = cp.get(classname);
    cc.defrost();
    process(cp, cc);
    return cc.toBytecode();
  }

  private static void process(ClassPool cp, CtClass ctClass) throws CannotCompileException, NotFoundException {
    String className = ctClass.getName();
    CtConstructor[] constructors = ctClass.getDeclaredConstructors();
    for (int i = 0; i < constructors.length; i++) {
      CtConstructor c = constructors[i];
      if (c.callsSuper()) {
        String details = null;
        if ((FileInputStream.class.getName().equals(className) || FileOutputStream.class.getName().equals(className))
            && c.getParameterTypes().length > 0 && File.class.getName().equals(c.getParameterTypes()[0].getName())) {
          details = "\" (for file '\" + $1.getAbsolutePath() + \"')\"";
        }
        else if (ZipFile.class.getName().equals(className) && c.getParameterTypes().length > 0
            && File.class.getName().equals(c.getParameterTypes()[0].getName())) {
          details = "\" (for zip file '\" + $1.getAbsolutePath() + \"')\"";
        }

        c.insertAfter(
            "{" +
                "  String msg = \"" + ctClass.getSimpleName() + "\";"
                + (details == null ? "" : "msg += " + details + ";") +
                "  msg += \" not closed, initialized from:\";" +
                "  " + ResourceLeakManager.class.getName() + ".register(this, msg, new Throwable());" +
                "}"
            );
      }
    }

    ctClass.getDeclaredMethod("close").insertAfter(
        "{" +
            "  " + ResourceLeakManager.class.getName() + ".handleClose(this);" +
            "}"
        );
  }
}
