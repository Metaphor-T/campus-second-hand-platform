package com.example.pojo.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.example.pojo.entity.BaseUser;

@Data
public class AccountUpdateDTO {
    @NotNull @Positive private Integer id;
    @Size(min=1,max=20) private String username;
    @Pattern(regexp="^$|^1[0-9]{10}$") private String phone;
    @Min(1) @Max(2) private Integer gender;
    @Min(1) @Max(2) private Integer loginStatus;
    public BaseUser entity() { return BaseUser.builder().id(id).username(username).phone(phone).gender(gender).loginStatus(loginStatus).build(); }
}
