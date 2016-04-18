/*
 * Copyright (C) 2016  Andrew Chow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
