package qilin.parm.ctxcons;

import qilin.core.context.ContextElement;
import qilin.core.context.ContextElements;
import qilin.core.context.ContextElementsComponent;
import qilin.core.context.ContextElementsThread;
import qilin.core.pag.CallSite;
import qilin.core.pag.ContextAllocNode;
import soot.Context;
import soot.MethodOrMethodContext;
import soot.SootMethod;

public class CallSiteCtxThreadConstructor implements CtxConstructor {
    public Context constructCtx(MethodOrMethodContext caller, ContextAllocNode receiverNode, CallSite callSite, SootMethod target) {
        Context callerContext = caller.context();
        assert callerContext instanceof ContextElements;
        ContextElements ctxElems = (ContextElements) callerContext; //?
        int s = ctxElems.size();
        ContextElement[] cxt = ctxElems.getElements();
        ContextElement[] array = new ContextElement[s + 1];
        array[0] = callSite;
        System.arraycopy(cxt, 0, array, 1, s);
        return new ContextElementsThread(array, s + 1, ctxElems.getOffContext(), ctxElems.getTopElements());
    }
}
