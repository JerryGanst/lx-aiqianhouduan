package org.example.ai_api.Bean.Entity;

import lombok.*;

import javax.persistence.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Entity
@Table(name = "V_ZSHR_YGJBXX_820",schema = "EDH")
public class UserInfo {
    @Id
    @Column(name = "CPF01")
    private String id;//用户工号

    @Column(name = "CPF02")
    private String name;//真实姓名

    @Column(name = "O_STEXT")
    private String department;//所属部门

    @Column(name = "ORGEH")
    private String departmentId;//所属部门id

    @Transient
    private boolean personLevel;//是否为处级干部
}
