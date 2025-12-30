package com.lianhua.erp.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "採購明細實體")
public class PurchaseItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(description = "明細 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_id", nullable = false)
  @Schema(description = "採購單")
  private Purchase purchase;

  @Column(nullable = false, length = 120)
  @Schema(description = "進貨項目")
  private String item;

  @Column(length = 20, nullable = false)
  @Schema(description = "數量單位（顯示用，例如：斤、箱、盒）")
  private String unit;

  @Column(nullable = false)
  @Schema(description = "數量")
  private Integer qty;

  @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
  @Schema(description = "單價")
  private BigDecimal unitPrice;

  @Column(precision = 10, scale = 2, nullable = false)
  @Schema(description = "小計（不含稅）")
  private BigDecimal subtotal;

  @Column(length = 255)
  @Schema(description = "備註")
  private String note;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
