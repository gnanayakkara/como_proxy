package com.como.epoxy.service.serviceinterface;

import com.como.epoxy.dto.EpoxyRequestDTO;

public interface EpoxyService {

    /**
     * Get responses from URLs and bind together and return
     *
     * @param epoxyRequestDTO
     * @return
     * @throws Exception
     */
    public String getResponses(EpoxyRequestDTO epoxyRequestDTO) throws Exception;
}
