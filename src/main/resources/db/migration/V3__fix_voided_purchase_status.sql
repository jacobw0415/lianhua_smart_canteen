-- ============================================================
-- 修正已作廢進貨單的錯誤狀態
-- ============================================================
-- 問題：先前作廢進貨單時，系統會將 status 強制改為 PENDING
-- 修正：根據已作廢的付款記錄來恢復原本的付款狀態（PARTIAL 或 PAID）
-- ============================================================

-- 步驟 1：更新有已作廢付款記錄的進貨單狀態
-- 如果已作廢付款總金額 = 進貨單總金額（允許小數點誤差），則狀態應為 PAID
UPDATE purchases p
SET p.status = 'PAID',
    p.updated_at = CURRENT_TIMESTAMP
WHERE p.record_status = 'VOIDED'
  AND p.status = 'PENDING'
  AND EXISTS (
    SELECT 1
    FROM payments pmt
    WHERE pmt.purchase_id = p.id
      AND pmt.status = 'VOIDED'
  )
  AND ABS((
    SELECT COALESCE(SUM(pmt.amount), 0)
    FROM payments pmt
    WHERE pmt.purchase_id = p.id
      AND pmt.status = 'VOIDED'
  ) - p.total_amount) < 0.01;  -- 允許 0.01 的小數點誤差

-- 步驟 2：更新有已作廢付款記錄但未全額付款的進貨單狀態
-- 如果已作廢付款總金額 > 0 且 < 進貨單總金額，則狀態應為 PARTIAL
UPDATE purchases p
SET p.status = 'PARTIAL',
    p.updated_at = CURRENT_TIMESTAMP
WHERE p.record_status = 'VOIDED'
  AND p.status = 'PENDING'
  AND EXISTS (
    SELECT 1
    FROM payments pmt
    WHERE pmt.purchase_id = p.id
      AND pmt.status = 'VOIDED'
  )
  AND (
    SELECT COALESCE(SUM(pmt.amount), 0)
    FROM payments pmt
    WHERE pmt.purchase_id = p.id
      AND pmt.status = 'VOIDED'
  ) > 0
  AND (
    SELECT COALESCE(SUM(pmt.amount), 0)
    FROM payments pmt
    WHERE pmt.purchase_id = p.id
      AND pmt.status = 'VOIDED'
  ) < p.total_amount;

-- ============================================================
-- 驗證查詢（執行前可先執行此查詢查看需要修正的記錄數）
-- ============================================================
-- 執行前查詢：查看需要修正的記錄
-- SELECT 
--     '修正前' AS stage,
--     COUNT(*) AS total_voided_pending,
--     SUM(CASE WHEN EXISTS (
--         SELECT 1 FROM payments pmt 
--         WHERE pmt.purchase_id = p.id AND pmt.status = 'VOIDED'
--     ) THEN 1 ELSE 0 END) AS has_voided_payments
-- FROM purchases p
-- WHERE p.record_status = 'VOIDED' AND p.status = 'PENDING';

-- 執行後查詢：驗證修正結果
-- SELECT 
--     '修正後' AS stage,
--     status,
--     COUNT(*) AS count
-- FROM purchases
-- WHERE record_status = 'VOIDED'
-- GROUP BY status;

-- ============================================================
-- 說明：
-- ============================================================
-- 1. 此腳本會修正所有已作廢（record_status = 'VOIDED'）且狀態為 PENDING 的進貨單
-- 2. 根據已作廢付款記錄的總金額來判斷原本的付款狀態：
--    - 如果已作廢付款總金額 = 進貨單總金額（允許 0.01 誤差）→ 狀態改為 PAID
--    - 如果已作廢付款總金額 > 0 且 < 進貨單總金額 → 狀態改為 PARTIAL
--    - 如果沒有已作廢付款記錄 → 保持 PENDING（正確）
-- 3. 此修正不會影響其他正常記錄（record_status = 'ACTIVE' 的記錄）
-- 4. 執行此腳本後，建議執行上述驗證查詢確認修正結果

