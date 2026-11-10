package com.example.mapper;

import com.example.pojo.dto.UserProfileUpdateDTO;
import com.example.pojo.vo.UserProfileVO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserProfileMapper {
    @Select("SELECT EXISTS(SELECT 1 FROM `user` WHERE base_user_id=#{id})")
    boolean hasUserRole(Integer id);
    @Select("SELECT EXISTS(SELECT 1 FROM admin WHERE base_user_id=#{id})")
    boolean hasAdminRole(Integer id);
    @Select("""
        SELECT b.id,b.account,b.username,b.avatar,b.gender,b.phone,b.create_time AS createTime,
               p.school,p.college,p.student_number AS studentNumber,'USER' AS role
        FROM base_user b LEFT JOIN user_campus_profile p ON p.base_user_id=b.id
        WHERE b.id=#{id} AND EXISTS(SELECT 1 FROM `user` u WHERE u.base_user_id=b.id)
        """)
    UserProfileVO selectProfile(Integer id);
    @Update("""
        <script>
        UPDATE base_user SET username=#{dto.username},phone=#{dto.phone},gender=#{dto.gender}
        <if test="avatar != null">,avatar=#{avatar}</if>
        WHERE id=#{id} AND login_status=1
        </script>
        """)
    int updateBasic(@Param("id") Integer id,@Param("dto") UserProfileUpdateDTO dto,@Param("avatar") String avatar);
    @Insert("""
        INSERT INTO user_campus_profile(base_user_id,school,college,student_number)
        VALUES(#{id},#{dto.school},#{dto.college},#{dto.studentNumber})
        ON DUPLICATE KEY UPDATE school=VALUES(school),college=VALUES(college),student_number=VALUES(student_number)
        """)
    int upsertCampus(@Param("id") Integer id,@Param("dto") UserProfileUpdateDTO dto);
    @Update("UPDATE base_user SET password=#{newPassword} WHERE id=#{id} AND password=#{oldPassword} AND login_status=1")
    int changePassword(@Param("id") Integer id,@Param("oldPassword") String oldPassword,@Param("newPassword") String newPassword);
}
