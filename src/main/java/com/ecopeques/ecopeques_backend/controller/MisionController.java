package com.ecopeques.ecopeques_backend.controller;

import com.ecopeques.ecopeques_backend.dto.MisionResponse;
import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.service.MisionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/misiones")
@RequiredArgsConstructor
public class MisionController {

    private final MisionService misionService;

    @GetMapping
    public ResponseEntity<List<MisionResponse>> obtenerMisiones(
            @RequestParam(required = false) CategoriaMision categoria
    ) {
        return ResponseEntity.ok(misionService.obtenerMisionesActivas(categoria));
    }
}
