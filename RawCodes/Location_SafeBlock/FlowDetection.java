import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.IAndroidComponent;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm.FlowSensitive;
import static soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode.NoCodeElimination;
import static soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode.AllImplicitFlows;
import static soot.jimple.infoflow.InfoflowConfiguration.PathBuildingAlgorithm.*;
import static soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode.Precise;

public class FlowDetection {
    public SetupApplication setupApplication;
    public InfoflowResults results;
    public InfoflowCFG iCFG;
    public CallGraph graph;

    Map<String, String> allComponents;

    public  FlowDetection(String apkFile, String jarFile , String sinkSourceFile , String Mode ) throws XmlPullParserException, IOException, URISyntaxException {
        if(Mode.equals("FAST")){
            this.setupApplication = setupApplicationBuilderFAST(apkFile, jarFile);
        } else if (Mode.equals("POWER")) {
            this.setupApplication = setupApplicationBuilderPOWER(apkFile, jarFile);
        } else if (Mode.equals("Recursive")) {
            this.setupApplication = setupApplicationBuilderRecursive(apkFile, jarFile);
        }
        this.results = setupApplication.runInfoflow(sinkSourceFile);
        this.iCFG = new InfoflowCFG();
        this.graph = Scene.v().getCallGraph();
        allComponentFinder();
    }

    public   SetupApplication setupApplicationBuilderFAST (String apkFile, String jarString){
        SetupApplication setupApplication = new SetupApplication(jarString, apkFile);
        InfoflowConfiguration config = setupApplication.getConfig();
        config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);
        config.setAliasingAlgorithm(FlowSensitive);
        config.setCodeEliminationMode(NoCodeElimination);
        config.getSolverConfiguration().setDataFlowSolver(InfoflowConfiguration.DataFlowSolver.ContextFlowSensitive);
        config.getSolverConfiguration().setSparsePropagationStrategy(InfoflowConfiguration.SparsePropagationStrategy.Precise);
        config.setDataFlowDirection(InfoflowConfiguration.DataFlowDirection.Backwards);
        config.setImplicitFlowMode(AllImplicitFlows);
        config.getPathConfiguration().setPathBuildingAlgorithm(ContextSensitive);
        config.setMemoryThreshold(1.0d);
        config.getPathConfiguration().setPathReconstructionTimeout(300);
        config.setDataFlowTimeout(300);
        config.getPathConfiguration().setMaxPathLength(100);
        return setupApplication;
    }
    public   SetupApplication setupApplicationBuilderPOWER (String apkFile, String jarString) throws URISyntaxException, IOException {
        SetupApplication setupApplication = new SetupApplication(jarString, apkFile);
        InfoflowAndroidConfiguration config = setupApplication.getConfig();
        config.setMergeDexFiles(true);
        config.setAliasingAlgorithm(FlowSensitive);
        config.setCodeEliminationMode(NoCodeElimination);
        config.getSolverConfiguration().setDataFlowSolver(InfoflowConfiguration.DataFlowSolver.ContextFlowSensitive);
        config.getSolverConfiguration().setSparsePropagationStrategy(InfoflowConfiguration.SparsePropagationStrategy.Precise);
        config.setImplicitFlowMode(AllImplicitFlows);
        config.getPathConfiguration().setPathReconstructionMode(Precise);
        config.getPathConfiguration().setPathBuildingAlgorithm(ContextSensitive);
        config.setMemoryThreshold(1.0d);
        config.setDataFlowTimeout(1000);
        config.getPathConfiguration().setPathReconstructionTimeout(1000);
        config.getPathConfiguration().setMaxPathLength(100);
        config.setLogSourcesAndSinks(true);
        return setupApplication;
    }

    public   SetupApplication setupApplicationBuilderRecursive (String apkFile, String jarString){
        SetupApplication setupApplication = new SetupApplication(jarString, apkFile);
        InfoflowConfiguration config = setupApplication.getConfig();
        config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);
        config.setAliasingAlgorithm(FlowSensitive);
        config.setCodeEliminationMode(NoCodeElimination);
        config.getSolverConfiguration().setDataFlowSolver(InfoflowConfiguration.DataFlowSolver.ContextFlowSensitive);
        config.getSolverConfiguration().setSparsePropagationStrategy(InfoflowConfiguration.SparsePropagationStrategy.Precise);
        config.setDataFlowDirection(InfoflowConfiguration.DataFlowDirection.Backwards);
        config.setImplicitFlowMode(AllImplicitFlows);
        config.getPathConfiguration().setPathReconstructionMode(Precise);
        config.getPathConfiguration().setPathBuildingAlgorithm(Recursive);
        config.setMemoryThreshold(1.0d);
        config.getPathConfiguration().setPathReconstructionTimeout(500);
        config.setDataFlowTimeout(500);
        config.getPathConfiguration().setMaxPathLength(100);
        config.setLogSourcesAndSinks(true);
        return setupApplication;
    }
    public  void allComponentFinder() throws IOException, XmlPullParserException {
        InfoflowAndroidConfiguration config = this.setupApplication.getConfig();
        ARSCFileParser resources = new ARSCFileParser();
        final File targetAPK = new File(config.getAnalysisFileConfig().getTargetAPKFile());
        resources.parse(targetAPK.getAbsolutePath());
        ProcessManifest processManifest = new ProcessManifest(targetAPK, resources);
        Map<String, String> map = new HashMap<>();
        for (IAndroidComponent component : processManifest.getAllComponents()) {
            String[] temp = component.getClass().getName().split("\\.");
            String type = temp[temp.length - 1];
            map.put(component.getNameString(), type);
        };
        this.allComponents = map;
    }
    public InfoflowResults getResults(){
        return this.results;
    }
    public InfoflowCFG getICFG(){
        return this.iCFG;
    }
    public CallGraph getGraph(){
        return this.graph;
    }
    public Map<String, String> getAllComponents(){
        return this.allComponents;
    }
}