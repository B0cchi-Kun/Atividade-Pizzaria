package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.exception.RegraNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = HomeController.class)
public class WebExceptionHandler {
    @ExceptionHandler(RegraNegocioException.class)
    public ModelAndView regraNegocio(RegraNegocioException exception) {
        ModelAndView view = new ModelAndView("error");
        view.addObject("status", HttpStatus.BAD_REQUEST.value());
        view.addObject("message", exception.getMessage());
        view.setStatus(HttpStatus.BAD_REQUEST);
        return view;
    }
}
