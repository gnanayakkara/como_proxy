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
import java.util.Base64;
import java.util.List;
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

        //Terminate the process if timeout params available
        if (epoxyRequestDTO.getRequestParams().get("timeout") != null){
            terminateThreadGroup(epoxyRequestDTO,executorService);
        }

        //Submit invoke api task to thread pool
        List<CompletableFuture<ResourceServiceDTO>> collect = listOfUrls.stream()
                .map(site -> {
                    return CompletableFuture.supplyAsync(() -> this.callResourseServices(site,epoxyRequestDTO), executorService);
                })
                .collect(Collectors.toList());

        List<ResourceServiceDTO> resourceServiceDTOList = collect.stream().map(val -> {
            try {
                return val.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return buildResponse(resourceServiceDTOList,epoxyRequestDTO);
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
     * Configure time out
     * @param executorService
     * @throws Exception
     */
    private void terminateThreadGroup(EpoxyRequestDTO epoxyRequestDTO,ExecutorService executorService){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if(!executorService.awaitTermination(Long.valueOf(epoxyRequestDTO.getRequestParams().get("timeout")),TimeUnit.MILLISECONDS)){
                        executorService.shutdown();
                        //Set time out value for thread group
                        executorService.shutdownNow();
                    }
                 } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String buildResponse(List<ResourceServiceDTO> resourceServiceDTOList,EpoxyRequestDTO epoxyRequestDTO) {

        if(resourceServiceDTOList != null) {

            Gson gson = new Gson();

            List<String> list = resourceServiceDTOList.stream().map(dto -> dto.getUrl() + ":" + dto.getResponseValue()).toList();

            if (epoxyRequestDTO.getResultType().equals("combined")){

                return gson.toJson(list.toArray());

            } else if (epoxyRequestDTO.getResultType().equals("appended")){

                return gson.toJson(list);
            }
        }

        return null;
    }

}
