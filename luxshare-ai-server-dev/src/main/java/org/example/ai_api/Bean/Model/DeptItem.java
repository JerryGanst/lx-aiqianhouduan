package org.example.ai_api.Bean.Model;

public interface DeptItem {
    String getId();
    String getName();
    String getLevelCode();
    default boolean getLevel(){
        return "04".equals(getLevelCode());
    }
}
