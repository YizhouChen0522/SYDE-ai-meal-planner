package com.syde.mealplanner.config.typehandler;

import com.syde.mealplanner.util.JsonMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractJsonTypeHandler<T> extends BaseTypeHandler<T> {

    private final TypeReference<T> typeReference;

    protected AbstractJsonTypeHandler(TypeReference<T> typeReference) {
        this.typeReference = typeReference;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, JsonMapper.getObjectMapper().writeValueAsString(parameter));
        } catch (JacksonException exception) {
            throw new TypeException("Could not convert value to JSON.", exception);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private T parseJson(String json) throws SQLException {
        if (json == null) {
            return null;
        }

        try {
            return JsonMapper.getObjectMapper().readValue(json, typeReference);
        } catch (JacksonException exception) {
            throw new SQLException("Could not convert JSON column value.", exception);
        }
    }
}
