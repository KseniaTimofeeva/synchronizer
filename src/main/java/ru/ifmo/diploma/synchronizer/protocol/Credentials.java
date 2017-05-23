package ru.ifmo.diploma.synchronizer.protocol;

import java.io.Serializable;

/**
 * Created by ksenia on 23.05.2017.
 */
public class Credentials implements Serializable {
        private String login;
        private String password;

        public Credentials(String login, String password) {
            this.login = login;
            this.password = password;
        }
}
