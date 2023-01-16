package SPAARK;

import battlecode.common.*;

public class Attack {
    protected static MapLocation attack(RobotController rc, MapLocation me, RobotInfo[] robotInfo, RobotType robotType, boolean attackAll) throws GameActionException {
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            MapLocation prioritizedRobotInfoLocation = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() == RobotType.HEADQUARTERS) {
                    continue;
                }
                if (w.getType() == robotType) {
                    if (prioritizedRobotInfo == null) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                    else if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                }
                else if (attackAll) {
                    if (prioritizedRobotInfo == null) {
                        prioritizedRobotInfo = w;
                        prioritizedRobotInfoLocation = w.getLocation();
                    }
                    else if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
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
    protected static MapLocation senseOpponent(RobotController rc, MapLocation me, RobotInfo[] robotInfo) throws GameActionException {
        if (robotInfo.length > 0) {
            RobotInfo prioritizedRobotInfo = null;
            MapLocation prioritizedRobotInfoLocation = null;
            for (RobotInfo w : robotInfo) {
                if (w.getType() == RobotType.HEADQUARTERS) {
                    continue;
                }
                if (prioritizedRobotInfo == null) {
                    prioritizedRobotInfo = w;
                    prioritizedRobotInfoLocation = w.getLocation();
                }
                else if (prioritizedRobotInfo.getHealth() > w.getHealth()) {
                    prioritizedRobotInfo = w;
                    prioritizedRobotInfoLocation = w.getLocation();
                }
            }
            return prioritizedRobotInfoLocation;
        }
        else {
            return null;
        }
    }
}