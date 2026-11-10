package com.example.mapper;

import com.example.campus.CampusVerificationRecord;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CampusVerificationMapper {
    @Select("SELECT * FROM campus_verification WHERE base_user_id=#{id} ORDER BY id DESC LIMIT 1")
    CampusVerificationRecord latest(Integer id);

    @Insert("""
        INSERT INTO campus_verification(base_user_id,request_id,school,student_number,provider,status)
        VALUES(#{baseUserId},#{requestId},#{school},#{studentNumber},#{provider},'PENDING')
        """)
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(CampusVerificationRecord record);

    @Update("""
        UPDATE campus_verification SET status=#{status},evidence_reference=#{evidenceReference},
          checked_at=#{checkedAt},expires_at=#{expiresAt}
        WHERE id=#{id} AND base_user_id=#{baseUserId} AND status='PENDING'
        """)
    int complete(CampusVerificationRecord record);

    @Update("""
        UPDATE campus_verification SET status='INVALIDATED'
        WHERE base_user_id=#{id} AND status IN ('PENDING','VERIFIED','REJECTED')
          AND (NOT(school <=> #{school}) OR NOT(student_number <=> #{studentNumber}))
        """)
    int invalidateChanged(@Param("id") Integer id,@Param("school") String school,@Param("studentNumber") String studentNumber);
}
