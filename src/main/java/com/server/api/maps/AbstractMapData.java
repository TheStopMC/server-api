package com.server.api.maps;

import net.hollowcube.polar.PolarWorldAccess;
import net.minestom.server.coordinate.Pos;

public abstract class AbstractMapData implements PolarWorldAccess {

    public abstract Pos spawn();

    public abstract void spawn(Pos spawn);

    public abstract String schematic();

    public abstract int worldBorder();

    public abstract void worldBorder(int worldBorder);

    public abstract Pos worldBorderCenter();

    public abstract void worldBorderCenter(Pos worldBorderCenter);


}
