package com.katelynslater.data;

import java.util.ArrayList;

public class Location {
    public final int id;
    public final String name;
    public Route nextRoute;
    public Route prevRoute;
    public Location(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
