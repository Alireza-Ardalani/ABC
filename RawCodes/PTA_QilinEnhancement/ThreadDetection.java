package eventBasedSystem;

import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.grimp.NewInvokeExpr;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.IOException;
import java.util.*;

import static soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm.FlowSensitive;
import static soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode.NoCodeElimination;
import static soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode.AllImplicitFlows;
import static soot.jimple.infoflow.InfoflowConfiguration.PathBuildingAlgorithm.ContextInsensitive;

public class ThreadDetection {

    private Set<SootMethod> workerThreadStarterMethod = new HashSet<>();
    public ThreadDetection(){
        CallGraph callGraph = Scene.v().getCallGraph();

        Set<SootMethod> allMethodsInCallGraph = new HashSet<>();
        for (Edge edge : callGraph) {
            SootMethod src = edge.getSrc().method();
            SootMethod tgt = edge.getTgt().method();
            allMethodsInCallGraph.add(src);
            allMethodsInCallGraph.add(tgt);
        }


        ThreadDetectionCallGraphPatcher threadDetectionCallGraphPatcher = new ThreadDetectionCallGraphPatcher(callGraph,allMethodsInCallGraph);
        //
        workerThreadStarterMethod.addAll(threadDetectionCallGraphPatcher.getTargetMethods());



        for (Edge edge : callGraph) {
            SootMethod src = edge.getSrc().method();
            SootMethod tgt = edge.getTgt().method();

            if (edge.kind().toString().equals("THREAD") ||
                    edge.kind().toString().equals("EXECUTOR") ) {
                workerThreadStarterMethod.add(tgt);
                System.out.println("\n" +"Group0-Plan0=> " + tgt );
            } else if (edge.kind().toString().equals("ASYNCTASK") &&
                    tgt.getSignature().contains("doInBackground")) {
                workerThreadStarterMethod.add(tgt);
                System.out.println("\n" +"Group0-Plan1=> " + tgt );
            }

            //Group1-Plan1
            if (src.getSignature().equals("<java.lang.Thread: void run()>")) {
                workerThreadStarterMethod.add(tgt);
                System.out.println("\n" +"Group1-Plan1=> " + tgt );
            }

            //Group2
            List<String> executors = new ArrayList<>(Arrays.asList(
                    "<java.util.concurrent.Executors: java.util.concurrent.ExecutorService newFixedThreadPool(int)>",
                    "<java.util.concurrent.Executors: java.util.concurrent.ExecutorService newFixedThreadPool(int,java.util.concurrent.ThreadFactory)>",
                    "<java.util.concurrent.Executors: java.util.concurrent.ExecutorService newCachedThreadPool()>",
                    "<java.util.concurrent.Executors: java.util.concurrent.ExecutorService newCachedThreadPool(java.util.concurrent.ThreadFactory)>",
                    "<java.util.concurrent.Executors: java.util.concurrent.ExecutorService newSingleThreadExecutor()>",
                    "<java.util.concurrent.Executors: java.util.concurrent.ExecutorService newSingleThreadExecutor(java.util.concurrent.ThreadFactory)>",
                    "<java.util.concurrent.Executors: java.util.concurrent.ExecutorService newWorkStealingPool()>",
                    "<java.util.concurrent.Executors: java.util.concurrent.ExecutorService newWorkStealingPool(int)>"
            ));

            //Group2-Plan3
            if (executors.contains(tgt.getSignature())) {
                //Group2-Plan3
                SootMethod run = findRunnableInExecutor(src);
                if(run != null){
                    workerThreadStarterMethod.add(run);
                    System.out.println("\n" +"Group2-Plan3=> " + run);
                } else {
                    System.out.println("RUN IS NULL");
                }
                //Group2-Plan3.1
                if(containExecutor(src)){
                    Iterator<Edge> edges = callGraph.edgesOutOf(src);
                    while (edges.hasNext()) {
                        Edge edge1 = edges.next();
                        SootMethod tgt1 = edge1.tgt();
                        if(isRunnableRunMethod(tgt1)){
                            workerThreadStarterMethod.add(tgt1);
                            System.out.println("\n" +"Group2-Plan3.1=> " + tgt1);
                        }
                    }
                }
            }


        }

        //Group1-Plan2
        for(SootClass clazz : Scene.v().getClasses()){

            if(isExtendedThread(clazz) && !isInternalFrameworkExtendedThread(clazz)){
                SootMethod run = getRunMethod(clazz);
                if(run != null){
                    workerThreadStarterMethod.add(run);
                    System.out.println("\n" +"Group1-Plan2=> " + run );
                }
            }

            for (SootMethod method: clazz.getMethods()) {
                if(method.isConcrete()){
                    method.retrieveActiveBody();
                    if (method.hasActiveBody()){
                        if(containExecutor(method)) {
                            Iterator<Edge> edges = callGraph.edgesOutOf(method);
                            while (edges.hasNext()) {
                                Edge edge1 = edges.next();
                                SootMethod tgt1 = edge1.tgt();
                                if(isRunnableRunMethod(tgt1)){
                                    workerThreadStarterMethod.add(tgt1);
                                    System.out.println("\n" +"Group2-Plan3.2=> " + tgt1);
                                }
                            }

                        }
                    } else {
                        //Not active Body
                    }

                }
            }
        }






    }

