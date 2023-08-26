package com.como.epoxy.service.serviceimpl;

import com.como.epoxy.dto.EpoxyRequestDTO;
import com.como.epoxy.dto.RequestURLs;
import com.como.epoxy.dto.ResourceServiceDTO;
import com.como.epoxy.service.serviceinterface.EpoxyService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class EpoxyServiceImpl implements EpoxyService {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResponses(EpoxyRequestDTO epoxyRequestDTO) throws Exception {

        //Get list of urls from base64 encoding
        List<String> listOfUrls = getListofUrlsFromBase64Value(epoxyRequestDTO.getEncodeUrls());

        //Create threads for the urls
        ExecutorService executorService = Executors.newFixedThreadPool(listOfUrls.size());

        //Submit invoke api task to thread pool
        List<CompletableFuture<ResourceServiceDTO>> collect = listOfUrls.stream()
                .map(site -> {
                    CompletableFuture<ResourceServiceDTO> resourceServiceDTOCompletableFuture = CompletableFuture
                            .supplyAsync(() -> this.callResourseServices(site, epoxyRequestDTO), executorService);

                    //Terminate the process if timeout params available
                    if(epoxyRequestDTO.getRequestParams().get("timeout")!=null){
                        terminateThread(resourceServiceDTOCompletableFuture,epoxyRequestDTO);
                    }

                    return resourceServiceDTOCompletableFuture;

                })
                .collect(Collectors.toList());

        Map<String, String> collectedResources = collect.stream().map(val -> {
            try {
                return val.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toMap(ResourceServiceDTO::getUrl, ResourceServiceDTO::getResponseValue));

        return buildResponse(collectedResources,epoxyRequestDTO);

    }

    /**
     * Get list of urls after decode from Base64
     * @param encodeUrls
     * @return
     * @throws Exception
     */
    private List<String> getListofUrlsFromBase64Value(String encodeUrls) throws Exception {

        byte[] decodedBytes = Base64.getDecoder().decode(encodeUrls);
        String decodedUrls = new String(decodedBytes);

        Gson gson = new Gson();
        Type listType = new TypeToken<RequestURLs>(){}.getType();
        RequestURLs requestURLs = gson.fromJson(decodedUrls,listType);

        return requestURLs.getUrls();
    }

    /**
     * Resource service call method
     * @param url
     * @return
     */
    private ResourceServiceDTO callResourseServices(String url,EpoxyRequestDTO requestDTO){

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);

            ResourceServiceDTO resourceServiceDTO = new ResourceServiceDTO();
            resourceServiceDTO.setUrl(url);

            resourceServiceDTO.setResponseValue(responseEntity.getBody().toString());

            return resourceServiceDTO;
        } catch (Exception e){

            //Error handle for requested parameter
            if (requestDTO.getRequestParams().get("errors") != null) {
                if (requestDTO.getRequestParams().get("errors").equals("[fail_any]")){
                    throw new RuntimeException("Execution failed for URL : " + url);
                } else if(requestDTO.getRequestParams().get("errors").equals("[replace]")){
                    ResourceServiceDTO resourceServiceDTO = new ResourceServiceDTO();
                    resourceServiceDTO.setUrl(url);
                    resourceServiceDTO.setResponseValue("failed");
                    return resourceServiceDTO;
                }
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }


    /**
     * Set timeout value to completable feature
     * @param resourceServiceDTOCompletableFuture
     * @param epoxyRequestDTO
     */
    private void terminateThread(CompletableFuture<ResourceServiceDTO> resourceServiceDTOCompletableFuture,EpoxyRequestDTO epoxyRequestDTO){
        resourceServiceDTOCompletableFuture.orTimeout(Long.valueOf(epoxyRequestDTO.getRequestParams().get("timeout")),TimeUnit.MILLISECONDS);
    }

    private String buildResponse(Map<String,String> resourceServiceDTOMap,EpoxyRequestDTO epoxyRequestDTO) throws Exception {

        if(resourceServiceDTOMap != null) {

            Gson gson = new Gson();

            if (epoxyRequestDTO.getResultType().equals("combined")){

                Type typeObject = new TypeToken<HashMap>() {}.getType();
                return gson.toJson(resourceServiceDTOMap, typeObject);

            } else if (epoxyRequestDTO.getResultType().equals("appended")){

                String[][] buildArray = resourceServiceDTOMap.entrySet().stream()
                        .map(entry -> new String[]{entry.getKey(), entry.getValue()})
                        .toArray(String[][]::new);

                Type typeObject = new TypeToken<String[][]>() {}.getType();

                return gson.toJson(buildArray,typeObject);
            }
        }

        return null;
    }

}
