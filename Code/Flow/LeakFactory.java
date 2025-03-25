import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeakFactory {
    public DataFlowResult result;
    public InfoflowCFG iCFG;
    public CallGraph graph;
    public Map<String, String> allComponents;
    public SootClass enclosingComponent;
    private String apkFilePath;

    private  List<Stmt> GUIList = new ArrayList<Stmt>();

    private List<String> GUIxmlList = new ArrayList<String>();
    private InfoflowCFG ICFGforGUI;

    private String Mode;

    public LeakFactory(DataFlowResult resultInput, InfoflowCFG iCFGInput , CallGraph graphInput , Map<String, String> allComponentsInput, String apkPath , String Mode){
        this.Mode = Mode;
        this.result = resultInput;
        this.iCFG = iCFGInput;
        this.graph = graphInput;
        this.allComponents = allComponentsInput;
        this.apkFilePath = apkPath;
        setEnclosingComponent();

    }

    private void setEnclosingComponent(){
        if(Mode.equals("Recursive")){
            SootMethod Sink = this.iCFG.getMethodOf(this.result.getSink().getStmt());
            this.enclosingComponent  = findComponent(this.graph, Sink,this.allComponents);
            if(this.enclosingComponent == null ){
                SootMethod Source = this.iCFG.getMethodOf(this.result.getSource().getStmt());
                this.enclosingComponent  = findComponent(this.graph, Source,this.allComponents);
            }
            if(this.enclosingComponent == null){
                if(!(result.getSource().getPath() == null)){
                    for(Stmt statement : result.getSource().getPath()){
                        String clas = iCFG.getMethodOf(statement).getDeclaringClass().toString();
                        for (Map.Entry<String, String> entry : allComponents.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if(key.equals(clas)) {
                                this.enclosingComponent = iCFG.getMethodOf(statement).getDeclaringClass();
                                return;
                            }
                        }
                    }
                }
            }

        } else if (Mode.equals("POWER")) {
            SootMethod Source = this.iCFG.getMethodOf(this.result.getSource().getStmt());
            this.enclosingComponent  = findComponent(this.graph, Source,this.allComponents);
            if(this.enclosingComponent == null ){
                SootMethod Sink = this.iCFG.getMethodOf(this.result.getSink().getStmt());
                this.enclosingComponent  = findComponent(this.graph, Sink,this.allComponents);
            }
            if(this.enclosingComponent == null){
                if(!(result.getSource().getPath() == null)){
                    for(Stmt statement : result.getSource().getPath()){
                        String clas = iCFG.getMethodOf(statement).getDeclaringClass().toString();
                        for (Map.Entry<String, String> entry : allComponents.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if(key.equals(clas)) {
                                this.enclosingComponent = iCFG.getMethodOf(statement).getDeclaringClass();
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
    private static SootClass findComponent(CallGraph graph, SootMethod method , Map<String,String> map){
        List<SootMethod> visitedMethod = new ArrayList<SootMethod>();
        Queue<SootMethod> methodsQueue = new LinkedList<>();
        visitedMethod.add(method);
        methodsQueue.add(method);
        boolean check = false;
        while(!methodsQueue.isEmpty()){
            SootMethod currentMethod = methodsQueue.poll();
            SootClass currentComponent = currentMethod.getDeclaringClass();
            String className = currentComponent.getName();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if(key.equals(className)) {
                    check = true;
                    break;
                }
            }
            if(check){
                return currentComponent;
            }
            Iterator<Edge> neighbours = graph.edgesInto(currentMethod);
            while(neighbours.hasNext()){
                SootMethod callerMethod = neighbours.next().src();
                if(!(visitedMethod.contains(callerMethod))){
                    visitedMethod.add(callerMethod);
                    methodsQueue.add(callerMethod);
                }
            }
        }
        return null;
    }

    public DataFlowResult getResult(){
        return this.result;
    }
    public void addGUI(Stmt input) {
        this.GUIList.add(input);
    }
    public SootClass getEnclosingComponent() {
        return enclosingComponent;
    }

    public List<String> getGUIxmlList(){
        return this.GUIxmlList;
    }

    public void addICFGforGUI(InfoflowCFG icfg) {
        this.ICFGforGUI = icfg;
    }

    public InfoflowCFG getiCFG(){
        return this.iCFG;
    }
}