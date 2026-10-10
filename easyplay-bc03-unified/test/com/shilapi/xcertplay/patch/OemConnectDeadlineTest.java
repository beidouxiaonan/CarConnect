package com.shilapi.xcertplay.patch;
import org.junit.Test;
import static org.junit.Assert.*;
public class OemConnectDeadlineTest {
    @Test public void socketConnectHasFiniteBudget(){OemConnectDeadline d=new OemConnectDeadline(0);assertNull(d.expired(4999999999L));assertTrue(d.expired(5000000000L).contains("连接本地 socket"));}
    @Test public void statusWaitDoesNotConsumeNextStageBudget(){OemConnectDeadline d=new OemConnectDeadline(0);d.enter("等待 SPP",4000000000L,18000000000L);assertNull(d.expired(20000000000L));d.enter("登记手机",21000000000L,5000000000L);assertNull(d.expired(25999999999L));assertTrue(d.expired(26000000000L).contains("登记手机"));}
    @Test public void statusWaitItselfRemainsBounded(){OemConnectDeadline d=new OemConnectDeadline(0);d.enter("等待 SPP",5000000000L,18000000000L);assertTrue(d.expired(23000000000L).contains("等待 SPP"));}
    @Test public void stageChangesCannotExtendTotalDeadline(){OemConnectDeadline d=new OemConnectDeadline(0);d.enter("交接",44000000000L,5000000000L);assertTrue(d.expired(45000000000L).contains("总建立超时"));}
    @Test public void clockOriginDoesNotAffectRelativeBudgets(){OemConnectDeadline d=new OemConnectDeadline(1000000000000L);assertNull(d.expired(1004999999999L));assertNotNull(d.expired(1005000000000L));}
}
