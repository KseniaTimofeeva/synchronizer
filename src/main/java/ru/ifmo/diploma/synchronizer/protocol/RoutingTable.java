package ru.ifmo.diploma.synchronizer.protocol;

import java.util.Map;

/**
 * Created by ksenia on 25.05.2017.
 */
public class RoutingTable extends HandshakeMessage {
    private Map<String, Credentials> authorizationTable;

    public RoutingTable(String fromAddr, Map<String, Credentials> authorizationTable) {
        super(fromAddr);
        this.authorizationTable = authorizationTable;
    }

    public Map<String, Credentials> getAuthorizationTable() {
        return authorizationTable;
    }
}
