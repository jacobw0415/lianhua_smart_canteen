package com.lianhua.erp.repository;

import com.lianhua.erp.domain.PasswordResetToken;
import com.lianhua.erp.domain.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // 透過 Token 字串查找
    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Transactional
    // 透過使用者查找（用於清理舊請求）
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId") // 🌿 直接用 ID 刪除最準確
    void deleteByUserId(@Param("userId") Long userId);
}