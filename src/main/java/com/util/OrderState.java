package com.util;

public enum OrderState {
    WaitToPay,//等待付款
    WaitStoreToAccept,//已付款，等待商家接单
    WaitToSendOut,//商家已接单，备货中，等待送出
    Sending,//配送中
    Over,//订单完成,确认收货
    CancelBeforeAccept,//商家接单前取消
    CancelAfterAccept,//商家接单后取消
    Refuse//商家拒接单
}
