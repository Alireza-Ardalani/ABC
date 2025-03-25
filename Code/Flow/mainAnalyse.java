package org.example;

import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.util.Chain;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm.FlowSensitive;
import static soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode.AllImplicitFlows;
import static soot.jimple.infoflow.InfoflowConfiguration.PathBuildingAlgorithm.ContextSensitive;
import static soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode.Precise;


public class mainAnalyse {

    public static void main(String[] args) throws XmlPullParserException, IOException, InterruptedException {
        String outputFolder = args[0];
        String resultFolder = args[1];
        String IOSignatures = args[2];
        String apkPath = args[3];
        String name = args[4];
        String jarFile = args[5];
        JADXHandler jadxHandler = new JADXHandler(apkPath);
        Set<String> packageNames = new HashSet<>();
        String packageName = jadxHandler.findPackageName();
        if(!packageName.isEmpty()){
            packageNames.add(packageName);
        }
        Set<String> packageNameFromActivities = jadxHandler.findPackageNameFromActivities();
        packageNames.addAll(packageNameFromActivities);
        SetupApplication setupApplication = setupApplication(apkPath, jarFile);
        InfoflowResults results = setupApplication.runInfoflow(IOSignatures);
        CallGraph CFG = Scene.v().getCallGraph();
        InfoflowCFG InfCFG = new InfoflowCFG();
        System.out.println(CFG.size());
        Set<Stmt> targets = setupApplication.getCollectedSources();
        System.out.println("SizeOfTheTarget=> " + targets.size());
        Set<SootMethod> targetMethods = new HashSet<>();
        Set<SootMethod> nonDummyEntryPoints;
        nonDummyEntryPoints = entryPointsOfApp(CFG,packageNames);
        if (!(targets.isEmpty())) {
            for (Stmt stmt : targets) {
                SootMethod targetMethod = stmt.getInvokeExpr().getMethod();
                targetMethods.add(targetMethod);
            }
            System.out.println("Entry===>  " + nonDummyEntryPoints.size());
            System.out.println("Target===> " + targetMethods.size());
            System.out.println("-------");
            for(SootMethod x : targetMethods){
                System.out.println(x);
            }
            System.out.println("-------");
            Analyse analyse = new Analyse(InfCFG, nonDummyEntryPoints, targets, name, outputFolder, resultFolder, apkPath, packageNames);

        } else {
            System.out.println("Alireza: There is not any Target method!!");
            moveAPK(apkPath,outputFolder);
            FileWriter w = new FileWriter(resultFolder + "/" + "Issue.txt",true);
            w.write(name + " -*-> " + "NoIOMethodINThisApp" + "\n");
            w.close();
        }
    }

