package de.tudarmstadt.informatik.tk.silentmusicparty.network;

import java.io.Serializable;

/**
 * Created by Mohit on 26.02.2017.
 */

public class Message implements Serializable {

    private String tag;
    private Object data;

    public Message(String tag, Object data) {
        this.tag = tag;
        this.data = data;
    }

    public String getTag() {
        return tag;
    }

    public Object getData() {
        return data;
    }
}
