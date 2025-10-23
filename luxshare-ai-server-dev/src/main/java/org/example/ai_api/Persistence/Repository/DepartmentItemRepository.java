package org.example.ai_api.Persistence.Repository;

import org.example.ai_api.Bean.Entity.DepartmentItem;
import org.example.ai_api.Bean.Model.DeptItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepartmentItemRepository extends Repository<DepartmentItem, String> {
    /** 取部门层级代码（处=04、部=05 …）；只要一条 */
    @Query(value = "SELECT ZHR900101 FROM EDH.V_ZZJG_820 " +
            "WHERE OBJID = :objId AND ROWNUM = 1",
            nativeQuery = true)
    String findLevelCode(@Param("objId") String objId);

    /** 取某父部门的直接下属里，级别=05(部) 的一层列表（只返回 id/name 两列） */
    @Query(value = "SELECT OBJID AS id, STEXT AS name " +
            "FROM EDH.V_ZZJG_820 " +
            "WHERE SOBID = :pid AND ZHR900101 = '05' " +
            "ORDER BY STEXT",
            nativeQuery = true)
    List<DeptItem> findDirectBuChildren(@Param("pid") String parentObjId);

    /** 取某部门 */
    @Query(value =
            "SELECT OBJID AS id, STEXT AS name, ZHR900101 AS levelCode " +
                    "FROM EDH.V_ZZJG_820 " +
                    "WHERE OBJID = :id " +
                    "  AND ROWNUM = 1",
            nativeQuery = true)
    DeptItem findDeptItem(@Param("id") String id);

    /** 从当前部门向上追溯，找到最近的“处级(04)”祖先（可能就是自己） */
    @Query(value =
            "SELECT * FROM ( " +
                    "  SELECT z.OBJID AS id, z.STEXT AS name, z.ZHR900101 AS levelCode " +
                    "  FROM EDH.V_ZZJG_820 z " +
                    "  START WITH z.OBJID = :deptId " +
                    "  CONNECT BY PRIOR z.SOBID = z.OBJID " +
                    ") WHERE levelCode = '04' AND ROWNUM = 1",
            nativeQuery = true)
    DeptItem findNearestChu(@Param("deptId") String deptId);

    /** 列出某父部门的所有“直属下级”（只返回部级） */
    @Query(value =
            "SELECT OBJID AS id, STEXT AS name, ZHR900101 AS levelCode " +
                    "FROM EDH.V_ZZJG_820 " +
                    "WHERE SOBID = :pid " +
                    "AND ZHR900101 = '05' " +
                    "ORDER BY STEXT",
            nativeQuery = true)
    List<DeptItem> listDirectChildren(@Param("pid") String parentObjId);
}
