package SPAARK_small_swarm;

import battlecode.common.*;

import java.util.Random;

public strictfp class Launcher {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    private final Random rng = new Random(2023);
    private static final Direction[] DIRECTIONS = {
            Direction.SOUTHWEST,
            Direction.SOUTH,
            Direction.SOUTHEAST,
            Direction.WEST,
            Direction.EAST,
            Direction.NORTHWEST,
            Direction.NORTH,
            Direction.NORTHEAST,
    };

    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private MapLocation prioritizedOpponentHeadquarters;

    private StoredLocations storedLocations;

    private RobotInfo[] robotInfo;
    private MapLocation opponentLocation;

    private RobotType prioritizedRobotType = RobotType.LAUNCHER;
    private int amplifierSensingRange = 25;
    private int amplifierCircleRange = 7;

    private int launcherCircleRange = 2;

    private int headquarterCircleRange = 16;

    private int defenseRange = 576;
    private int edgeRange = 4;

    private MapLocation lastLauncherLocation;

    private MapLocation prioritizedAmplifierLocation;

    private boolean clockwiseRotation = true;
    private Direction lastDirection = Direction.CENTER;

    private int state = 0;
    // state
    // 0 is everything

    private StringBuilder indicatorString = new StringBuilder();

    public Launcher(RobotController rc) {
        try {
            this.rc = rc;
            int hqCount = 0;
            for (int i = GlobalArray.HEADQUARTERS; i < GlobalArray.HEADQUARTERS + GlobalArray.HEADQUARTERS_LENGTH; i++) {
                if (GlobalArray.hasLocation(rc.readSharedArray(i)))
                    hqCount++;
            }
            headquarters = new MapLocation[hqCount];
            for (int i = 0; i < hqCount; i++) {
                headquarters[i] = GlobalArray.parseLocation(rc.readSharedArray(i + GlobalArray.HEADQUARTERS));
            }
            storedLocations = new StoredLocations(rc, headquarters);
        } catch (GameActionException e) {
            System.out.println("GameActionException at Launcher constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Launcher constructor");
            e.printStackTrace();
        } finally {
            run();
        }
    }

    private void run() {
        while (true) {
            try {
                me = rc.getLocation();
                round = rc.getRoundNum();

                indicatorString = new StringBuilder();

                robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
                MapLocation loc = Attack.attack(rc, me, robotInfo, prioritizedRobotType, true, indicatorString);
                if (loc == null) {
                    robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                    loc = Attack.senseOpponent(rc, me, robotInfo);
                }
                if (loc != null) {
                    opponentLocation = loc;
                }

                storedLocations.detectWells();
                storedLocations.detectIslandLocations();
                storedLocations.writeToGlobalArray();

                runState();

                me = rc.getLocation();
                robotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
                loc = Attack.attack(rc, me, robotInfo, prioritizedRobotType, true, indicatorString);
                if (loc == null) {
                    robotInfo = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
                    loc = Attack.senseOpponent(rc, me, robotInfo);
                }
                if (loc != null) {
                    opponentLocation = loc;
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException at Launcher");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Launcher");
                e.printStackTrace();
            } finally {
                rc.setIndicatorString(indicatorString.toString());
                Clock.yield();
            }
        }
    }

    // intentionally stand on opponent islands
    private void runState() throws GameActionException {
        if (state == 0) {
            if (detectAmplifier()) {
                state = 1;
                runState();
                return;
            }
            prioritizedHeadquarters = headquarters[0];
            for (MapLocation hq : headquarters) {
                if (hq != null) {
                    if (prioritizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                        prioritizedHeadquarters = hq;
                    }
                }
            }
            MapLocation[] opponentLocations = GlobalArray.getKnownOpponentLocations(rc);
            MapLocation prioritizedOpponentLocation = null;
            int prioritizedOpponentLocationIndex = -1;
            int index = -1;
            for (MapLocation m : opponentLocations) {
                index++;
                if (m == null) {
                    continue;
                }
                if (prioritizedOpponentLocation == null) {
                    prioritizedOpponentLocation = m;
                    prioritizedOpponentLocationIndex = index;
                    continue;
                }
                if (prioritizedOpponentLocation.distanceSquaredTo(me) > m.distanceSquaredTo(me)) {
                    prioritizedOpponentLocation = m;
                    prioritizedOpponentLocationIndex = index;
                }
            }
            if (prioritizedOpponentLocation != null && prioritizedOpponentLocation.distanceSquaredTo(me) <= defenseRange) {
                if (GlobalArray.DEBUG_INFO >= 2) {
                    rc.setIndicatorLine(me, prioritizedOpponentLocation, 75, 255, 75);
                } else if (GlobalArray.DEBUG_INFO > 0) {
                    rc.setIndicatorDot(me, 75, 255, 75);
                }
                if (storedLocations.removedOpponents[prioritizedOpponentLocationIndex] == true) {
                    Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                } else {
                    Direction[] bug2array = Motion.bug2(rc, prioritizedOpponentLocation, lastDirection, clockwiseRotation, indicatorString);
                    lastDirection = bug2array[0];
                    if (bug2array[1] == Direction.CENTER) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                    RobotInfo[] robotInfo = rc.senseNearbyRobots(RobotType.LAUNCHER.visionRadiusSquared, rc.getTeam().opponent());
                    boolean allDead = true;
                    for (RobotInfo r : robotInfo) {
                        if (r.getType() != RobotType.HEADQUARTERS) {
                            allDead = false;
                        }
                    }
                    if (prioritizedOpponentLocation.distanceSquaredTo(me) <= 2 && allDead) {
                        storedLocations.removeOpponentLocation(prioritizedOpponentLocationIndex);
                    }
                }
                return;
            }
            // idk whats going on
            RobotInfo[] friendlyRobotInfo = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
            int surroundingLaunchers = 0;
            if (friendlyRobotInfo.length > 0) {
                RobotInfo lowestIdFriendlyRobotInfo = null;
                RobotInfo highestIdFriendlyRobotInfo = null;
                for (RobotInfo w : friendlyRobotInfo) {
                    if (w.getType() != RobotType.LAUNCHER) {
                        continue;
                    }
                    surroundingLaunchers += 1;
                    if (lowestIdFriendlyRobotInfo == null) {
                        lowestIdFriendlyRobotInfo = w;
                        highestIdFriendlyRobotInfo = w;
                        continue;
                    }
                    if (lowestIdFriendlyRobotInfo.ID > w.ID) {
                        lowestIdFriendlyRobotInfo = w;
                    }
                    if (highestIdFriendlyRobotInfo.ID < w.ID) {
                        highestIdFriendlyRobotInfo = w;
                    }
                }
                if (surroundingLaunchers >= 2) {
                    if (lowestIdFriendlyRobotInfo.ID > rc.getID()) {
                        if (opponentLocation != null) {
                            Direction[] bug2array = Motion.bug2(rc, opponentLocation, lastDirection, clockwiseRotation, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                            me = rc.getLocation();
                            if (GlobalArray.DEBUG_INFO >= 2) {
                                rc.setIndicatorLine(me, opponentLocation, 255, 125, 25);
                            } else if (GlobalArray.DEBUG_INFO > 0) {
                                rc.setIndicatorDot(me, 255, 125, 25);
                            }
                            if (me.distanceSquaredTo(opponentLocation) <= 5) {
                                opponentLocation = null;
                            }
                            lastLauncherLocation = null;
                        } else {
                            if (prioritizedHeadquarters.distanceSquaredTo(me) <= 100) {
                                Direction[] bug2array = Motion.bug2(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), lastDirection, clockwiseRotation, indicatorString);
                                lastDirection = bug2array[0];
                                if (bug2array[1] == Direction.CENTER) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                            } else if (rng.nextBoolean()) {
                                Direction[] bug2array = Motion.bug2(rc, highestIdFriendlyRobotInfo.getLocation(), lastDirection, clockwiseRotation, indicatorString);
                                lastDirection = bug2array[0];
                                if (bug2array[1] == Direction.CENTER) {
                                    clockwiseRotation = !clockwiseRotation;
                                }
                                me = rc.getLocation();
                                if (GlobalArray.DEBUG_INFO >= 2) {
                                    rc.setIndicatorLine(me, highestIdFriendlyRobotInfo.getLocation(), 75, 255, 255);
                                } else if (GlobalArray.DEBUG_INFO > 0) {
                                    rc.setIndicatorDot(me, 75, 255, 255);
                                }
                            } else {
                                Motion.moveRandomly(rc);
                            }
                            lastLauncherLocation = null;
                        }
                    } else {
                        if (opponentLocation != null) {
                            if (GlobalArray.DEBUG_INFO >= 2) {
                                rc.setIndicatorLine(me, opponentLocation, 255, 125, 25);
                            } else if (GlobalArray.DEBUG_INFO > 0) {
                                rc.setIndicatorDot(me, 255, 125, 25);
                            }
                            Direction[] bug2array = Motion.bug2(rc, opponentLocation, lastDirection, clockwiseRotation, indicatorString);
                            lastDirection = bug2array[0];
                            if (bug2array[1] == Direction.CENTER) {
                                clockwiseRotation = !clockwiseRotation;
                            }
                            me = rc.getLocation();
                            // if (surroundingLaunchers <= 4) {
                            //     if (me.distanceSquaredTo(opponentLocation) <= 8) {
                            //         opponentLocation = null;
                            //     }
                            // }
                            if (me.distanceSquaredTo(opponentLocation) <= 5) {
                                opponentLocation = null;
                            }
                            lastLauncherLocation = null;
                        } else {
                            if (lastLauncherLocation != null && rc.canMove(lastLauncherLocation.directionTo(lowestIdFriendlyRobotInfo.getLocation()))) {
                                rc.move(lastLauncherLocation.directionTo(lowestIdFriendlyRobotInfo.getLocation()));
                                lastLauncherLocation = lowestIdFriendlyRobotInfo.getLocation();
                            }
                            else {
                                if (me.distanceSquaredTo(lowestIdFriendlyRobotInfo.getLocation()) <= launcherCircleRange * 1.5) {
                                    clockwiseRotation = Motion.circleAroundTarget(rc, me, lowestIdFriendlyRobotInfo.getLocation(), launcherCircleRange, clockwiseRotation);
                                    lastLauncherLocation = lowestIdFriendlyRobotInfo.getLocation();
                                }
                                else {
                                    Direction[] bug2array = Motion.bug2(rc, lowestIdFriendlyRobotInfo.getLocation(), lastDirection, clockwiseRotation, indicatorString);
                                    lastDirection = bug2array[0];
                                    if (bug2array[1] == Direction.CENTER) {
                                        clockwiseRotation = !clockwiseRotation;
                                    }
                                }
                            }
                        }
                        me = rc.getLocation();
                        if (GlobalArray.DEBUG_INFO >= 2) {
                            rc.setIndicatorLine(me, lowestIdFriendlyRobotInfo.getLocation(), 255, 255, 75);
                        } else if (GlobalArray.DEBUG_INFO > 0) {
                            rc.setIndicatorDot(me, 255, 255, 75);
                        }
                    }
                    return;
                }
            }
            // defense or soemthing
            indicatorString.append("DEF; ");
            headquarterCircleRange = 16 + surroundingLaunchers / 3;
            lastLauncherLocation = null;
            // if (opponentLocation != null) {
            //     if (GlobalArray.DEBUG_INFO >= 2) {
            //         rc.setIndicatorLine(me, opponentLocation, 255, 125, 25);
            //     } else if (GlobalArray.DEBUG_INFO > 0) {
            //         rc.setIndicatorDot(me, 255, 125, 25);
            //     }
            //     Direction[] bug2array = Motion.bug2(rc, opponentLocation, lastDirection, clockwiseRotation, indicatorString);
            //     lastDirection = bug2array[0];
            //     if (bug2array[1] == Direction.CENTER) {
            //         clockwiseRotation = !clockwiseRotation;
            //     }
            //     me = rc.getLocation();
            //     if (me.distanceSquaredTo(opponentLocation) <= 2) {
            //         opponentLocation = null;
            //     }
            // } else 
            if (me.distanceSquaredTo(prioritizedHeadquarters) <= headquarterCircleRange * 1.25) {
                if (me.x <= edgeRange || me.x >= rc.getMapWidth() - edgeRange || me.y <= edgeRange || me.y >= rc.getMapHeight() - edgeRange) {
                    if (rc.isMovementReady()) {
                        clockwiseRotation = !clockwiseRotation;
                    }
                }
                clockwiseRotation = Motion.circleAroundTarget(rc, me, prioritizedHeadquarters, headquarterCircleRange, clockwiseRotation);
            } else {
                Direction[] bug2array = Motion.bug2(rc, prioritizedHeadquarters, lastDirection, clockwiseRotation, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
            }
            me = rc.getLocation();
            if (GlobalArray.DEBUG_INFO >= 1) {
                rc.setIndicatorDot(me, 75, 255, 75);
            }
        }
        else if (state == 1) {
            if (!detectAmplifier()) {
                state = 0;
                runState();
                return;
            }
            if (GlobalArray.DEBUG_INFO >= 2) {
                rc.setIndicatorLine(me, prioritizedAmplifierLocation, 255, 175, 75);
            } else if (GlobalArray.DEBUG_INFO > 0) {
                rc.setIndicatorDot(me, 255, 175, 75);
            }
            if (me.distanceSquaredTo(prioritizedAmplifierLocation) <= amplifierCircleRange * 1.25) {
                indicatorString.append("CIRC-AMP " + prioritizedAmplifierLocation.toString() + "; ");
                clockwiseRotation = Motion.circleAroundTarget(rc, me, prioritizedAmplifierLocation, amplifierCircleRange, clockwiseRotation);
            } else {
                indicatorString.append("PATH->AMP " + prioritizedAmplifierLocation.toString() + "; ");
                Direction[] bug2array = Motion.bug2(rc, prioritizedAmplifierLocation, lastDirection, clockwiseRotation, indicatorString);
                lastDirection = bug2array[0];
                if (bug2array[1] == Direction.CENTER) {
                    clockwiseRotation = !clockwiseRotation;
                }
            }
        }
    }

    private boolean detectAmplifier() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        for (RobotInfo r : robots) {
            if (r.getType() == RobotType.AMPLIFIER && r.getLocation().distanceSquaredTo(me) < amplifierSensingRange) {
                prioritizedAmplifierLocation = r.getLocation();
                return true;
            }
        }
        return false;
    }

    private void updatePrioritizedOpponentHeadquarters() throws GameActionException {
        prioritizedOpponentHeadquarters = null;
        for (RobotInfo r : robotInfo) {
            if (r.getType() == RobotType.HEADQUARTERS) {
                if (prioritizedOpponentHeadquarters == null) {
                    prioritizedOpponentHeadquarters = r.getLocation();
                    continue;
                }
                if (prioritizedOpponentHeadquarters.distanceSquaredTo(me) > r.getLocation().distanceSquaredTo(me)) {
                    prioritizedOpponentHeadquarters = r.getLocation();
                }
            }
        }
    }
}