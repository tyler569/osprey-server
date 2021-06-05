package com.pygostylia.osprey;

import java.io.IOException;
import java.util.ArrayList;

public class BoatEntity extends ObjectEntity {
    ArrayList<Entity> passengers = new ArrayList<>();

    public BoatEntity(Position position) {
        super(position);
    }

    @Override
    int type() {
        return 6;
    }

    @Override
    public void spawnForPlayer(Player player) throws IOException {
        super.spawnForPlayer(player);
        if (!passengers.isEmpty()) {
            updatePassengers(player);
        }
    }

    void updatePassengers(Player player) {
        try {
            player.sendSetPassengers(this, passengers);
        } catch (IOException e) {
            player.println("Failed to set passengers for <-");
        }
    }

    void updatePassengers() {
        playersWithLoaded.forEach(this::updatePassengers);
    }

    public void addPassenger(Entity passenger) {
        passengers.add(passenger);
        if (passenger instanceof Player player) {
            player.setRidingEntity(this);
        }
        updatePassengers();
    }

    public void removePassenger(Entity passenger) {
        passengers.remove(passenger);
        if (passenger instanceof Player player) {
            player.setNotRidingEntity();
        }
        updatePassengers();
    }
}
