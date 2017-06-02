package ru.ifmo.diploma.synchronizer.protocol.handshake;

/**
 * Created by ksenia on 23.05.2017.
 */
public class Credentials extends HandshakeMessage {
    private String from;
    private String login;
    private String password;

    public Credentials(String fromAddr, String login, String password) {
        super(fromAddr);
        this.from = fromAddr;
        this.login = login;
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Credentials that = (Credentials) o;

        if (login != null ? !login.equals(that.login) : that.login != null) return false;
        return password != null ? password.equals(that.password) : that.password == null;
    }

    @Override
    public int hashCode() {
        int result = login != null ? login.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "from='" + from + '\'' +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
