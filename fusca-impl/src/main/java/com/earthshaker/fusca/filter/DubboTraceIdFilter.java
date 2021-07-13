package com.earthshaker.fusca.filter;

import com.earthshaker.fusca.constants.Constants;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DubboTraceIdFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext rpcContext = RpcContext.getContext();
        // before
        if (rpcContext.isProviderSide()) {
            // get traceId from dubbo consumer，and set traceId to MDC
            String traceId = rpcContext.getAttachment(Constants.TRACE_ID_KEY);
            MDC.put(Constants.TRACE_ID_KEY, traceId);
        }

        if (rpcContext.isConsumerSide()) {
            // get traceId from MDC, and set traceId to rpcContext
            String traceId = MDC.get(Constants.TRACE_ID_KEY);
            rpcContext.setAttachment(Constants.TRACE_ID_KEY, traceId);
        }
        Result result = invoker.invoke(invocation);
        // after
        if (rpcContext.isProviderSide()) {
            // clear traceId from MDC
            MDC.remove(Constants.TRACE_ID_KEY);
        }
        return result;
    }
}
