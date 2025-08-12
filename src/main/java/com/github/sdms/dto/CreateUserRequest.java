package com.github.sdms.dto;

import com.github.sdms.model.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreateUserRequest {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "密码（确认密码）")
    private String password1;

    @Schema(description = "确认密码")
    private String password2;

    @Schema(description = "馆点代码")
    private String libraryCode;

    @Schema(description = "用户角色（管理员、馆员、读者）")
    private RoleType roleType;

}
