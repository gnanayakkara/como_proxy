package com.como.epoxy.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class EpoxyRequestDTO {

    private String encodeUrls;
    private String resultType;
    private Map<String,String> requestParams;

}
