package com.cineman.dao;

import org.springframework.jdbc.core.JdbcTemplate;

/** Lớp DAO chung: cung cấp kết nối dùng chung (JdbcTemplate trên DataSource của Spring) cho các DAO con. */
public abstract class DAO {

    protected final JdbcTemplate jdbcTemplate;

    protected DAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
