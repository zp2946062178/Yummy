package com.util;

public enum UserState{
    //普通消费者的状态
    WaitToActive,//待激活
    CloseAccount,//账号已经注销

    //平台店铺状态
    ToCheck,//待审核状态

    Valid//账号正使用中
}