    public static boolean isRunnableRunMethod(SootMethod method) {
        if (!method.getName().equals("run")) return false;
        if (!method.getReturnType().toString().equals("void")) return false;
        if (method.getParameterCount() != 0) return false;

        // Check if class or its superclass implements java.lang.Runnable
        SootClass clazz = method.getDeclaringClass();

        while (clazz != null && !clazz.getName().equals("java.lang.Object")) {
            for (SootClass iface : clazz.getInterfaces()) {
                if (iface.getName().equals("java.lang.Runnable")) {
                    return true;  // class implements Runnable
                }
            }

            if (clazz.hasSuperclass()) {
                clazz = clazz.getSuperclass();
            } else {
                break;
            }
        }

        return false;
    }


    public static boolean isExtendedThread(SootClass clazz) {
        String targetSuperClassName = "java.lang.Thread";
        while (clazz.hasSuperclass()) {
            SootClass superClass = clazz.getSuperclass();
            if (superClass.getName().equals(targetSuperClassName)) {
                return true;
            }
            if (superClass.getName().equals("java.lang.Object")) {
                return false;
            }
            clazz = superClass;
        }
        return false;
    }
    public static boolean isInternalFrameworkExtendedThread(SootClass sootClass) {
        String name = sootClass.getName();
        return name.startsWith("android.")
                || name.startsWith("androidx.")
                || name.startsWith("kotlin.")
                || name.startsWith("kotlinx.")
                || name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
                || name.startsWith("com.android.")
                || name.startsWith("dalvik.")
                || name.startsWith("libcore.")
                || name.startsWith("junit.")
                || name.startsWith("org.junit.")
                || name.startsWith("org.apache.")
                || name.startsWith("org.json.")
                || name.startsWith("org.xml.")
                || name.startsWith("org.w3c.")
                || name.startsWith("org.objectweb.")
                || name.startsWith("org.intellij.");
    }

    private static SootMethod getRunMethod(SootClass clazz){
        SootClass current = clazz;
        while (current.hasSuperclass()) {
            for (SootMethod method : current.getMethods()) {
                if (method.getName().equals("run") &&
                        method.getParameterCount() == 0 &&
                        method.getReturnType().toString().equals("void")) {
                    return method;
                }
            }
            current = current.getSuperclass();
            if (current.getName().equals("java.lang.Object")) break;
        }
        return null;
    }

    public static SootMethod findRunnableInExecutor(SootMethod method) {
        if (!method.hasActiveBody()) return null;
        Body body = method.getActiveBody();
        UnitPatchingChain units = body.getUnits();

        final String targetSubSig = "void execute(java.lang.Runnable)";

        for (Unit unit : units) {
            if (!(unit instanceof Stmt)) continue;
            Stmt stmt = (Stmt) unit;
            if (!stmt.containsInvokeExpr()) continue;

            InvokeExpr invoke = stmt.getInvokeExpr();
            if (!invoke.getMethod().getSubSignature().equals(targetSubSig)) continue;

            Value arg = invoke.getArg(0);
            Value resolved = traceValue(arg, body);

            if (resolved != null) {
                SootMethod runMethod = resolveRunMethod(resolved);
                if (runMethod != null) return runMethod;
            }
        }

        return null;
    }

    public static Value traceValue(Value value, Body body) {
        if (!(value instanceof Local)) return value;
        Local local = (Local) value;

        for (Unit unit : body.getUnits()) {
            if (!(unit instanceof DefinitionStmt)) continue;

            DefinitionStmt def = (DefinitionStmt) unit;
            if (!def.getLeftOp().equivTo(local)) continue;

            Value rhs = def.getRightOp();

            if (rhs instanceof NewExpr || rhs instanceof NewInvokeExpr) {
                return local;  // Direct instantiation
            }

            if (rhs instanceof InvokeExpr) {
                return traceInvokeReturn((InvokeExpr) rhs);
            }

            if (rhs instanceof Local) {
                return traceValue(rhs, body);
            }

            if (rhs instanceof InstanceFieldRef || rhs instanceof StaticFieldRef) {
                return traceFieldValue(rhs, body);
            }
        }

        return null;
    }

