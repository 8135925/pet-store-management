package com.petshop.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** SQLite TEXT <-> LocalDateTime（yyyy-MM-dd HH:mm:ss） */
@MappedTypes(LocalDateTime.class)
public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime p, JdbcType jdbcType) throws SQLException {
        ps.setString(i, p.format(FMT));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String column) throws SQLException {
        return parse(rs.getString(column));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int column) throws SQLException {
        return parse(rs.getString(column));
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int column) throws SQLException {
        return parse(cs.getString(column));
    }

    private LocalDateTime parse(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(s.trim(), FMT);
    }
}
