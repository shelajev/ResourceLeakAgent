ResourceLeakAgent
=================

A small demo project that build a javaagent to instrument FileInputStream/FileOutputStrem so they report itself if they are not closed at a GC time.