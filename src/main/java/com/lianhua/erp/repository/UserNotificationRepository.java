package com.lianhua.erp.repository;

import com.lianhua.erp.domain.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    // 獲取特定使用者的未讀通知，並同時抓取 Notification 實體 (避免 N+1 問題)
    @Query("SELECT un FROM UserNotification un JOIN FETCH un.notification " +
            "WHERE un.userId = :userId AND un.isRead = false " +
            "ORDER BY un.notification.createdAt DESC")
    List<UserNotification> findUnreadByUserId(@Param("userId") Long userId);

    // 快速統計未讀數量
    long countByUserIdAndIsReadFalse(Long userId);

    // 獲取使用者的所有通知 (分頁建議未來再加)
    @Query("SELECT un FROM UserNotification un JOIN FETCH un.notification " +
            "WHERE un.userId = :userId " +
            "ORDER BY un.notification.createdAt DESC")
    List<UserNotification> findAllByUserId(@Param("userId") Long userId);
}