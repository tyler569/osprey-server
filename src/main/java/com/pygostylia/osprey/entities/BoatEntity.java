package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.EntityPosition;
import com.pygostylia.osprey.Registry;

import java.util.ArrayList;

public class BoatEntity extends ObjectEntity {
    static final int TYPE = Registry.entity("minecraft:boat");

    public boolean turningLeft;
    public boolean turningRight;
    ArrayList<Entity> passengers = new ArrayList<>();

    public BoatEntity(EntityPosition entityPosition) {
        super(entityPosition);
    }

    @Override
    public int type() {
        return TYPE;
    }

    @Override
    public float colliderXZ() {
        return 1.375f;
    }

    @Override
    public float colliderY() {
        return 0.5625f;
    }

    @Override
    public void spawnForPlayer(Player player) {
        super.spawnForPlayer(player);
        if (!passengers.isEmpty()) {
            updatePassengers(player);
        }
    }

    void updatePassengers(Player player) {
        player.sendSetPassengers(this, passengers);
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

    @Override
    public void interact(Player sender) {
        addPassenger(sender);
    }

    public void dismount(Player sender) {
        removePassenger(sender);
    }

    @Override
    public void attack(Player sender) {
        destroy();
    }
}
