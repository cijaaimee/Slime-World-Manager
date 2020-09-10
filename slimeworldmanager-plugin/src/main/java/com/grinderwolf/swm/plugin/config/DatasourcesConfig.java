package com.grinderwolf.swm.plugin.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@Getter
@ConfigSerializable
public class DatasourcesConfig {

    @Setting("file") private FileConfig fileConfig = new FileConfig();
    @Setting("mysql") private MysqlConfig mysqlConfig = new MysqlConfig();
    @Setting("mongodb") private MongoDBConfig mongoDbConfig = new MongoDBConfig();

    @Getter
    @ConfigSerializable
    public static class MysqlConfig {

        @Setting("enabled") private boolean enabled = false;

        @Setting("host") private String host = "127.0.0.1";
        @Setting("port") private int port = 3306;

        @Setting("username") private String username = "slimeworldmanager";
        @Setting("password") private String password = "";

        @Setting("database") private String database = "slimeworldmanager";
    }

    @Getter
    @ConfigSerializable
    public static class MongoDBConfig {

        @Setting("enabled") private boolean enabled = false;

        @Setting("host") private String host = "127.0.0.1";
        @Setting("port") private int port = 27017;

        @Setting("auth") private String authSource = "admin";
        @Setting("username") private String username = "slimeworldmanager";
        @Setting("password") private String password = "";

        @Setting("database") private String database = "slimeworldmanager";
        @Setting("collection") private String collection = "worlds";

        @Setting("uri") private String uri = "";
    }

    @Getter
    @ConfigSerializable
    public static class FileConfig {

        @Setting("path") private String path = "slime_worlds";

    }
}
