package com.example.pojo.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.example.pojo.entity.BaseUser;

@Data
public class AccountCreateDTO {
    @NotBlank @Size(min=3,max=30) private String account;
    @NotBlank @Size(min=6,max=64) private String password;
    @NotBlank @Size(max=20) private String username;
    @Pattern(regexp="^$|^1[0-9]{10}$") private String phone;
    @Min(1) @Max(2) private Integer gender;
    public BaseUser entity() { return BaseUser.builder().account(account.strip()).password(password).username(username.strip()).phone(phone).gender(gender).build(); }
}
