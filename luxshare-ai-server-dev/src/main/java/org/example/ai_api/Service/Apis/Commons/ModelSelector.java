package org.example.ai_api.Service.Apis.Commons;

import org.example.ai_api.Config.AIConfig;
import org.example.ai_api.Exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 模型选择与校验：
 * - 校验模型下标有效性
 * - 根据场景选择对应模型（如法务专题特殊映射）
 * @author 10353965
 */
@Component
public class ModelSelector {
    @Autowired
    private AIConfig aiConfig;

    /**
     * 校验模型下标是否合法
     * @param model 模型下标
     */
    public void validateModelIndex(int model) {
        if (model > aiConfig.getModels().size()) {
            throw new NotFoundException("模型不存在");
        }
    }

    /**
     *  根据模型下标获取模型名称（非知识库问答）
     * @param model  模型下标
     * @return  模型名称
     */
    public String getModel(int model){
        return aiConfig.getModels().get(model);
    }

    /**
     *  根据模型下标获取模型名称（知识库问答）
     * @param model  模型下标
     * @param type    场景类型
     * @return    模型名称
     */
    public String getQueryModel(int model,String type){
        if ("法务专题".equals(type)) {
            //法务部分模型临时写死
            return "reasoning";
        } else {
            return aiConfig.getModels().get(model);
        }
    }

}