    static void allMethodInAPK(String apkPath, CallGraph callGraph, String name, String resultFolder, String outputFolder) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        Set<SootMethod> set = new HashSet<SootMethod>();
        Iterator<Edge> edges = callGraph.iterator();
        System.out.println("SizE==> " + callGraph.size());
        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod source;
            SootMethod destination;
            if (edge.src() != null) {
                source = edge.getSrc().method();
                set.add(source);
            }
            if(edge.tgt() != null){
                destination = edge.getTgt().method();
                set.add(destination);
            }

        }
        for (SootMethod m : set) {
            Iterator<Edge> occurrence = callGraph.edgesInto(m);
            int i = 0;
            while (occurrence.hasNext()) {
                occurrence.next();
                i++;
            }
            map.put(m.getSignature(), i);
        }
        FileWriter fileWriter = new FileWriter(resultFolder + "/" + name + ".txt", true);
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    try {
                        fileWriter.write(entry.getKey() + " ---> " + entry.getValue() + "\n");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        fileWriter.close();
        //moveAPK(apkPath, outputFolder);
    }
    private static Set<SootMethod> entryPointsOfApp(CallGraph callGraph,Set<String> packageNames){
        List<String> lifeCycle = Arrays.asList(
                "afterTextChanged",
                "beforeTextChanged",
                "onActivityCreated",
                "onActivityResult",
                "onAttach",
                "onAttachedToWindow",
                "onBackPressed",
                "onCancel",
                "onConfigurationChanged",
                "onContextItemSelected",
                "onContextMenuClosed",
                "onCreate",
                "onCreateApplication",
                "onCreateContextMenu",
                "onCreateDialog",
                "onCreateOptionsMenu",
                "onCreateView",
                "onDestroy",
                "onDestroyView",
                "onDetach",
                "onDetachedFromWindow",
                "onDismiss",
                "onDraw",
                "onEditorAction",
                "onFling",
                "onItemClick",
                "onItemLongClick",
                "onLayout",
                "onLongPress",
                "onMeasure",
                "onNewIntent",
                "onOptionsItemSelected",
                "onPause",
                "onPostCreate",
                "onPostResume",
                "onPrepareOptionsMenu",
                "onReceive",
                "onRequestPermissionsResult",
                "onRestart",
                "onRestoreInstanceState",
                "onResume",
                "onResumeFragments",
                "onSaveInstanceState",
                "onScroll",
                "onScrollStateChanged",
                "onScrolled",
                "onShowPress",
                "onSingleTapUp",
                "onSizeChanged",
                "onStart",
                "onStop",
                "onTextChanged",
                "onUserInteraction",
                "onUserLeaveHint",
                "onViewCreated",
                "onViewStateRestored",
                "onWindowFocusChanged",
                "onWindowVisibilityChanged",
                "onCreatePanelMenu"
        );
        Set<SootMethod> methods = new HashSet<>();
        Iterator<Edge> edges = callGraph.iterator();
        while (edges.hasNext()) {
            Edge edge = edges.next();
            if(edge.src()!=null){
                SootMethod method = edge.getSrc().method();
                String sig = method.getSignature();
                boolean packageMatch = packageNames.stream().anyMatch(sig::contains);
                boolean lifeCycleMatch = lifeCycle.stream().anyMatch(sig::contains);
                if(packageMatch && lifeCycleMatch){
                    methods.add(method);
                }
            }
        }
        return  methods;
    }

    public static SetupApplication setupApplication(String apkFile, String jarFile) {
        SetupApplication setupApplication = new SetupApplication(jarFile, apkFile);
        IInfoflowConfig configSoot = new IInfoflowConfig() {
            @Override
            public void setSootOptions(Options options, InfoflowConfiguration config) {
                Options.v().set_whole_program(true);
                Options.v().set_allow_phantom_refs(false);
                Options.v().set_no_bodies_for_excluded(true);
                Options.v().set_process_multiple_dex(true);
                Options.v().set_include_all(true);
            }
        };
        setupApplication.setSootConfig(configSoot);

        InfoflowAndroidConfiguration config = setupApplication.getConfig();
        config.setMergeDexFiles(true);
        config.setAliasingAlgorithm(FlowSensitive);
        config.getSolverConfiguration().setDataFlowSolver(InfoflowConfiguration.DataFlowSolver.ContextFlowSensitive);
        //config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.CHA);
        config.getSolverConfiguration().setSparsePropagationStrategy(InfoflowConfiguration.SparsePropagationStrategy.Precise);
        config.setDataFlowDirection(InfoflowConfiguration.DataFlowDirection.Backwards);
        config.setImplicitFlowMode(AllImplicitFlows);
        config.getPathConfiguration().setPathReconstructionMode(Precise);
        config.getPathConfiguration().setPathBuildingAlgorithm(ContextSensitive);
        config.setMemoryThreshold(1.0d);
        config.setDataFlowTimeout(60);
        config.getPathConfiguration().setPathReconstructionTimeout(60);
        config.getPathConfiguration().setPathReconstructionBatchSize(10);
        config.getPathConfiguration().setMaxPathLength(100);
        config.setLogSourcesAndSinks(true);
        config.getAccessPathConfiguration().setAccessPathLength(5);
        config.getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
        config.getAccessPathConfiguration().setUseThisChainReduction(false);
        return setupApplication;
    }

    private static void moveAPK(String apkPath, String finishFolder) {
        File sourceFile = new File(apkPath);
        File destinationDirectory = new File(finishFolder);

        if (!sourceFile.exists() || !destinationDirectory.exists()) {
            System.err.println("Source file or destination directory does not exist.");
        }
        try {
            Path sourcePath = sourceFile.toPath();
            Path destinationPath = new File(destinationDirectory, sourceFile.getName()).toPath();

            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File moved successfully!");
        } catch (IOException e) {
            System.err.println("Error moving file: " + e.getMessage());
        }
    }
}