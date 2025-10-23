package org.example.ai_api.Controller;

import org.example.ai_api.Bean.ApiRepeat.GlossaryRepeat;
import org.example.ai_api.Bean.ApiRepeat.GlossaryUpdateRepeat;
import org.example.ai_api.Bean.Model.ResultData;
import org.example.ai_api.Bean.WebRequest.GlossaryRequest;
import org.example.ai_api.Bean.WebRequest.GlossaryUpdate;
import org.example.ai_api.Config.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/glossary")
public class GlossaryController {

    @Qualifier("SyncWebClient")
    @Autowired
    private WebClient  webClient;
    @Value("${ai.categories.glossary}")
    private String url ;


    @PostMapping
    public ResultData<GlossaryRepeat> glossary(@RequestBody GlossaryRequest request) {
        GlossaryRepeat glossaryRepeat =  webClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GlossaryRepeat.class)
                .block();
        return ResultData.success(glossaryRepeat);
    }

    @PostMapping("/glossary_update")
    public ResultData<GlossaryUpdateRepeat>  glossaryUpdate(@RequestBody GlossaryUpdate request) {
        GlossaryUpdateRepeat glossaryUpdateRepeat =  webClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GlossaryUpdateRepeat.class)
                .block();
        return ResultData.success(glossaryUpdateRepeat);
    }

}
