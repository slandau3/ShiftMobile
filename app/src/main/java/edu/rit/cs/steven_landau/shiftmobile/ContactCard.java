package edu.rit.cs.steven_landau.shiftmobile;

import java.io.Serializable;

/**
 * Created by Steven Landau on 10/12/2016.
 */
public class ContactCard implements Serializable {

    private String name;
    private String number;

    public ContactCard(String name, String number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return this.name;
    }

    public String getNumber() {
        return this.number;
    }
}
