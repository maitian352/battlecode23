package SPORK;

import battlecode.common.*;

import java.util.Random;

public class Motion {
    private static final Random rng = new Random(2023);
    private static final Direction[] directions = {
        Direction.SOUTHWEST,
        Direction.SOUTH,
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
    };

    public static void moveRandomly(RobotController rc) throws GameActionException {
        while (rc.isMovementReady()) {
            Direction direction = directions[rng.nextInt(directions.length)];
            if (rc.canMove(direction)) {
                rc.move(direction);
            }
        }
    }
    public static void spreadRandomly(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        if (rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam()).length <= 10) {
            moveRandomly(rc);
            return;
        }
        Direction direction = me.directionTo(target).opposite();
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(4);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    moved = true;
                }
            }
            else if (random == 3) {
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    moved = true;
                }
            }
            else {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            if (moved == false) {
                Direction d = directions[rng.nextInt(directions.length)];
                if (rc.canMove(d)) {
                    rc.move(d);
                }
            }
        }
    }
    public static void spreadRandomly(RobotController rc, MapLocation me, MapLocation target, boolean avoidCorners) throws GameActionException {
        if (me.distanceSquaredTo(new MapLocation(0, 0)) <= 25 || me.distanceSquaredTo(new MapLocation(rc.getMapWidth(), 0)) <= 25 || me.distanceSquaredTo(new MapLocation(0, rc.getMapHeight())) <= 25 || me.distanceSquaredTo(new MapLocation(rc.getMapWidth(), rc.getMapHeight())) <= 25) {
            Direction direction = me.directionTo(new MapLocation(rc.getMapWidth() / 2,rc.getMapHeight() / 2));
            while (rc.isMovementReady()) {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    continue;
                }
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    continue;
                }
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    continue;
                }
            }
            return;
        }
        if (rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam()).length <= 10) {
            moveRandomly(rc);
            return;
        }
        Direction direction = me.directionTo(target).opposite();
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(4);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    moved = true;
                }
            }
            else if (random == 3) {
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    moved = true;
                }
            }
            else {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            if (moved == false) {
                Direction d = directions[rng.nextInt(directions.length)];
                if (rc.canMove(d)) {
                    rc.move(d);
                }
            }
        }
    }
    public static void spreadCenter(RobotController rc, MapLocation me) throws GameActionException {
        Direction direction = me.directionTo(new MapLocation(rc.getMapWidth() / 2,rc.getMapHeight() / 2));
        while (rc.isMovementReady()) {
            boolean moved = false;
            int random = rng.nextInt(6);
            if (random == 0) {
                if (rc.canMove(direction.rotateLeft())) {
                    rc.move(direction.rotateLeft());
                    moved = true;
                }
            }
            else if (random == 3) {
                if (rc.canMove(direction.rotateRight())) {
                    rc.move(direction.rotateRight());
                    moved = true;
                }
            }
            else if (random == 1 || random == 2) {
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    moved = true;
                }
            }
            while (moved == false) {
                Direction d = directions[rng.nextInt(directions.length)];
                if (rc.canMove(d)) {
                    rc.move(d);
                    moved = true;
                }
            }
        }
    }
    public static void circleAroundTarget(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        Direction direction = me.directionTo(target).rotateLeft();
        if (direction.ordinal() % 2 == 1) {
            direction = direction.rotateLeft();
        }
        if (rc.canMove(direction)) {
            rc.move(direction);
        }
    }
    public static boolean circleAroundTarget(RobotController rc, MapLocation me, MapLocation target, int distance, boolean clockwiseRotation) throws GameActionException {
        Direction direction = me.directionTo(target);
        if (me.distanceSquaredTo(target) > (int) distance * 1.25) {
            if (clockwiseRotation) {
                direction = direction.rotateLeft();
            }
            else {
                direction = direction.rotateRight();
            }
        }
        else if (me.distanceSquaredTo(target) < (int) distance * 0.75) {
            direction = direction.opposite();
        }
        else {
            if (clockwiseRotation) {
                direction = direction.rotateLeft().rotateLeft();
            }
            else {
                direction = direction.rotateRight().rotateRight();
            }
        }
        if (rc.isMovementReady()) {
            if (rc.canMove(direction)) {
                rc.move(direction);
                return clockwiseRotation;
            }
            else {
                return !clockwiseRotation;
            }
        }
        return clockwiseRotation;
    }

    public static void bug(RobotController rc,MapLocation dest) throws GameActionException {
        Direction lastDirection = Direction.CENTER;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return;
            }
            boolean moved = false;
            Direction direction = me.directionTo(dest);
            if (rc.canMove(direction)) {
                rc.move(direction);
                lastDirection = direction;
            }
            else {
                rc.setIndicatorString(direction.toString());
                if (rc.canMove(direction.rotateLeft()) && lastDirection != direction.rotateLeft().opposite()) {
                    rc.move(direction.rotateLeft());
                    lastDirection = direction.rotateLeft();
                }
                else if (rc.canMove(direction.rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().opposite()) {
                    rc.move(direction.rotateLeft().rotateLeft());
                    lastDirection = direction.rotateLeft().rotateLeft();
                }
                else if (rc.canMove(direction.rotateLeft().rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().rotateLeft().opposite()) {
                    rc.move(direction.rotateLeft().rotateLeft().rotateLeft());
                    lastDirection = direction.rotateLeft().rotateLeft().rotateLeft();
                }
                else if (rc.canMove(direction.rotateLeft().rotateLeft().rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().rotateLeft().rotateLeft().opposite()) {
                    rc.move(direction.rotateLeft().rotateLeft().rotateLeft().rotateLeft());
                    lastDirection = direction.rotateLeft().rotateLeft().rotateLeft().rotateLeft();
                }
                else if (rc.canMove(direction.rotateRight()) && lastDirection != direction.rotateRight().opposite()) {
                    rc.move(direction.rotateRight());
                    lastDirection = direction.rotateRight();
                }
                else if (rc.canMove(direction.rotateRight().rotateRight()) && lastDirection != direction.rotateRight().rotateRight().opposite()) {
                    rc.move(direction.rotateRight().rotateRight());
                    lastDirection = direction.rotateRight().rotateRight();
                }
                else if (rc.canMove(direction.rotateRight().rotateRight().rotateRight()) && lastDirection != direction.rotateRight().rotateRight().rotateRight().opposite()) {
                    rc.move(direction.rotateRight().rotateRight().rotateRight());
                    lastDirection = direction.rotateRight().rotateRight().rotateRight();
                }
                else if (rc.canMove(direction.opposite()) && lastDirection != direction) {
                    rc.move(direction.opposite());
                    lastDirection = direction.opposite();
                }
            }
        }
    }
    public static boolean bug(RobotController rc,MapLocation dest, boolean clockwiseRotation) throws GameActionException {
        Direction lastDirection = Direction.CENTER;
        while (rc.isMovementReady()) {
            MapLocation me = rc.getLocation();
            if (me.equals(dest)) {
                return clockwiseRotation;
            }
            boolean moved = false;
            Direction direction = me.directionTo(dest);
            if (rc.canMove(direction)) {
                rc.move(direction);
                lastDirection = direction;
            }
            else {
                rc.setIndicatorString(direction.toString() + " " + clockwiseRotation);
                if (clockwiseRotation) {
                    if (rc.canMove(direction.rotateLeft()) && lastDirection != direction.rotateLeft().opposite()) {
                        rc.move(direction.rotateLeft());
                        lastDirection = direction.rotateLeft();
                    }
                    else if (rc.canMove(direction.rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().opposite()) {
                        rc.move(direction.rotateLeft().rotateLeft());
                        lastDirection = direction.rotateLeft().rotateLeft();
                    }
                    else if (rc.canMove(direction.rotateLeft().rotateLeft().rotateLeft()) && lastDirection != direction.rotateLeft().rotateLeft().rotateLeft().opposite()) {
                        rc.move(direction.rotateLeft().rotateLeft().rotateLeft());
                        lastDirection = direction.rotateLeft().rotateLeft().rotateLeft();
                    }
                    else if (rc.canMove(direction.opposite()) && lastDirection != direction) {
                        rc.move(direction.opposite());
                        lastDirection = direction.opposite();
                        clockwiseRotation = !clockwiseRotation;
                    }
                    else {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
                else {
                    if (rc.canMove(direction.rotateRight()) && lastDirection != direction.rotateRight().opposite()) {
                        rc.move(direction.rotateRight());
                        lastDirection = direction.rotateRight();
                    }
                    else if (rc.canMove(direction.rotateRight().rotateRight()) && lastDirection != direction.rotateRight().rotateRight().opposite()) {
                        rc.move(direction.rotateRight().rotateRight());
                        lastDirection = direction.rotateRight().rotateRight();
                    }
                    else if (rc.canMove(direction.rotateRight().rotateRight().rotateRight()) && lastDirection != direction.rotateRight().rotateRight().rotateRight().opposite()) {
                        rc.move(direction.rotateRight().rotateRight().rotateRight());
                        lastDirection = direction.rotateRight().rotateRight().rotateRight();
                    }
                    else if (rc.canMove(direction.opposite()) && lastDirection != direction) {
                        rc.move(direction.opposite());
                        lastDirection = direction.opposite();
                        clockwiseRotation = !clockwiseRotation;
                    }
                    else {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
            }
            break;
        }
        return clockwiseRotation;
    }
}