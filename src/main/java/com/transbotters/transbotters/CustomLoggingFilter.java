package com.transbotters.transbotters;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NonNls;
import org.springframework.web.filter.CommonsRequestLoggingFilter;


public class CustomLoggingFilter extends CommonsRequestLoggingFilter {
    private final String[] shouldFilter = {"static", "webjars", "image", "css"};

    @Override
    protected void beforeRequest(HttpServletRequest request,  String message) {
        @NonNls String enhancedMessage = request.getRemoteAddr() + " " + message; // Retrieve the IP address
        super.beforeRequest(request, message + enhancedMessage);
    }


    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        return logger.isDebugEnabled() &&
                !Utils.containsAny(request.getRequestURL().toString(), shouldFilter); //Avoid logging static files
    }
}
