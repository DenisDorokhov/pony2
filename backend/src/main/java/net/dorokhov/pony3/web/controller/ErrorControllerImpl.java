package net.dorokhov.pony3.web.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class ErrorControllerImpl implements ErrorController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping
    public String error(HttpServletRequest request, Model model) {
        model.addAttribute("status", getStatus(request));
        return "error";
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            logger.error("Could not resolve HTTP status.");
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception e) {
            logger.error("Unknown HTTP status: '{}'.", statusCode, e);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
