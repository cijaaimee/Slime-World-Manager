package com.grinderwolf.swm.plugin.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@Getter
@ConfigSerializable
public class DatasourcesConfig {

    @Setting("file") private final FileConfig fileConfig = new FileConfig();
    @Setting("mysql") private final MysqlConfig mysqlConfig = new MysqlConfig();
    @Setting("mongodb") private final MongoDBConfig mongoDbConfig = new MongoDBConfig();

    @Getter
    @ConfigSerializable
    public static class MysqlConfig {

        @Setting("enabled") private final boolean enabled = false;

        @Setting("host") private final String host = "127.0.0.1";
        @Setting("port") private final int port = 3306;

        @Setting("username") private final String username = "slimeworldmanager";
        @Setting("password") private final String password = "";

        @Setting("database") private final String database = "slimeworldmanager";
    }

    @Getter
    @ConfigSerializable
    public static class MongoDBConfig {

        @Setting("enabled") private final boolean enabled = false;

        @Setting("host") private final String host = "127.0.0.1";
        @Setting("port") private final int port = 27017;

        @Setting("auth") private final String authSource = "admin";
        @Setting("username") private final String username = "slimeworldmanager";
        @Setting("password") private final String password = "";

        @Setting("database") private final String database = "slimeworldmanager";
        @Setting("collection") private final String collection = "worlds";
    }

    @Getter
    @ConfigSerializable
    public static class FileConfig {

        @Setting("path") private final String path = "slime_worlds";

    }
}
