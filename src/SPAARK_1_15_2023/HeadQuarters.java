package SPAARK_1_15_2023;

import battlecode.common.*;

import java.util.Random;

public strictfp class HeadQuarters {
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

    protected int hqIndex;
    private int locInt;
    private int hqCount;

    private int turnCount = 0;

    private int anchorCooldown = 0;

    private int carriers = 0;
    private int carrierCooldown = 0;

    private int launchers = 0;
    private int launcherCooldown = 0;

    private int possibleSpawningLocations = 0;

    private boolean isPrimaryHQ = false;
    private boolean setTargetElixirWell = false;
    protected int adamantium = 0;
    protected int mana = 0;
    protected int lastAdamantium = 0;
    protected int lastMana = 0;

    public HeadQuarters(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
            // setting headquarter locations
            locInt = GlobalArray.intifyLocation(rc.getLocation());
            if (!GlobalArray.hasLocation(rc.readSharedArray(1))) {
                rc.writeSharedArray(1, locInt);
                hqIndex = 1;
                isPrimaryHQ = true;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(2))) {
                rc.writeSharedArray(2, locInt);
                hqIndex = 2;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(3))) {
                rc.writeSharedArray(3, locInt);
                hqIndex = 3;
            } else if (!GlobalArray.hasLocation(rc.readSharedArray(4))) {
                rc.writeSharedArray(4, locInt);
                hqIndex = 4;
            } else {
                throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Too many HeadQuarters!");
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException at HeadQuarters constructor");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at HeadQuarters constructor");
            e.printStackTrace();
        } finally {
            // Clock.yield();
        }
        run();
    }

    private void run() {
        // for (int i = 0;i < 10;i++) {
        //     Clock.yield();
        // }
        while (true) {
            try {
                turnCount++;
                me = rc.getLocation();
                
                adamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                mana = rc.getResourceAmount(ResourceType.MANA);

                // if (turnCount > 600) {
                //     rc.resign();
                // }

                MapLocation optimalSpawningLocationWell = optimalSpawnLocation(rc, me, true);
                MapLocation optimalSpawningLocation = optimalSpawnLocation(rc, me, false);
                if (anchorCooldown <= 0 && turnCount >= 200 && rc.getNumAnchors(Anchor.STANDARD) == 0) {
                    if (adamantium >= 100 && mana >= 100) {
                        rc.buildAnchor(Anchor.STANDARD);
                        System.out.println("Anchor Produced!");
                        anchorCooldown = 50;
                    }
                }
                else {
                    boolean canProduceAmplifier = false;
                    for (int a = 0;a < 4;a++) {
                        if ((rc.readSharedArray(14 + a) >> 14) % 2 == 0) {
                            canProduceAmplifier = true;
                        }
                    }
                    if (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.LAUNCHER, optimalSpawningLocation) && possibleSpawningLocations >= 3) {
                        rc.buildRobot(RobotType.LAUNCHER, optimalSpawningLocation);
                        launchers += 1;
                        rc.setIndicatorString("Produced Launcher!");
                    }
                    else if (optimalSpawningLocation != null && rc.canBuildRobot(RobotType.AMPLIFIER, optimalSpawningLocation) && possibleSpawningLocations >= 6 && launchers > 20 && canProduceAmplifier) {
                        rc.buildRobot(RobotType.AMPLIFIER, optimalSpawningLocation);
                    }
                    else if (optimalSpawningLocationWell != null && rc.canBuildRobot(RobotType.CARRIER, optimalSpawningLocationWell) && possibleSpawningLocations >= 5) {
                        rc.buildRobot(RobotType.CARRIER, optimalSpawningLocationWell);
                        carriers += 1;
                        rc.setIndicatorString("Produced Carrier!");
                    }
                }
                anchorCooldown -= 1;
                // store
                try {
                    GlobalArray.storeHeadquarters(this);
                }
                catch (GameActionException e) {

                }
                if (isPrimaryHQ) {
                    if (hqCount == 0) {
                        for (int i = 1; i <= 4; i++) {
                            if (GlobalArray.hasLocation(rc.readSharedArray(i))) hqCount++;
                        }
                    }
                    // set prioritized resource
                    // set upgrade wells if resources adequate
                    boolean upgradeWells = true;
                    int totalRatio = 0;
                    for (int i = 1; i <= hqCount+1; i++) {
                        int arrayHQ = rc.readSharedArray(i);
                        if (!GlobalArray.adequateResources(arrayHQ)) {
                            upgradeWells = false;
                        }
                        totalRatio += GlobalArray.resourceRatio(arrayHQ);
                    }
                    // upgrade wells
                    globalArray.setUpgradeWells(upgradeWells);
                    // prioritized resources
                    int deviation = totalRatio - (2 * hqCount);
                    if (Math.abs(deviation) <= 1) {
                        globalArray.setPrioritizedResource(ResourceType.NO_RESOURCE);
                    } else if (deviation < 0) {
                        globalArray.setPrioritizedResource(ResourceType.MANA);
                    } else {
                        globalArray.setPrioritizedResource(ResourceType.ADAMANTIUM);
                    }
                    // set target elixir well
                    if (turnCount > 200 && !setTargetElixirWell) {
                        // setTargetElixirWell();
                    }
                    // save game state
                    rc.writeSharedArray(0, globalArray.getGameStateNumber());
                }
                lastAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
                lastMana = rc.getResourceAmount(ResourceType.MANA);
            } catch (GameActionException e) {
                System.out.println("GameActionException at HeadQuarters");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at HeadQuarters");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    private void setTargetElixirWell() throws Exception {
        try {
            setTargetElixirWell = true;
            MapLocation[] wells = GlobalArray.getKnownWellLocations(rc);
            MapLocation[] headQuarters = GlobalArray.getKnownHeadQuarterLocations(rc);
            int lowestDist = Integer.MAX_VALUE;
            int wellIndex = -1;
            int hqIndex = -1;
            for (int i = 0; i < headQuarters.length; i++) {
                if (headQuarters[i] != null) {
                    for (int j = 0; j < wells.length; j++) {
                        if (wells[j] != null) {
                            int dist = headQuarters[i].distanceSquaredTo(wells[j]);
                            if (dist < lowestDist) {
                                lowestDist = dist;
                                hqIndex = i;
                                wellIndex = j;
                            }
                        }
                    }
                }
            }
            if (wellIndex > -1) {
                System.out.println("SET ELIXIR-HQ TARGET PAIR: " + wells[wellIndex].toString() + " " + headQuarters[hqIndex].toString());
                globalArray.setTargetElixirWellHQPair(wellIndex, hqIndex);
            }
        } catch (GameActionException e) {
            System.out.println("GameActionException setting target elixir well");
            e.printStackTrace();
        }
    }

    private MapLocation optimalSpawnLocation(RobotController rc, MapLocation me, boolean well) throws GameActionException {
        WellInfo[] wellInfo = rc.senseNearbyWells();
        MapLocation[] spawningLocations = rc.getAllLocationsWithinRadiusSquared(me, 9);
        MapLocation optimalSpawningLocation = null;
        possibleSpawningLocations = 0;
        if (wellInfo.length > 0 && well) {
            WellInfo prioritizedWellInfo = wellInfo[0];
            MapLocation prioritizedWellInfoLocation = wellInfo[0].getMapLocation();
            ResourceType prioritizedResourceType = ResourceType.ADAMANTIUM;
            // if (turnCount < 15) {
            //     prioritizedResourceType = ResourceType.ADAMANTIUM;
            // }
            for (WellInfo w : wellInfo) {
                if (prioritizedWellInfo.getResourceType() == prioritizedResourceType) {
                    if (w.getResourceType() == prioritizedResourceType
                            && prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w
                                    .getMapLocation().distanceSquaredTo(me)) {
                        prioritizedWellInfo = w;
                        prioritizedWellInfoLocation = w.getMapLocation();
                    }
                } else {
                    if (prioritizedWellInfo.getMapLocation().distanceSquaredTo(me) > w.getMapLocation()
                            .distanceSquaredTo(me)) {
                        prioritizedWellInfo = w;
                        prioritizedWellInfoLocation = w.getMapLocation();
                    }
                }
            }
            for (MapLocation m : spawningLocations) {
                if (rc.sensePassability(m) == false || rc.isLocationOccupied(m)) {
                    continue;
                }
                possibleSpawningLocations += 1;
                if (optimalSpawningLocation == null) {
                    optimalSpawningLocation = m;
                }
                else if (optimalSpawningLocation.distanceSquaredTo(prioritizedWellInfoLocation) > m.distanceSquaredTo(prioritizedWellInfoLocation)) {
                    optimalSpawningLocation = m;
                }
            }
        }
        else {
            for (MapLocation m : spawningLocations) {
                if (rc.sensePassability(m) == false || rc.isLocationOccupied(m)) {
                    continue;
                }
                possibleSpawningLocations += 1;
                if (optimalSpawningLocation == null) {
                    optimalSpawningLocation = m;
                }
                else if (optimalSpawningLocation.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2)) > m.distanceSquaredTo(new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2))) {
                    optimalSpawningLocation = m;
                }
            }
        }
        return optimalSpawningLocation;
    }
}