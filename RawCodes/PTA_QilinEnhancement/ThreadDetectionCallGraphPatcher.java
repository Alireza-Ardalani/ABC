package eventBasedSystem;

import soot.*;
import soot.grimp.NewInvokeExpr;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ThreadDetectionCallGraphPatcher {
    static Set<SootMethod> targetMethods = new HashSet<>();
    ThreadDetectionCallGraphPatcher(CallGraph callGraph, Set<SootMethod> methods){
        resolveExecutorSubmitMissingEdges(callGraph, methods);
        patchAsyncTaskExecuteToDoInBackground(callGraph);
//        resolveHandler();

    }
    public static void resolveExecutorSubmitMissingEdges(CallGraph callGraph, Set<SootMethod> methods) {
        final Set<String> submitMethods = Set.of(
                "java.util.concurrent.Future submit(java.lang.Runnable)",
                "java.util.concurrent.Future submit(java.util.concurrent.Callable)",

                "java.util.concurrent.ScheduledFuture schedule(java.lang.Runnable,long,java.util.concurrent.TimeUnit)",
                "java.util.concurrent.ScheduledFuture schedule(java.util.concurrent.Callable,long,java.util.concurrent.TimeUnit)",
                "java.util.concurrent.ScheduledFuture scheduleAtFixedRate(java.lang.Runnable,long,long,java.util.concurrent.TimeUnit)",
                "java.util.concurrent.ScheduledFuture scheduleWithFixedDelay(java.lang.Runnable,long,long,java.util.concurrent.TimeUnit)"
        );

        for (SootMethod method : methods) {
            if (!method.isConcrete()) continue;
            Body body;
            try {
                body = method.retrieveActiveBody();
            } catch (Exception e) {
                continue;
            }

            boolean hasRunOrCallEdge = false;
            Iterator<Edge> outEdges = callGraph.edgesOutOf(method);
            while (outEdges.hasNext()) {
                SootMethod tgt = outEdges.next().getTgt().method();
                if (isRunnableRunMethod(tgt) || isCallableCallMethod(tgt)) {
                    hasRunOrCallEdge = true;
                    break;
                }
            }
            if (hasRunOrCallEdge) continue;

            for (Unit unit : body.getUnits()) {
                if (!(unit instanceof Stmt)) continue;
                Stmt stmt = (Stmt) unit;
                if (!stmt.containsInvokeExpr()) continue;

                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                String subSig = invokeExpr.getMethod().getSubSignature();

                if (!submitMethods.contains(subSig)) continue;
                if (invokeExpr.getArgCount() < 1) continue;

                Value arg = invokeExpr.getArg(0);
                Value resolved = traceValue(arg, body);

                if (resolved == null || !(resolved.getType() instanceof RefType)) continue;

                SootClass cls = Scene.v().getSootClassUnsafe(((RefType) resolved.getType()).getClassName(), false);
                if (cls == null || !cls.isConcrete()) continue;

                // Try to resolve run() or call() method
                SootMethod target = null;
                if (implementsInterface(cls, "java.lang.Runnable")) {
                    try {
                        target = cls.getMethod("void run()");
                    } catch (Exception ignored) {}
                } else if (implementsInterface(cls, "java.util.concurrent.Callable")) {
                    try {
                        target = cls.getMethod("java.lang.Object call()");
                    } catch (Exception e1) {
                        try {
                            target = cls.getMethod("java.lang.String call()");
                        } catch (Exception ignored) {}
                    }
                }

                if (target != null) {
                    callGraph.addEdge(new Edge(method, stmt, target));
                    System.out.println("[+] Added synthetic edge: " + method.getSignature() + " → " + target.getSignature());
                    targetMethods.add(target);
                    Set<SootMethod> visited = new HashSet<>();
                    expandCallGraphFrom(target, callGraph, visited);
                }
            }
        }

    }

    public static boolean isRunnableRunMethod(SootMethod method) {
        return method.getName().equals("run") &&
                method.getReturnType().toString().equals("void") &&
                method.getParameterCount() == 0 &&
                implementsInterface(method.getDeclaringClass(), "java.lang.Runnable");
    }
    public static boolean isCallableCallMethod(SootMethod method) {
        return method.getName().equals("call") &&
                method.getParameterCount() == 0 &&
                implementsInterface(method.getDeclaringClass(), "java.util.concurrent.Callable");
    }
    public static boolean implementsInterface(SootClass clazz, String ifaceName) {
        while (clazz != null && !clazz.getName().equals("java.lang.Object")) {
            for (SootClass iface : clazz.getInterfaces()) {
                if (iface.getName().equals(ifaceName)) {
                    return true;
                }
            }
            clazz = clazz.hasSuperclass() ? clazz.getSuperclass() : null;
        }
        return false;
    }
    public static Value traceValue(Value value, Body body) {
        if (!(value instanceof Local)) return value;
        Local local = (Local) value;

        for (Unit u : body.getUnits()) {
            if (!(u instanceof DefinitionStmt)) continue;
            DefinitionStmt def = (DefinitionStmt) u;
            if (!def.getLeftOp().equivTo(local)) continue;

            Value rhs = def.getRightOp();

            if (rhs instanceof NewExpr || rhs instanceof NewInvokeExpr) {
                return local;
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
    public static void patchAsyncTaskExecuteToDoInBackground(CallGraph callGraph) {
        Set<String> asyncExecutionMethods = Set.of(
                "<android.os.AsyncTask: android.os.AsyncTask execute(java.lang.Object[])>",
                "<android.os.AsyncTask: android.os.AsyncTask executeOnExecutor(java.util.concurrent.Executor,java.lang.Object[])>"
        );

        Set<SootMethod> executionMethods = new HashSet<>();
        for (String sig : asyncExecutionMethods) {
            try {
                SootMethod m = Scene.v().getMethod(sig);
                executionMethods.add(m);
                // System.out.println("[*] Found AsyncTask method: " + sig);
            } catch (Exception e) {
                //System.err.println("[!] Could not find method: " + sig);
            }
        }

        for (SootMethod execMethod : executionMethods) {
            // System.out.println("[*] Checking call graph edges into: " + execMethod.getSignature());

            Iterator<Edge> edgesInto = callGraph.edgesInto(execMethod);
            int count = 0;

            while (edgesInto.hasNext()) {
                count++;
                Edge edge = edgesInto.next();
                SootMethod caller = edge.getSrc().method();
                //System.out.println("[*] Caller: " + caller.getSignature());

                if (!caller.isConcrete()) continue;

                if (hasCallToDoInBackground(callGraph, caller)) {
                    //  System.out.println("[*] Already calls doInBackground, skipping.");
                    continue;
                }

                Set<SootClass> constructedClasses = new HashSet<>();
                try {
                    Body body = caller.retrieveActiveBody();
                    for (Unit u : body.getUnits()) {
                        Stmt stmt = (Stmt) u;
                        if (!stmt.containsInvokeExpr()) continue;

                        InvokeExpr expr = stmt.getInvokeExpr();
                        if (!(expr instanceof SpecialInvokeExpr)) continue;

                        if (!expr.getMethod().getName().equals("<init>")) continue;

                        SootClass targetClass = expr.getMethod().getDeclaringClass();
                        constructedClasses.add(targetClass);
                        //System.out.println("[*] Found <init> to: " + targetClass.getName());
                    }
                } catch (Exception e) {
                    //System.err.println("[!] Could not read body for: " + caller.getSignature());
                    continue;
                }

                for (SootClass cls : constructedClasses) {
                    //  System.out.println("[*] Checking class: " + cls.getName());

                    // Must extend AsyncTask
                    if (!Scene.v().getOrMakeFastHierarchy().canStoreType(cls.getType(),
                            Scene.v().getSootClass("android.os.AsyncTask").getType())) {
                        // System.out.println("[!] Class does not extend AsyncTask: " + cls.getName());
                        continue;
                    }

                    for (SootMethod m : cls.getMethods()) {
                        if (m.getName().equals("doInBackground")) {
                            System.out.println("[+] Found doInBackground override: " + m.getSignature());

                            if (!edgeExists(callGraph, caller, m)) {
                                callGraph.addEdge(new Edge(caller, edge.srcStmt(), m));
                                System.out.println("[+] Synthetic edge added: " + caller.getSignature()
                                        + " → " + m.getSignature());
                                targetMethods.add(m);

                                Set<SootMethod> visited = new HashSet<>();
                                expandCallGraphFrom(m, callGraph, visited);
                            }
                        }
                    }
                }
            }

            if (count == 0) {
                //  System.out.println("[!] No callers found for: " + execMethod.getSignature());
            }
        }
    }
    private static boolean hasCallToDoInBackground(CallGraph callGraph, SootMethod caller) {
        Iterator<Edge> outEdges = callGraph.edgesOutOf(caller);
        while (outEdges.hasNext()) {
            SootMethod callee = outEdges.next().getTgt().method();
            if (callee.getName().equals("doInBackground")) {
                return true;
            }
        }
        return false;
    }


    public static void expandCallGraphFrom(SootMethod method, CallGraph callGraph, Set<SootMethod> visited) {
        if (!method.isConcrete() || visited.contains(method)) return;
        visited.add(method);

        Body body;
        try {
            body = method.retrieveActiveBody();
        } catch (Exception e) {
            return;
        }

        for (Unit unit : body.getUnits()) {
            Stmt stmt = (Stmt) unit;
            if (!stmt.containsInvokeExpr()) continue;

            InvokeExpr expr = stmt.getInvokeExpr();
            SootMethod target;
            try {
                target = expr.getMethod();
            } catch (Exception e) {
                continue;
            }

            if (!edgeExists(callGraph, method, target)) {
                callGraph.addEdge(new Edge(method, stmt, target));
                System.out.println("[++] Edge: " + method.getSignature() + " → " + target.getSignature());
            }

            // Recurse on the target
            expandCallGraphFrom(target, callGraph, visited);
        }
    }

    private static boolean edgeExists(CallGraph callGraph, SootMethod src, SootMethod tgt) {
        Iterator<Edge> outEdges = callGraph.edgesOutOf(src);
        while (outEdges.hasNext()) {
            Edge e = outEdges.next();
            if (e.getTgt().method().equals(tgt)) {
                return true;
            }
        }
        return false;
    }

    public static Set<SootMethod> getTargetMethods() {
        return targetMethods;
    }

    public static void resolveHandler() {
        Set<SootClass> handlerSubclasses = new HashSet<>();

        // Step 1: Find subclasses of android.os.Handler
        for (SootClass cls : Scene.v().getClasses()) {
            if (!cls.isPhantom() && cls.hasSuperclass()) {
                SootClass superCls = cls.getSuperclass();
                while (superCls != null && !superCls.getName().equals("java.lang.Object")) {
                    if (superCls.getName().equals("android.os.Handler")) {
                        handlerSubclasses.add(cls);
                        break;
                    }
                    if (!superCls.hasSuperclass()) break;
                    superCls = superCls.getSuperclass();
                }
            }
        }

        System.out.println("Handler subclasses:");
        for (SootClass handlerCls : handlerSubclasses) {
            System.out.println(" - " + handlerCls.getName());
        }

        // Step 2: For each constructor, find invocations
        CallGraph cg = Scene.v().getCallGraph();

        for (SootClass handlerCls : handlerSubclasses) {
            for (SootMethod constructor : handlerCls.getMethods()) {
                if (!constructor.getName().equals("<init>")) continue;

                Iterator<Edge> edges = cg.edgesInto(constructor);
                while (edges.hasNext()) {
                    Edge edge = edges.next();
                    SootMethod caller = edge.src();
                    Stmt stmt = edge.srcStmt();

                    if (!caller.hasActiveBody()) continue;
                    if (!stmt.containsInvokeExpr()) continue;

                    InvokeExpr expr = stmt.getInvokeExpr();
                    if (!(expr instanceof SpecialInvokeExpr)) continue;

                    SpecialInvokeExpr specialInvoke = (SpecialInvokeExpr) expr;
                    Value base = specialInvoke.getBase();

                    Body body = caller.getActiveBody();
                    PatchingChain<Unit> units = body.getUnits();
                    Local baseLocal = (Local) base;

                    // Step 3: Ensure handleMessage(Message) method exists
                    SootMethod handleMessage = null;
                    for (SootMethod m : handlerCls.getMethods()) {
                        if (m.getName().equals("handleMessage")
                                && m.getParameterCount() == 1
                                && m.getParameterType(0).toString().equals("android.os.Message")) {
                            handleMessage = m;
                            break;
                        }
                    }

                    if (handleMessage == null) continue;

                    // Step 4: Add local for Message
                    Local msgLocal = Jimple.v().newLocal("msgDummy", RefType.v("android.os.Message"));
                    body.getLocals().add(msgLocal);

                    // Step 5: Assign msgDummy = Message.obtain()
                    SootMethod obtain = Scene.v().getMethod("<android.os.Message: android.os.Message obtain()>");
                    AssignStmt obtainStmt = Jimple.v().newAssignStmt(msgLocal, Jimple.v().newStaticInvokeExpr(obtain.makeRef()));

                    // Step 6: Add r2.handleMessage(msgDummy)
                    InvokeExpr handleExpr = Jimple.v().newVirtualInvokeExpr(baseLocal, handleMessage.makeRef(), msgLocal);
                    Stmt callStmt = Jimple.v().newInvokeStmt(handleExpr);

                    // Step 7: Inject statements with comment
                    // You can’t insert a comment as a statement in Jimple, so just print it as a log here
                    System.out.println("// Injected handleMessage call in: " + caller.getSignature());

                    units.insertAfter(obtainStmt, stmt);
                    units.insertAfter(callStmt, obtainStmt);

                    // Step 8: Add call graph edge
                    Edge newEdge = new Edge(caller, callStmt, handleMessage);
                    cg.addEdge(newEdge);

                    // Step 9: Log
                    System.out.println("Added statement: " + callStmt);
                    System.out.println("Added edge: " + caller.getSignature() + " --> " + handleMessage.getSignature());
                }
            }
        }
    }
}