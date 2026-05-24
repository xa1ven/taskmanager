package com.taskmanager.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

fun configureDatabase() {
    val dbUrl = System.getenv("DB_URL") ?: error("DB_URL environment variable is not set")
    val dbUser = System.getenv("DB_USER") ?: error("DB_USER environment variable is not set")
    val dbPassword = System.getenv("DB_PASSWORD") ?: error("DB_PASSWORD environment variable is not set")

    val config = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 600_000
        connectionTimeout = 30_000
        maxLifetime = 1_800_000
    }

    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)
    println("Database connected: $dbUrl")
}