    public static Value traceInvokeReturn(InvokeExpr expr) {
        SootMethod target = expr.getMethod();
        if (!target.hasActiveBody()) return null;

        for (Unit u : target.getActiveBody().getUnits()) {
            if (u instanceof ReturnStmt) {
                ReturnStmt ret = (ReturnStmt) u;
                Value returned = ret.getOp();

                if (returned instanceof Local) {
                    return traceValue(returned, target.getActiveBody());
                } else {
                    return returned;
                }
            }
        }

        return null;
    }

    public static Value traceFieldValue(Value fieldRef, Body callerBody) {
        SootField field = null;

        if (fieldRef instanceof InstanceFieldRef) {
            field = ((InstanceFieldRef) fieldRef).getField();
        } else if (fieldRef instanceof StaticFieldRef) {
            field = ((StaticFieldRef) fieldRef).getField();
        }

        if (field == null || !field.isDeclared()) return null;

        SootClass declaringClass = field.getDeclaringClass();
        if (!declaringClass.isConcrete()) return null;

        // Search for field assignment in declaring class methods
        for (SootMethod method : declaringClass.getMethods()) {
            if (!method.hasActiveBody()) continue;

            for (Unit unit : method.getActiveBody().getUnits()) {
                if (!(unit instanceof DefinitionStmt)) continue;

                DefinitionStmt def = (DefinitionStmt) unit;
                if (!(def.getLeftOp() instanceof FieldRef)) continue;

                FieldRef leftRef = (FieldRef) def.getLeftOp();
                if (!leftRef.getField().equals(field)) continue;

                Value rhs = def.getRightOp();
                if (rhs instanceof Local) {
                    return traceValue(rhs, method.getActiveBody());
                } else {
                    return rhs;
                }
            }
        }

        return null;
    }

    public static SootMethod resolveRunMethod(Value value) {
        if (!(value.getType() instanceof RefType)) return null;

        String className = ((RefType) value.getType()).getClassName();
        SootClass clazz = Scene.v().getSootClassUnsafe(className, false);
        if (clazz == null || !clazz.isConcrete()) return null;

        for (SootMethod m : clazz.getMethods()) {
            if (m.getName().equals("run") &&
                    m.getParameterCount() == 0 &&
                    m.getReturnType().toString().equals("void")) {
                return m;
            }
        }

        return null;
    }

    public static boolean containExecutor(SootMethod method) {
        method.retrieveActiveBody();
        if (!method.hasActiveBody()) return false;
        Body body = method.getActiveBody();
        List<String> executorMethods = Arrays.asList(
                "void execute(java.lang.Runnable)"
//                "java.util.concurrent.Future submit(java.lang.Runnable)",
//                "java.util.concurrent.Future submit(java.util.concurrent.Callable)",
//                "java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)"
        );

        for (Unit unit : body.getUnits()) {
            if (!(unit instanceof Stmt)) continue;
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invoke = stmt.getInvokeExpr();
                String sig = invoke.getMethod().getSubSignature();

                if (executorMethods.contains(sig)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Set<SootMethod> getWorkerThreadStarterMethod() {
        Set<SootMethod> cleanList = new HashSet<>();
        for (SootMethod m : workerThreadStarterMethod) {
            String subSig = m.getSubSignature(); // like: java.lang.Object doInBackground(java.lang.Object[])
            SootClass declaringClass = m.getDeclaringClass();

            if (subSig.contains("doInBackground")) {
                // Try to find java.lang.Void doInBackground(java.lang.Void[]) in the same class
                String targetSubSig = "java.lang.Void doInBackground(java.lang.Void[])";

                if (declaringClass.declaresMethod(targetSubSig)) {
                    SootMethod cleanMethod = declaringClass.getMethod(targetSubSig);
                    cleanList.add(cleanMethod);
                    continue;
                }
            }

            // Default: add the original method
            cleanList.add(m);
        }

        return workerThreadStarterMethod;

//        Set<SootMethod> cleanList = new HashSet<>();
//        for (SootMethod m : workerThreadStarterMethod) {
//            String subSig = m.getSubSignature(); // like: java.lang.Object doInBackground(java.lang.Object[])
//            SootClass declaringClass = m.getDeclaringClass();
//
//            if (subSig.contains("doInBackground")) {
//                // Try to find java.lang.Void doInBackground(java.lang.Void[]) in the same class
//                String targetSubSig = "java.lang.Void doInBackground(java.lang.Void[])";
//
//                if (declaringClass.declaresMethod(targetSubSig)) {
//                    SootMethod cleanMethod = declaringClass.getMethod(targetSubSig);
//                    cleanList.add(cleanMethod);
//                    continue;
//                }
//            }
//
//            // Default: add the original method
//            cleanList.add(m);
//        }
//
//        return cleanList;
    }
}



