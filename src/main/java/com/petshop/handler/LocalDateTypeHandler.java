package com.petshop.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** SQLite TEXT <-> LocalDate（yyyy-MM-dd） */
@MappedTypes(LocalDate.class)
public class LocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDate p, JdbcType jdbcType) throws SQLException {
        ps.setString(i, p.format(FMT));
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, String column) throws SQLException {
        return parse(rs.getString(column));
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, int column) throws SQLException {
        return parse(rs.getString(column));
    }

    @Override
    public LocalDate getNullableResult(CallableStatement cs, int column) throws SQLException {
        return parse(cs.getString(column));
    }

    private LocalDate parse(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(s.trim(), FMT);
    }
}
