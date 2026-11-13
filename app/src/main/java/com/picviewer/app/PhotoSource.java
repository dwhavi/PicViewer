package com.picviewer.app;
import com.picviewer.core.Scanner;
import java.io.File;

interface PhotoSource extends Scanner.Source, AutoCloseable {
 void fetch(String path,File target,long max,long knownSize,long knownModified)throws Exception;
 void close();
}
