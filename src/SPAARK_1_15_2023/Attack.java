package SPAARK_1_15_2023;

import battlecode.common.*;

public class Attack {
    protected static MapLocation attack(RobotController rc, MapLocation me, RobotType robotType, boolean attackAll) throws GameActionException{
        RobotInfo[] robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam().opponent());
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            MapLocation prioritizedRobotInfoLocation = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() == robotType) {
                    if (prioritizedRobotInfo == null) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                    else if (prioritizedRobotInfoLocation.distanceSquaredTo(me) > w.getLocation().distanceSquaredTo(me)) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                }
                else if (attackAll) {
                    if (prioritizedRobotInfo == null) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                    else if (prioritizedRobotInfoLocation.distanceSquaredTo(me) > w.getLocation()
                            .distanceSquaredTo(me)) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                }
            }
            if (prioritizedRobotInfoLocation != null) {
                if (rc.canAttack(prioritizedRobotInfoLocation)) {
                    rc.setIndicatorString("Attacking");
                    rc.attack(prioritizedRobotInfoLocation);
                }
            }
            return prioritizedRobotInfoLocation;
        }
        else {
            return null;
        }
    }
}