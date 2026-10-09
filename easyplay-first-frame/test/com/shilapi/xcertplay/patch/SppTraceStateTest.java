package com.shilapi.xcertplay.patch;
import org.junit.Test;
import static org.junit.Assert.*;
public class SppTraceStateTest {
    @Test public void noEventDoesNotPretendReleaseSucceeded() {
        SppTraceState s=new SppTraceState();s.cache(true,0);s.request(100);
        String t=s.summary(12100);assertTrue(t.contains("清理请求后广播=0"));assertTrue(t.contains("无法区分真实占用与缓存残留"));
    }
    @Test public void staleDisconnectedEventBeforeRequestCannotOverrideCache() {
        SppTraceState s=new SppTraceState();s.event(0,0);s.cache(true,100);s.request(1000);
        assertFalse(s.summary(2000).contains("缓存不一致"));
    }
    @Test public void newDisconnectWithTrueCacheReportsContradiction() {
        SppTraceState s=new SppTraceState();s.cache(true,0);s.request(100);s.event(0,400);
        assertTrue(s.summary(500).contains("断开广播与缓存不一致"));assertTrue(s.summary(500).contains("仍停止接入"));
    }
    @Test public void reconnectAfterDisconnectReportsLatestConnectedEvent() {
        SppTraceState s=new SppTraceState();s.request(0);s.event(0,100);s.event(1,200);s.cache(true,300);
        assertTrue(s.summary(400).contains("清理请求后广播=2"));assertTrue(s.summary(400).contains("最近广播=连接"));
        assertFalse(s.summary(400).contains("缓存不一致"));
    }
    @Test public void invalidBroadcastCannotCountAsReply() {
        SppTraceState s=new SppTraceState();s.event(-1,0);s.event(2,1);assertTrue(s.summary(10).contains("广播次数=0"));
    }
    @Test public void changesAreObservedWithoutRetainingAddresses() {
        SppTraceState s=new SppTraceState();s.cache(true,0);s.cache(true,1);s.cache(false,2);
        assertTrue(s.summary(3).contains("状态变化=1"));assertTrue(s.summary(3).contains("Binder缓存=未连接"));
        assertTrue(s.summary(3).endsWith("广播只作诊断，不替代清理成功校验。"));
    }
}
