package SPAARK_1_13_2023;

import battlecode.common.*;

public strictfp class Booster {
    private RobotController rc;
    private MapLocation me;

    private static int turnCount = 0;

    public Booster(RobotController rc) {
        try {
            this.rc = rc;
            rc.setIndicatorString("Initializing");
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Booster constructor");
        //     e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Booster constructor");
            e.printStackTrace();
        } finally {
            Clock.yield();
        }
        run();
    }
    
    public void run() {
        while (true) {
            try {
                // code
                turnCount++;
            // } catch (GameActionException e) {
            //     System.out.println("GameActionException at Booster");
            //     e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Booster");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}