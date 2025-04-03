package com.example.czrtp;

// Position class to store x, y, z coordinates and world name
class Position {
    private final double x;
    private final double y;
    private final double z;
    private final String worldName;

    public Position(double x, double y, double z, String worldName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getWorldName() {
        return worldName;
    }
}

// Zone class to store zone information
class Zone {
    private final String name;
    private final Position[] positions;

    public Zone(String name) {
        this.name = name;
        this.positions = new Position[5]; // Index 0 not used, positions 1-4
    }

    public String getName() {
        return name;
    }

    public void setPosition(int posNumber, Position position) {
        if (posNumber >= 1 && posNumber <= 4) {
            positions[posNumber] = position;
        }
    }

    public Position getPosition(int posNumber) {
        if (posNumber >= 1 && posNumber <= 4) {
            return positions[posNumber];
        }
        return null;
    }

    public boolean isComplete() {
        // Check if all positions are set
        for (int i = 1; i <= 4; i++) {
            if (positions[i] == null) {
                return false;
            }
        }
        return true;
    }
}

// ZoneCreation class to track zone creation progress
class ZoneCreation {
    private final String zoneName;
    private final Position[] positions;

    public ZoneCreation(String zoneName) {
        this.zoneName = zoneName;
        this.positions = new Position[5]; // Index 0 not used, positions 1-4
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setPosition(int posNumber, Position position) {
        if (posNumber >= 1 && posNumber <= 4) {
            positions[posNumber] = position;
        }
    }

    public Position getPosition(int posNumber) {
        if (posNumber >= 1 && posNumber <= 4) {
            return positions[posNumber];
        }
        return null;
    }

    public boolean isComplete() {
        // Check if all positions are set
        for (int i = 1; i <= 4; i++) {
            if (positions[i] == null) {
                return false;
            }
        }
        return true;
    }
}