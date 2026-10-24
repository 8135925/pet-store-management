package com.petshop.module.card;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 核销扣次 SQL 内存库真跑（设计 §15.1 第三条）。
 * 验证：余额 0 影响 0 行；有余额影响 1 行且赠次优先（BR-01）。
 */
public class CardDeductSqlTest {

    private Connection conn;

    private static final String DEDUCT =
            "UPDATE customer_card "
            + "SET used_gift = used_gift + CASE WHEN (gift_qty - used_gift) > 0 THEN 1 ELSE 0 END, "
            + "    used_paid = used_paid + CASE WHEN (gift_qty - used_gift) > 0 THEN 0 ELSE 1 END "
            + "WHERE id = ? AND status = 'ACTIVE' AND expire_on >= ? "
            + "  AND (paid_qty - used_paid + gift_qty - used_gift) > 0";

    @BeforeEach
    public void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE customer_card (id INTEGER PRIMARY KEY, card_no TEXT,"
                    + "customer_id INTEGER, status TEXT, paid_qty INTEGER, gift_qty INTEGER,"
                    + "used_paid INTEGER, used_gift INTEGER, expire_on TEXT)");
            st.execute("INSERT INTO customer_card VALUES (1,'C1',1,'ACTIVE',10,2,0,0,'2099-12-31')");
            st.execute("INSERT INTO customer_card VALUES (2,'C2',1,'ACTIVE',10,0,10,0,'2099-12-31')");
            st.execute("INSERT INTO customer_card VALUES (3,'C3',1,'VOID',10,2,0,0,'2099-12-31')");
            st.execute("INSERT INTO customer_card VALUES (4,'C4',1,'ACTIVE',10,2,0,0,'2020-01-01')");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        conn.close();
    }

    private int deduct(int cardId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(DEDUCT)) {
            ps.setInt(1, cardId);
            ps.setString(2, "2026-10-24");
            return ps.executeUpdate();
        }
    }

    private int[] used(int cardId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT used_paid, used_gift FROM customer_card WHERE id = " + cardId)) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            return new int[]{rs.getInt(1), rs.getInt(2)};
        }
    }

    @Test
    public void availableCardDeductsGiftFirst() throws Exception {
        assertEquals(1, deduct(1));
        assertEquals(0, used(1)[0]);
        assertEquals(1, used(1)[1]);
    }

    @Test
    public void zeroBalanceAffectsZeroRows() throws Exception {
        assertEquals(0, deduct(2));
        assertEquals(10, used(2)[0]);
        assertEquals(0, used(2)[1]);
    }

    @Test
    public void voidAndExpiredCardsAffectZeroRows() throws Exception {
        assertEquals(0, deduct(3));
        assertEquals(0, deduct(4));
    }

    @Test
    public void giftFirstThenPaid() throws Exception {
        deduct(1);
        deduct(1);
        deduct(1);
        assertEquals(1, used(1)[0]);
        assertEquals(2, used(1)[1]);
    }
}
