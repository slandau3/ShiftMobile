package edu.rit.cs.steven_landau.shiftmobile;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Steven Landau on 10/12/2016.
 *
 * This is a contact pulled from the phone. We may or may already
 * have them in our conversations. The point of this class is to
 * have all of our phones contacts stored on the client side
 * so that we can start a conversation (without having to enter in the exact number)
 * with anyone. The phone will parse it's contacts and send this class
 * which contains an arraylist full of Contact cards.
 */
public class RetrievedContacts extends ArrayList<ContactCard> implements Serializable {

    private static ArrayList<ContactCard> cc;

    public RetrievedContacts() {
        cc = new ArrayList<>();
    }

    public void reset() {
        cc.clear();
    }

    public void addContactCard(ContactCard cCard) {
        cc.add(cCard);
    }
    public ArrayList<ContactCard> getCC() {
        return cc;
    }
}
