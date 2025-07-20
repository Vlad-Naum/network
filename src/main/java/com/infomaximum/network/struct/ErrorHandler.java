package com.infomaximum.network.struct;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

public interface ErrorHandler {

    void handle(Request request, Response response, Integer errorCode, String errorMessage, Throwable throwable);

}
