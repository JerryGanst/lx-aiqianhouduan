package org.example.ai_api.Utils;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

public class TypeDetector {
    private static final Map<String, String> TYPES = new HashMap<>();

    static {
        TYPES.put("sample","通用模式");
        TYPES.put("hr","人资行政专题");
        TYPES.put("it","IT专题");
        TYPES.put("law","法务专题");
        TYPES.put("board","董办专题");
        TYPES.put("tran","翻译");
        TYPES.put("final","总结");
    }

    /**
     * 根据type获取对应的中文名称
     * @param type  type
     * @return  中文名称
     */
    public static String getType(String type){
        if(type == null || type.isEmpty()){
            return null;
        }
        return TYPES.getOrDefault(type,null);
    }
}
