package org.example.ai_api.Bean.WebRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@ToString
public class DepartmentFiles {
    /**
     * 文件夹ID
     */
    @JsonProperty("folderId")
    private String folderId;
    /**
     *  部门id
     */
    @JsonProperty("departmentId")
    private String departmentId;
    /**
     *  二级文件夹id
     */
    @JsonProperty("tagList")
    private List<String> tagList;
    /**
     *  用户ID
     */
    @JsonProperty("userId")
    private String userId;
    /**
     *  排序类型
     */
    @JsonProperty("sortType")
    String sortType;
    /**
     *  是否升序
     */
    @JsonProperty("increase")
    boolean increase;
    /**
     *  关键字
     */
    @JsonProperty("keywords")
    String keywords;
    /**
     * 页码
     */
    @JsonProperty("page")
    int page = 1;
    /**
     *  每页条数
     */
    @JsonProperty("pageSize")
    int pageSize = 10;

}
