package edu.rit.cs.steven_landau.shiftmobile;

import java.io.Serializable;

/**
 * Created by Steven Landau on 10/6/2016.
 *
 * SendCard lets the server know that we want to send
 * a text message. The text message will contain a message and the contact info of that person.
 */
public class SendCard implements Serializable{
    private String msg;
    private String number;
    private String name;

    public SendCard(String msg, String number, String name) {
        this.msg = msg;
        this.number = number;
        this.name = name;
    }

    public String getMsg() {
        return this.msg;
    }

    public String getNumber() { return this.number; }

    public String getName() { return this.name; }
}
