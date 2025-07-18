package eventBasedSystem;

import driver.PTAFactory;
import driver.PTAOption;
import org.xmlpull.v1.XmlPullParserException;
import qilin.DataClass;
import qilin.core.PTA;
import qilin.core.pag.PAG;
import qilin.pta.PTAConfig;
import qilin.stat.SimplifiedEvaluator;

import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

;

public class RunAndroid {
    static String androidJarFilesPath;
    static String apkFilePath;
    static String resultFolder;
    static String inputFolder;
    static String mode;
    static String name;

public static void main(String[] args) throws IOException, XmlPullParserException {
    System.out.println("Min Heap Size: " + (Runtime.getRuntime().totalMemory() / (1024 * 1024)) + " MB");
    System.out.println("Max Heap Size: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
    mode = args[0];
    inputFolder =  args[1];
    name = args[2];
    apkFilePath = inputFolder + File.separator + name;
    resultFolder = args[3];
    androidJarFilesPath =  args[4];

    androidRun();
}

    private static void androidRun() throws IOException, XmlPullParserException {
        AndroidLoader androidLoader = new AndroidLoader(apkFilePath, androidJarFilesPath);
        AndroidLoader.resolveAnonymousClass();
        DataClass.setThreads(androidLoader.runThreadDetection());
        DataClass.setBytecodeOrigins(androidLoader.getByteCodeOrigin());
        androidLoader.loadEntryPoints();
        runPTA();

    }
    //Thread

    private static void runPTA() throws IOException {
        PTA pta;
        long startTime = System.nanoTime();
        if(mode.equals("1-CFA")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "1c"});
        } else if(mode.equals("1-OBJ")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "1o"});
        } else if (mode.equals("2-CFA")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "2c"});
        } else if(mode.equals("2-OBJ")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "2o"});
        } else if(mode.equals("1-CFA-B")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "1c", "-offCtx", "Bytecode",});
        } else if(mode.equals("1-CFA-C")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "1c", "-offCtx", "Component",});
        } else if(mode.equals("1-CFA-T")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "1c", "-offCtx", "Thread",});
        } else if(mode.equals("2-CFA-B")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "2c", "-offCtx", "Bytecode",});
        } else if(mode.equals("2-CFA-C")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "2c", "-offCtx", "Component",});
        } else if(mode.equals("2-CFA-T")){
            new PTAOption().parseCommandLine(new String[]{"-se", "-pta", "2c", "-offCtx", "Thread",});
        }


        pta = PTAFactory.createPTA(PTAConfig.v().getPtaConfig().ptaPattern);
        pta.run();
        long endTime = System.nanoTime();
        double elapsedTimeInSeconds = (endTime - startTime) / 1_000_000_000.0;
        PAG pag = pta.getPag();

        int methodContextNumber = 0;
        Map<SootMethod, Map<Context, MethodOrMethodContext>> contextMethodMap = pag.getContextMethodMap();
        for (SootMethod key : contextMethodMap.keySet()) {
            methodContextNumber = methodContextNumber + contextMethodMap.get(key).size();
        }

        System.out.println("Alloc_Size===> " + pag.getAlloc().size());
        System.out.println("Size===> " + methodContextNumber);

        FileWriter fw = new FileWriter(resultFolder + "/" + name + ".txt", true);
        fw.write("PTA_Mode: " + mode + "\n");
        fw.write("Allocation: " + pag.getAlloc().size() + "\n");
        fw.write("Method: " + methodContextNumber + "\n");
        fw.write("Time: " + elapsedTimeInSeconds + "\n");
        fw.write("------------------------------------\n");
        fw.close();
    }




































//    public static void main(String[] args) throws IOException, XmlPullParserException {
//        System.out.println("Min Heap Size: " + (Runtime.getRuntime().totalMemory() / (1024 * 1024)) + " MB");
//        System.out.println("Max Heap Size: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
//
////        apkFilePath = "/Users/alirezaardalani/Desktop/NewWorkerThread/1-NewThread/APKs/declaredThenStartedNamedRunnable.apk";
////        apkFilePath = "/Users/alirezaardalani/Desktop/NewWorkerThread/2-concurrentExecutors/APK1s/executorSubmitCallable.apk";
////        apkFilePath = "/Users/alirezaardalani/Desktop/K9-debug.apk";
////        androidJarFilesPath = "/Users/alirezaardalani/Desktop/platforms/";
//
////        ptaBottom = "1c";
//////        ptaTop = "0";
//
//        androidRun();
//    }
//
//    private static void androidRun() throws IOException, XmlPullParserException {
//        AndroidLoader androidLoader = new AndroidLoader(apkFilePath, androidJarFilesPath);
//        AndroidLoader.resolveAnonymousClass();
//        DataClass.setThreads(androidLoader.runThreadDetection());
//        DataClass.setComponents(androidLoader.getComponent());
//        DataClass.setBytecodeOrigins(androidLoader.getByteCodeOrigin());
//        androidLoader.loadEntryPoints();
//        runPTA();
//    }
//    //Thread
//
//    private static void runPTA() throws IOException {
//        PTA pta;
//        long startTime = System.nanoTime();
//        new PTAOption().parseCommandLine(new String[]{
//                "-se",
//                "-offCtx", "Component",
//                "-pta", ptaBottom,
////                "-top", ptaTop,
//
//        });
//        pta = PTAFactory.createPTA(PTAConfig.v().getPtaConfig().ptaPattern);
//        pta.run();
//        long endTime = System.nanoTime();
//        double elapsedTimeInSeconds = (endTime - startTime) / 1_000_000_000.0;
//
//        SimplifiedEvaluator evaluator = new SimplifiedEvaluator(pta);
//        //System.out.println(evaluator.export);
//
//
//        CallGraph callGraph = Scene.v().getCallGraph();
//
////        for(var edge: callGraph){
////            System.out.println(edge.kind()+" - "+edge.src() +" ==> "+ edge.tgt());
////        }
//
//        PAG pag = pta.getPag();
//        System.out.println();
//
//        int methodContextNumber = 0;
//        Map<SootMethod, Map<Context, MethodOrMethodContext>> contextMethodMap = pag.getContextMethodMap();
//        for (SootMethod key : contextMethodMap.keySet()) {
//            methodContextNumber = methodContextNumber + contextMethodMap.get(key).size();
//        }
//
//        System.out.println("Alloc_Size===> " + pag.getAlloc().size());
//        System.out.println("Size===> " + methodContextNumber);
//
//
//
//    }



}
