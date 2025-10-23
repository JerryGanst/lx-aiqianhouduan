package org.example.ai_api.Service;

import org.example.ai_api.Bean.Model.DeptItem;
import org.example.ai_api.Persistence.Repository.DepartmentItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentItemRepository departmentItemRepository;

    /**
     * 给定任意部门，返回：最近处级(04) + 该处级下直属部级(05)列表。
     */
    public List<DeptItem> listChuWithBuChildren(String userDeptId) {
        DeptItem chu = departmentItemRepository.findNearestChu(userDeptId);
        if (chu == null) {
            return Collections.emptyList();
        }
        List<DeptItem> result = new ArrayList<>();
        result.add(chu);
        result.addAll(departmentItemRepository.listDirectChildren(chu.getId())); // 仅05

        Map<String, DeptItem> uniq = new LinkedHashMap<>();
        for (DeptItem it : result) {
            if (it != null && it.getId() != null) {
                uniq.putIfAbsent(it.getId(), it);
            }
        }
        return new ArrayList<>(uniq.values());
    }
}
