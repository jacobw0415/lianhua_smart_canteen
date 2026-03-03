package com.lianhua.erp.config;

import com.lianhua.erp.domain.Permission;
import com.lianhua.erp.domain.Role;
import com.lianhua.erp.domain.User;
import com.lianhua.erp.repository.PermissionRepository;
import com.lianhua.erp.repository.RoleRepository;
import com.lianhua.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    @Bean
    @Transactional // 🌿 確保關聯儲存時處於同一事務
    CommandLineRunner initSystemData() {
        return args -> {
            log.info("🌿 開始初始化 Lianhua ERP v2.7 系統基礎數據...");

            // 1. 初始化顆粒度權限 (Permissions)，配合 @PreAuthorize("hasAuthority('...')") 使用
            // 系統管理
            Permission pUserView = createPermissionIfNotFound("user:view", "查看使用者", "系統管理");
            Permission pUserEdit = createPermissionIfNotFound("user:edit", "編輯使用者", "系統管理");
            Permission pRoleView = createPermissionIfNotFound("role:view", "查看角色與權限", "系統管理");
            Permission pRoleEdit = createPermissionIfNotFound("role:edit", "編輯角色與權限", "系統管理");
            // 訂單
            Permission pOrderView = createPermissionIfNotFound("order:view", "查看訂單", "訂單管理");
            Permission pOrderEdit = createPermissionIfNotFound("order:edit", "處理訂單", "訂單管理");
            // 進貨
            Permission pPurchaseView = createPermissionIfNotFound("purchase:view", "查看進貨", "進貨管理");
            Permission pPurchaseEdit = createPermissionIfNotFound("purchase:edit", "編輯進貨", "進貨管理");
            Permission pSupplierView = createPermissionIfNotFound("supplier:view", "查看供應商", "進貨管理");
            Permission pPaymentView = createPermissionIfNotFound("payment:view", "查看付款紀錄", "進貨管理");
            Permission pApView = createPermissionIfNotFound("ap:view", "查看應付帳款", "進貨管理");
            // 銷售
            Permission pSaleView = createPermissionIfNotFound("sale:view", "查看銷售", "銷售管理");
            Permission pSaleEdit = createPermissionIfNotFound("sale:edit", "編輯銷售", "銷售管理");
            Permission pOrderCustomerView = createPermissionIfNotFound("order_customer:view", "查看訂單客戶", "銷售管理");
            Permission pReceiptView = createPermissionIfNotFound("receipt:view", "查看收款紀錄", "銷售管理");
            Permission pArView = createPermissionIfNotFound("ar:view", "查看應收帳款", "銷售管理");
            // 費用
            Permission pExpenseView = createPermissionIfNotFound("expense:view", "查看費用", "費用管理");
            Permission pExpenseEdit = createPermissionIfNotFound("expense:edit", "編輯費用", "費用管理");
            Permission pExpenseCategoryView = createPermissionIfNotFound("expense_category:view", "查看費用分類", "費用管理");
            // 產品
            Permission pProductView = createPermissionIfNotFound("product:view", "查看產品", "產品管理");
            Permission pProductEdit = createPermissionIfNotFound("product:edit", "編輯產品", "產品管理");
            Permission pProductCategoryView = createPermissionIfNotFound("product_category:view", "查看商品分類", "產品管理");
            // 人員 / 員工
            Permission pEmployeeView = createPermissionIfNotFound("employee:view", "查看員工", "人力資源");
            // 報表
            Permission pReportView = createPermissionIfNotFound("report:view", "查看報表", "報表");
            // 儀表板
            Permission pDashboardView = createPermissionIfNotFound("dashboard:view", "查看儀表板", "儀表板");
            // 通知中心
            Permission pNotificationView = createPermissionIfNotFound("notification:view", "查看通知中心", "通知");

            // 2. 清理非標準角色（name 非 ROLE_ 開頭），須在 transaction 內執行以載入 Role.users（lazy）
            transactionTemplate.executeWithoutResult(status -> {
                roleRepository.findAll().stream()
                        .filter(r -> r.getName() != null && !r.getName().startsWith("ROLE_"))
                        .toList()
                        .forEach(r -> {
                            r.getUsers().forEach(u -> {
                                u.getRoles().remove(r);
                                userRepository.save(u);
                            });
                            roleRepository.delete(r);
                            log.info("   -> 移除非標準角色: {}", r.getName());
                        });
            });

            // 3. 初始化角色並同步權限（每次啟動確保 ADMIN 全權限、USER 僅檢視）
            Set<Permission> allPermissions = Set.of(
                    pUserView, pUserEdit, pRoleView, pRoleEdit,
                    pOrderView, pOrderEdit,
                    pPurchaseView, pPurchaseEdit, pSupplierView, pPaymentView, pApView,
                    pSaleView, pSaleEdit, pOrderCustomerView, pReceiptView, pArView,
                    pExpenseView, pExpenseEdit, pExpenseCategoryView,
                    pProductView, pProductEdit, pProductCategoryView,
                    pEmployeeView,
                    pReportView, pDashboardView,
                    pNotificationView
            );
            Set<Permission> viewOnlyPermissions = Set.of(
                    pOrderView,
                    pPurchaseView, pSupplierView, pPaymentView, pApView,
                    pSaleView, pOrderCustomerView, pReceiptView, pArView,
                    pExpenseView, pExpenseCategoryView,
                    pProductView, pProductCategoryView,
                    pEmployeeView,
                    pReportView, pDashboardView,
                    pNotificationView
            );

            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role r = Role.builder()
                        .name("ROLE_ADMIN")
                        .description("系統管理員：\n" +
                                "擁有系統全功能權限，負責帳號、角色與權限配置，並具備所有模組之新增、修改、作廢與刪除權。掌握核心設定，可全面操控系統運作、維護數據完整與變更各項參數。")
                        .permissions(new HashSet<>(allPermissions))
                        .build();
                log.info("   -> 建立角色: ROLE_ADMIN");
                return roleRepository.save(r);
            });
            adminRole.setDescription("系統管理員：\n" +
                    "擁有系統全功能權限，負責帳號、角色與權限配置，並具備所有模組之新增、修改、作廢與刪除權。掌握核心設定，可全面操控系統運作、維護數據完整與變更各項參數。");
            syncRolePermissions(adminRole, allPermissions);
            roleRepository.save(adminRole);

            Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
                Role r = Role.builder()
                        .name("ROLE_USER")
                        .description("一般使用者：\n" +
                                "僅能查看訂單、進銷存、費用、產品及儀表板報表等營運資料，嚴禁變更系統設定、帳號或角色。不具備任何資料之新增、修改、作廢與刪除權，確保數據僅供讀取與分析。")
                        .permissions(new HashSet<>(viewOnlyPermissions))
                        .build();
                log.info("   -> 建立角色: ROLE_USER");
                return roleRepository.save(r);
            });
            userRole.setDescription("一般使用者：\n" +
                    "僅能查看訂單、進銷存、費用、產品及儀表板報表等營運資料，嚴禁變更系統設定、帳號或角色。不具備任何資料之新增、修改、作廢與刪除權，確保數據僅供讀取與分析。");
            syncRolePermissions(userRole, viewOnlyPermissions);
            roleRepository.save(userRole);
            log.info("   -> ROLE_USER 權限已同步: {}", userRole.getPermissions().stream()
                    .map(Permission::getName).sorted().toList());

            // 4. 初始化管理員帳號 (Admin User)
            // 必須確保具備 ROLE_ADMIN，JwtUtils 才能在 Claim 加入正確的 roles
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("系統管理員")
                        .email("admin@lianhua.com")
                        .enabled(true)
                        .roles(new HashSet<>()) // 初始化集合避免 NullPointerException
                        .build();

                admin.addRole(adminRole); // 建立多對多關聯
                userRepository.save(admin);
                log.info("✅ 初始管理員帳號 'admin' 建立完成 (預設密碼: admin123)");
            }

            log.info("🌿 系統數據初始化檢查結束。");
        };
    }

    /**
     * 同步角色權限：清空後再寫入，確保 role_permissions 關聯表被正確更新。
     * 使用 clear + addAll 讓 Hibernate 正確追蹤並持久化關聯，避免僅 set 新 Set 時 join table 未更新。
     */
    private void syncRolePermissions(Role role, Set<Permission> permissions) {
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
    }

    private Permission createPermissionIfNotFound(String name, String description, String module) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    Permission p = Permission.builder()
                            .name(name)
                            .description(description)
                            .module(module)
                            .build();
                    log.info("   -> 建立權限: {}", name);
                    return permissionRepository.save(p);
                });
    }

}