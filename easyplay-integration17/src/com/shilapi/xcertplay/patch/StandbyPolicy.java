package com.shilapi.xcertplay.patch;
/** No clock or platform side effects. A new off event invalidates all older wake work. */
final class StandbyPolicy {
    private long cycle;
    private boolean held,screenOn=true;
    synchronized long off() {
        if(held&&!screenOn)return 0;
        screenOn=false;held=true;return ++cycle;
    }
    synchronized long wake() {
        if(!held||screenOn)return 0;
        screenOn=true;return cycle;
    }
    synchronized boolean offValid(long token){return held&&!screenOn&&cycle==token;}
    synchronized boolean wakeValid(long token){return held&&screenOn&&cycle==token;}
    synchronized boolean blocked(){return held;}
    synchronized boolean complete(long token){if(!wakeValid(token))return false;held=false;return true;}
    synchronized void cancel(){held=false;screenOn=true;cycle++;}
}
