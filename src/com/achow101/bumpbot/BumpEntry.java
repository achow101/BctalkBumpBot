package com.achow101.bumpbot;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Created by Andy Chow on 4/16/2016.
 */
@Entity
public class BumpEntry {

    @Id private String url;
    private String bumpText;
    private long time;

    public BumpEntry(String url, String bumpText, long time)
    {
        this.url = url;
        this.bumpText = bumpText;
        this.time = time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public String getUrl()
    {
        return url;
    }

    public String getBumpText()
    {
        return bumpText;
    }

    public long getTime()
    {
        return time;
    }
}
