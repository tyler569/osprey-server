package com.pygostylia.osprey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class Explosion {
    static Random rng = new Random();

    static Collection<Location> generateBoomBlocks(Location center, float power) {
        var locations = new ArrayList<Location>();
        final int maxRadius = 20;
        for (int x = -maxRadius; x < maxRadius; x++) {
            for (int y = -maxRadius; y < maxRadius; y++) {
                for (int z = -maxRadius; z < maxRadius; z++) {
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
