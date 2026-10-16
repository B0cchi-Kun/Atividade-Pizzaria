package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.exception.RegraNegocioException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        ClienteController.class, PedidoController.class, ProdutoController.class,
        UsuarioController.class, AuditoriaController.class
})
public class RestApiExceptionHandler {
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> regraNegocio(RegraNegocioException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", exception.getMessage()));
    }
}
