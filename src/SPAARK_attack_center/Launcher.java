package SPAARK_attack_center;

import battlecode.common.*;

import java.util.Random;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public strictfp class Launcher {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();

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

    private MapLocation[] headquarters;
    private MapLocation prioritizedHeadquarters;
    private MapLocation prioritizedOpponentHeadquarters;

    private RobotType prioritizedRobotType = RobotType.LAUNCHER;
    private int amplifierSensingRange = 50;
    private int amplifierCircleRange = 10;

    private int headquarterCircleRange = 16;
    private int headquarterCircleStuck = 0;
    
    private int defenseRange = 64;
    private boolean[] invalidOpponentLocations = new boolean[4];

    private int centerRange = 2;
    private boolean arrivedAtCenter = false;

    protected int amplifierID = -1;
    protected int launcherID = -1;

    private MapLocation prioritizedRobotInfoLocation;

    private MapLocation prioritizedAmplifierLocation;

    private boolean clockwiseRotation = true;
    
    private int state = 0;
    // state
    // 0 is wander
    // 1 is travelling to amplifier
    // 2 is travelling with amplifier
    // 3 is defense

    public Launcher(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            int hqCount = 0;
            for (int i = 1; i <= 4; i++) {
                if (GlobalArray.hasLocation(rc.readSharedArray(i)))
                    hqCount++;
            }
            headquarters = new MapLocation[hqCount];
            for (int i = 0; i < hqCount; i++) {
                headquarters[i] = GlobalArray.parseLocation(rc.readSharedArray(i + 1));
            }
            if (rc.getRoundNum() % 3 == 0) {
                state = 3;
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at Carrier constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Launcher constructor");
            e.printStackTrace();
        } finally {
            // Clock.yield();
        }
        run();
    }
    
    private void run() {
        while (true) {
            try {
                me = rc.getLocation();
                prioritizedRobotInfoLocation = Attack.attack(rc, me, prioritizedRobotType, true);
                
                if (state == 0) {
                    updatePrioritizedOpponentHeadquarters();
                    if (prioritizedOpponentHeadquarters != null) {
                        boolean hasSpace = false;
                        for (Direction d : directions) {
                            if (rc.canSenseLocation(prioritizedOpponentHeadquarters.add(d))) {
                                if (rc.senseRobotAtLocation(prioritizedOpponentHeadquarters.add(d)) == null && rc.sensePassability(prioritizedOpponentHeadquarters.add(d))) {
                                    hasSpace = true;
                                }
                            }
                        }
                        if (hasSpace) {
                            state = 2;
                            continue;
                        }
                    }
                    if (!detectAmplifier()) {
                        prioritizedHeadquarters = headquarters[0];
                        for (MapLocation hq : headquarters) {
                            if (hq != null) {
                                if (prioritizedHeadquarters.distanceSquaredTo(me) > hq.distanceSquaredTo(me)) {
                                    prioritizedHeadquarters = hq;
                                }
                            }
                        }
                        // Motion.spreadRandomly(rc, me, prioritizedHeadquarters, true);
                        rc.setIndicatorString("swarming");
                        // if (rng.nextBoolean()) {
                        //     Motion.swarm(rc, me, RobotType.CARRIER);
                        // }
                        // else {
                        //     Motion.spreadCenter(rc, me);
                        // }
                        if (prioritizedRobotInfoLocation != null) {
                            Motion.bug(rc, prioritizedRobotInfoLocation);
                            rc.setIndicatorLine(me, prioritizedRobotInfoLocation, 255, 125, 25);
                        }
                        else if (arrivedAtCenter) {
                            Motion.swarm(rc, me, RobotType.LAUNCHER);
                        }
                        else if (me.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2)) >= centerRange) {
                            Motion.bug(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2));
                            rc.setIndicatorString("moving to center");
                        }
                        else {
                            arrivedAtCenter = true;
                            // clockwiseRotation = Motion.circleAroundTarget(rc, me, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), centerRange, clockwiseRotation);
                        }
                        // Motion.spreadCenter(rc, me);
                    }
                }
                if (state == 1) {
                    int amplifierArray = rc.readSharedArray(amplifierID);
                    rc.setIndicatorString(amplifierID + " " + amplifierArray);
                    if (amplifierArray >> 14 == 0) {
                        state = 0;
                        continue;
                    }
                    updatePrioritizedOpponentHeadquarters();
                    if (prioritizedOpponentHeadquarters != null) {
                        prioritizedRobotInfoLocation = Attack.attack(rc, me, prioritizedRobotType, true);
                        boolean hasSpace = false;
                        for (Direction d : directions) {
                            if (rc.canSenseLocation(prioritizedOpponentHeadquarters.add(d))) {
                                if (rc.senseRobotAtLocation(prioritizedOpponentHeadquarters.add(d)) == null && rc.sensePassability(prioritizedOpponentHeadquarters.add(d))) {
                                    hasSpace = true;
                                }
                            }
                        }
                        if (hasSpace) {
                            state = 2;
                            continue;
                        }
                    }
                    prioritizedAmplifierLocation = GlobalArray.parseLocation(amplifierArray);
                    rc.setIndicatorLine(me, prioritizedAmplifierLocation, 255, 175, 75);
                    if (me.distanceSquaredTo(prioritizedAmplifierLocation) <= amplifierCircleRange) {
                        clockwiseRotation = Motion.circleAroundTarget(rc, me, prioritizedAmplifierLocation, amplifierCircleRange, clockwiseRotation);
                    }
                    else {
                        clockwiseRotation = Motion.bug(rc, prioritizedAmplifierLocation, clockwiseRotation);
                    }
                }
                if (state == 2) {
                    rc.setIndicatorString("Blocking HQ...");
                    if (me.distanceSquaredTo(prioritizedOpponentHeadquarters) <= 2) {
                        Motion.circleAroundTarget(rc, me, prioritizedOpponentHeadquarters);
                    }
                    else {
                        boolean hasSpace = false;
                        for (Direction d : directions) {
                            if (rc.canSenseLocation(prioritizedOpponentHeadquarters.add(d))) {
                                if (rc.senseRobotAtLocation(prioritizedOpponentHeadquarters.add(d)) == null && rc.sensePassability(prioritizedOpponentHeadquarters.add(d))) {
                                    hasSpace = true;
                                }
                            }
                        }
                        if (hasSpace) {
                            clockwiseRotation = Motion.bug(rc, prioritizedOpponentHeadquarters, clockwiseRotation);
                        }
                        else {
                            state = 0;
                        }
                    }
                }
                if (state == 3) {
                    // if (!detectAmplifier()) {
                        rc.setIndicatorString("defense");
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
                            rc.setIndicatorLine(me, prioritizedOpponentLocation, 75, 255, 75);
                            if (invalidOpponentLocations[prioritizedOpponentLocationIndex] == true) {
                                clockwiseRotation = Motion.bug(rc, prioritizedHeadquarters, clockwiseRotation);
                                if (rc.canWriteSharedArray(0, 0)) {
                                    rc.writeSharedArray(22 + prioritizedOpponentLocationIndex, 0);
                                    invalidOpponentLocations[prioritizedOpponentLocationIndex] = false;
                                }
                            }
                            else {
                                clockwiseRotation = Motion.bug(rc, prioritizedOpponentLocation, clockwiseRotation);
                                RobotInfo[] opponentRobots = rc.senseNearbyRobots(RobotType.LAUNCHER.visionRadiusSquared,rc.getTeam().opponent());
                                boolean allDead = true;
                                for (RobotInfo r : opponentRobots) {
                                    if (r.getType() != RobotType.HEADQUARTERS) {
                                        allDead = false;
                                    }
                                }
                                if (prioritizedOpponentLocation.distanceSquaredTo(me) <= 2 && allDead) {
                                    invalidOpponentLocations[prioritizedOpponentLocationIndex] = true;
                                }
                            }
                        }
                        else {
                            if (me.distanceSquaredTo(prioritizedHeadquarters) <= headquarterCircleRange * 1.5) {
                                clockwiseRotation = Motion.circleAroundTarget(rc, me, prioritizedHeadquarters, headquarterCircleRange, clockwiseRotation);
                                if (me.equals(rc.getLocation())) {
                                    headquarterCircleStuck += 1;
                                    if (headquarterCircleStuck == 10) {
                                        state = 0;
                                    }
                                }
                                else {
                                    headquarterCircleStuck = 0;
                                }
                            }
                            else {
                                clockwiseRotation = Motion.bug(rc, prioritizedHeadquarters, clockwiseRotation);
                            }
                            if (prioritizedRobotInfoLocation != null) {
                                state = 4;
                            }
                        }
                        me = rc.getLocation();
                        rc.setIndicatorDot(me, 75, 255, 75);
                    // }
                }
                if (state == 4) {
                    if (prioritizedRobotInfoLocation != null) {
                        rc.setIndicatorLine(me, prioritizedRobotInfoLocation, 255, 125, 25);
                        clockwiseRotation = Motion.bug(rc, prioritizedRobotInfoLocation, clockwiseRotation);
                    }
                    else {
                        state = 3;
                    }
                }
                if (state == 5) {
                    if (!detectAmplifier()) {
                        Motion.spreadEdges(rc, me);
                    }
                }
                me = rc.getLocation();
                prioritizedRobotInfoLocation = Attack.attack(rc, me, prioritizedRobotType, true);
            } catch (GameActionException e) {
                System.out.println("GameActionException at Launcher");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Launcher");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    private boolean detectAmplifier() throws GameActionException {
        prioritizedAmplifierLocation = null;
        for (int a = 0;a < 4;a++) {
            int amplifierArray = rc.readSharedArray(14 + a);
            if (amplifierArray >> 14 != 0) {
                MapLocation amplifierLocation = GlobalArray.parseLocation(amplifierArray);
                if (amplifierLocation.distanceSquaredTo(me) < amplifierSensingRange) {
                    if (prioritizedAmplifierLocation == null) {
                        prioritizedAmplifierLocation = amplifierLocation;
                        amplifierID = 14 + a;
                    }
                    else if (amplifierLocation.distanceSquaredTo(me) < prioritizedAmplifierLocation.distanceSquaredTo(me)) {
                        prioritizedAmplifierLocation = amplifierLocation;
                        amplifierID = 14 + a;
                    }
                }
            }
        }
        if (prioritizedAmplifierLocation != null) {
            state = 1;
            return true;
        }
        return false;
    }
    private void updatePrioritizedOpponentHeadquarters() throws GameActionException {
        RobotInfo[] opponentRobots = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        prioritizedOpponentHeadquarters = null;
        for (RobotInfo r : opponentRobots) {
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