package SPAARK_TEST_2;

import battlecode.common.*;

public strictfp class Booster {
    protected RobotController rc;
    protected MapLocation me;
    private GlobalArray globalArray = new GlobalArray();
    private int round = 0;

    protected StringBuilder indicatorString = new StringBuilder();

    public Booster(RobotController rc) {
        try {
            this.rc = rc;
        // } catch (GameActionException e) {
        //     System.out.println("GameActionException at Booster constructor");
        //     e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Exception at Booster constructor");
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
                // code
            // } catch (GameActionException e) {
            //     System.out.println("GameActionException at Booster");
            //     e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception at Booster");
                e.printStackTrace();
            } finally {
                rc.setIndicatorString(indicatorString.toString());
                Clock.yield();
            }
        }
    }
}