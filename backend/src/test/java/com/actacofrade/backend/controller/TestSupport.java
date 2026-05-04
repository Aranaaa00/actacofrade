package com.actacofrade.backend.controller;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Utilidades compartidas para los tests de integración HTTP de los controladores
 * con MockMvc en modo standaloneSetup.
 */
final class TestSupport {

    private TestSupport() {}

    static HandlerMethodArgumentResolver authPrincipalResolver(String username, String role) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterAnnotation(AuthenticationPrincipal.class) != null;
            }
            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return User.withUsername(username).password("x").roles(role).build();
            }
        };
    }

    static HandlerMethodArgumentResolver authPrincipalResolver() {
        return authPrincipalResolver("admin@hermandad.es", "ADMINISTRADOR");
    }
}
