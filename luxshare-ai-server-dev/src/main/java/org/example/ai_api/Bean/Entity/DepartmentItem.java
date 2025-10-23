package org.example.ai_api.Bean.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "V_ZZJG_820", schema = "EDH")
public class DepartmentItem {
    /** 部门ID（SAP），视图里是 NUMC；这里用 String 防止前导零问题 */
    @Id
    @Column(name = "OBJID")
    private String objId;

    /** 上级部门ID（SAP） */
    @Column(name = "SOBID")
    private String parentId;

    /** 部门名称（简） */
    @Column(name = "STEXT")
    private String name;

    /** 部门层级代码：06课/05部/04处/... */
    @Column(name = "ZHR900101")
    private String levelCode;

}
