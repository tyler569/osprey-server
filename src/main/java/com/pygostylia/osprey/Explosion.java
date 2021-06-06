package com.pygostylia.osprey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class Explosion {
    static Random rng = new Random();

    static Collection<Location> generateBoomBlocks(Location center, float power) {
        var locations = new ArrayList<Location>();
        for (int x = -20; x < 20; x++) {
            for (int y = -20; y < 20; y++) {
                for (int z = -20; z < 20; z++) {
                    var location = center.offset(x, y, z);
                    if (location.y() > 255 || location.y() < 0) continue;
                    double distance = center.distance(location);
                    if (power / 1.5 > distance || rng.nextDouble() * power > distance) {
                        locations.add(location);
                    }
                }
            }
        }
        return locations;
    }
}
