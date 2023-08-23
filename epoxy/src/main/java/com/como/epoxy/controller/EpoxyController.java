package com.como.epoxy.controller;

import com.como.epoxy.dto.EpoxyRequestDTO;
import com.como.epoxy.service.serviceinterface.EpoxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/epoxy")
public class EpoxyController {

    @Autowired
    private EpoxyService epoxyService;

    @GetMapping("/fetch/{value}/{result}")
    public String fetchProxyDetails(@PathVariable String value, @PathVariable String result,
                                    @RequestParam Map<String,String> requestParams) throws Exception {


        EpoxyRequestDTO epoxyRequestDTO = EpoxyRequestDTO.builder()
                .encodeUrls(value)
                .resultType(result)
                .requestParams(requestParams)
                .build();

        String responses = epoxyService.getResponses(epoxyRequestDTO);
        return responses;
    }
}
